package com.tamilnotify.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tamilnotify.app.R
import com.tamilnotify.app.data.DisplayMode

/**
 * Handles the creation, attribution, and reposting of translated notifications.
 *
 * PLATFORM RESTRICTION NOTE:
 * On Android 7.0+ (API 24+), Android requires the Notification Small Icon to be a valid
 * drawable resource belonging to the posting application's own package. Passing an icon
 * resource ID from a third-party app (e.g. WhatsApp) will throw a SecurityException or
 * render as an empty placeholder.
 *
 * To respect this platform restriction while maintaining source identity:
 * 1. Small Icon: Always uses Tamil Notify's monochrome vector icon (R.drawable.ic_notification).
 * 2. Large Icon: Safely resolves the source app's colored launcher icon via PackageManager
 *    and renders it as a Bitmap into setLargeIcon().
 */
object NotificationReposter {

    private const val TAG = "NotificationReposter"
    const val CHANNEL_ID = "tamil_translations_channel"
    const val EXTRA_IS_REPOSTED = "com.tamilnotify.app.IS_REPOSTED"
    const val NOTIFICATION_TAG = "tamil_translated"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Tamil Translated Notifications"
            val descriptionText = "Displays real-time Tamil translations of captured notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Reposts a translated notification with source app attribution.
     */
    fun repost(
        context: Context,
        sourcePackage: String,
        sourceAppName: String,
        senderOrTitle: String,
        originalContent: String,
        translatedContent: String,
        displayMode: DisplayMode,
        originalNotificationId: Int,
        originalContentIntent: PendingIntent? = null
    ) {
        createNotificationChannel(context)

        // Resolve source app icon bitmap for the Large Icon
        val sourceAppIconBitmap = getAppIconBitmap(context, sourcePackage)

        // Build notification display title
        val titleText = if (senderOrTitle.isNotBlank()) {
            "$senderOrTitle • $sourceAppName"
        } else {
            sourceAppName
        }

        // Format body text according to display mode
        val bodyText = when (displayMode) {
            DisplayMode.TAMIL_ONLY -> translatedContent
            DisplayMode.BOTH -> "$translatedContent\n\nOriginal: $originalContent"
        }

        // Distinct ID derived from source package and ID to avoid collision across different apps
        val repostId = (sourcePackage.hashCode() xor originalNotificationId) and 0x7FFFFFFF

        val extras = Bundle().apply {
            putBoolean(EXTRA_IS_REPOSTED, true)
            putString("com.tamilnotify.app.ORIGINAL_PACKAGE", sourcePackage)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titleText)
            .setContentText(translatedContent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
                    .setBigContentTitle(titleText)
                    .setSummaryText("தமிழ் மொழிபெயர்ப்பு")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addExtras(extras)

        // Set source app icon as LargeIcon if available
        if (sourceAppIconBitmap != null) {
            builder.setLargeIcon(sourceAppIconBitmap)
        }

        // Pass through original content click intent if technically available
        if (originalContentIntent != null) {
            builder.setContentIntent(originalContentIntent)
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_TAG, repostId, builder.build())
            Log.i(TAG, "✓ Successfully reposted translated notification for $sourceAppName (ID: $repostId)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing POST_NOTIFICATIONS permission on Android 13+: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to repost notification: ${e.message}", e)
        }
    }

    /**
     * Safely converts application icon Drawable into Bitmap for setLargeIcon.
     */
    private fun getAppIconBitmap(context: Context, packageName: String): Bitmap? {
        return try {
            val pm = context.packageManager
            val drawable: Drawable = pm.getApplicationIcon(packageName)
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }
}
