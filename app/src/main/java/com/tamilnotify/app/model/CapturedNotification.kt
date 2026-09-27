package com.tamilnotify.app.model

enum class TranslationStatus {
    PENDING,
    TRANSLATING,
    TRANSLATED,
    SKIPPED,
    FAILED
}

data class CapturedNotification(
    val key: String,
    val notificationId: Int,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val bigText: String?,
    val postTime: Long,
    val isOngoing: Boolean,
    val isGroupSummary: Boolean,
    val capturedAt: Long = System.currentTimeMillis(),
    val isEligibleForTranslation: Boolean = true,
    val translatedText: String? = null,
    val translationStatus: TranslationStatus = TranslationStatus.PENDING,
    val reasonSkipped: String? = null
)
