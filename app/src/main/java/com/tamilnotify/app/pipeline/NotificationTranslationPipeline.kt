package com.tamilnotify.app.pipeline

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import android.util.LruCache
import com.tamilnotify.app.data.AppSettingsRepository
import com.tamilnotify.app.data.DisplayMode
import com.tamilnotify.app.data.UserSettings
import com.tamilnotify.app.ml.TranslationEngine
import com.tamilnotify.app.model.CapturedNotification
import com.tamilnotify.app.model.TranslationStatus
import com.tamilnotify.app.notification.NotificationReposter
import com.tamilnotify.app.repository.NotificationStateHolder
import kotlinx.coroutines.flow.first

/**
 * End-to-end processing pipeline for captured notifications:
 * Capture -> Validate -> Filter -> Protect Tokens -> On-Device Translate -> Restore Tokens -> Repost.
 */
class NotificationTranslationPipeline(private val context: Context) {

    companion object {
        private const val TAG = "TranslationPipeline"

        // Cache recent processed notification hashes to deduplicate rapid updates
        private val recentTextHashes = LruCache<String, Long>(100)
    }

    private val settingsRepository = AppSettingsRepository(context)
    private val translationEngine = TranslationEngine.getInstance()

    suspend fun processNotification(
        sbn: StatusBarNotification,
        captured: CapturedNotification
    ) {
        val settings = settingsRepository.settingsFlow.first()

        // 1. Check master toggle
        if (!settings.isTranslationEnabled) {
            markSkipped(captured.key, "Translation globally disabled")
            return
        }

        // 2. Per-app filter check
        val isExplicitTest = (sbn.packageName == context.packageName && sbn.id == 9001)
        val isAppEnabled = isExplicitTest || settings.enabledPackages.contains(sbn.packageName)
        if (!isAppEnabled) {
            markSkipped(captured.key, "App not enabled in settings")
            return
        }

        // 3. Ongoing notifications filter
        if (sbn.isOngoing) {
            markSkipped(captured.key, "Ongoing notification (e.g. media/call)")
            return
        }

        // 4. Group summary header filter
        val notification = sbn.notification ?: return
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary) {
            markSkipped(captured.key, "Group summary header")
            return
        }

        // 5. Extract effective text body
        val textToTranslate = when {
            !captured.bigText.isNullOrBlank() -> captured.bigText
            captured.text.isNotBlank() -> captured.text
            else -> ""
        }

        if (textToTranslate.isBlank()) {
            markSkipped(captured.key, "Empty message body")
            return
        }

        // 6. Duplicate update prevention
        val textHashKey = "${sbn.packageName}_${textToTranslate.hashCode()}"
        val lastSeenTime = recentTextHashes.get(textHashKey)
        val now = System.currentTimeMillis()
        if (lastSeenTime != null && (now - lastSeenTime) < 5000) {
            Log.d(TAG, "Skipping duplicate update for $textHashKey within 5s")
            return
        }
        recentTextHashes.put(textHashKey, now)

        // 7. Check if already predominantly Tamil
        if (LanguageDetectorUtil.isPredominantlyTamil(textToTranslate)) {
            markSkipped(captured.key, "Already predominantly Tamil")
            return
        }

        // 8. Protect sensitive tokens (URLs, OTPs, currency, dates, numbers)
        NotificationStateHolder.updateNotification(captured.key) {
            it.copy(translationStatus = TranslationStatus.TRANSLATING)
        }

        val protectedPayload = TokenProtector.protectTokens(textToTranslate)
        Log.i(TAG, "Protected ${protectedPayload.tokenMap.size} tokens in: [${protectedPayload.maskedText}]")

        // 9. On-device Translation via ML Kit
        val translationResult = translationEngine.translate(
            text = protectedPayload.maskedText,
            targetLanguage = settings.targetLanguage
        )

        translationResult.fold(
            onSuccess = { rawTranslatedText ->
                // 10. Restore protected tokens verbatim
                val finalTamilText = TokenProtector.restoreTokens(
                    translatedText = rawTranslatedText,
                    tokenMap = protectedPayload.tokenMap
                )

                Log.i(TAG, """
                    |✓ Translation successful!
                    |Original: $textToTranslate
                    |Tamil:    $finalTamilText
                """.trimMargin())

                // Update state holder for UI
                NotificationStateHolder.updateNotification(captured.key) {
                    it.copy(
                        translationStatus = TranslationStatus.TRANSLATED,
                        translatedText = finalTamilText
                    )
                }

                // 11. Repost notification with source app attribution and loop-prevention flags
                NotificationReposter.repost(
                    context = context,
                    sourcePackage = sbn.packageName,
                    sourceAppName = captured.appName,
                    senderOrTitle = captured.title,
                    originalContent = textToTranslate,
                    translatedContent = finalTamilText,
                    displayMode = settings.displayMode,
                    originalNotificationId = sbn.id,
                    originalContentIntent = notification.contentIntent
                )
            },
            onFailure = { error ->
                Log.e(TAG, "Translation failed: ${error.message}", error)
                NotificationStateHolder.updateNotification(captured.key) {
                    it.copy(
                        translationStatus = TranslationStatus.FAILED,
                        reasonSkipped = error.localizedMessage ?: "Translation engine error"
                    )
                }
            }
        )
    }

    private fun markSkipped(key: String, reason: String) {
        Log.d(TAG, "Notification $key skipped: $reason")
        NotificationStateHolder.updateNotification(key) {
            it.copy(
                translationStatus = TranslationStatus.SKIPPED,
                reasonSkipped = reason
            )
        }
    }
}
