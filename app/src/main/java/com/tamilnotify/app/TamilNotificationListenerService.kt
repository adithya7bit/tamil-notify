package com.tamilnotify.app

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.tamilnotify.app.model.CapturedNotification
import com.tamilnotify.app.pipeline.NotificationTranslationPipeline
import com.tamilnotify.app.repository.NotificationStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android NotificationListenerService responsible for intercepting system notifications.
 * Automatically processes English notifications, translates them to Tamil on-device,
 * and reposts them in the background without needing the UI to be open.
 */
class TamilNotificationListenerService : NotificationListenerService() {

    companion object {
        const val TAG = "TamilNotifyListener"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var pipeline: NotificationTranslationPipeline

    override fun onCreate() {
        super.onCreate()
        pipeline = NotificationTranslationPipeline(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "✓ NotificationListenerService CONNECTED to Android System")
        NotificationStateHolder.setServiceConnected(true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "✗ NotificationListenerService DISCONNECTED from Android System")
        NotificationStateHolder.setServiceConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val sourcePackage = sbn.packageName ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // 1. RECURSION PREVENTION:
        // Strictly ignore reposted notifications from Tamil Notify to prevent infinite loops
        val isRepostedBySelf = extras.getBoolean("com.tamilnotify.app.IS_REPOSTED", false) ||
                sbn.tag == "tamil_translated"
        if (isRepostedBySelf) {
            Log.d(TAG, "Ignored reposted notification ($sourcePackage) to prevent loop.")
            return
        }

        // If from self and not our explicit test notification, ignore
        val isExplicitTest = (sourcePackage == packageName && sbn.id == 9001)
        if (sourcePackage == packageName && !isExplicitTest) {
            Log.d(TAG, "Ignored internal notification from $sourcePackage")
            return
        }

        // 2. Extract notification details
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()

        val isOngoing = sbn.isOngoing
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

        // Resolve source app display name
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(
                sourcePackage,
                PackageManager.MATCH_UNINSTALLED_PACKAGES
            )
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            sourcePackage
        }

        val effectiveContent = when {
            !bigText.isNullOrBlank() -> bigText
            text.isNotBlank() -> text
            else -> ""
        }

        val hasContent = title.isNotBlank() || effectiveContent.isNotBlank()

        Log.i(TAG, """
            |┌───────────────────────────────────────────────────────────────┐
            |│ [NOTIFICATION CAPTURED]                                       │
            |├───────────────────────────────────────────────────────────────┤
            |│ App:          $appName ($sourcePackage)
            |│ ID:           ${sbn.id} | Tag: ${sbn.tag ?: "null"}
            |│ Ongoing:      $isOngoing | GroupSummary: $isGroupSummary
            |│ Title:        $title
            |│ Text:         $text
            |│ BigText:      ${bigText ?: "(none)"}
            |│ SubText:      ${subText ?: "(none)"}
            |│ HasContent:   $hasContent
            |└───────────────────────────────────────────────────────────────┘
        """.trimMargin())

        val captured = CapturedNotification(
            key = sbn.key ?: "${sourcePackage}_${sbn.id}_${sbn.postTime}",
            notificationId = sbn.id,
            packageName = sourcePackage,
            appName = appName,
            title = title,
            text = text,
            bigText = bigText,
            postTime = sbn.postTime,
            isOngoing = isOngoing,
            isGroupSummary = isGroupSummary,
            isEligibleForTranslation = hasContent && !isOngoing && !isGroupSummary
        )

        // Record in observable repository
        NotificationStateHolder.addNotification(captured)

        // Asynchronously process translation pipeline in the background
        serviceScope.launch {
            try {
                pipeline.processNotification(sbn, captured)
            } catch (e: Exception) {
                Log.e(TAG, "Error in translation pipeline execution: ${e.message}", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        if (sbn.packageName == packageName) return

        Log.d(TAG, "Notification removed: package=${sbn.packageName}, id=${sbn.id}")
    }
}
