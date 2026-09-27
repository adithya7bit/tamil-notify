package com.tamilnotify.app.pipeline

/**
 * Utility to identify whether text is already predominantly Tamil,
 * preventing redundant translations.
 */
object LanguageDetectorUtil {

    private const val TAMIL_UNICODE_START = 0x0B80
    private const val TAMIL_UNICODE_END = 0x0BFF

    /**
     * Determines whether the given text already contains significant Tamil text (>35% of alphabetic characters).
     */
    fun isPredominantlyTamil(text: String): Boolean {
        if (text.isBlank()) return false

        var tamilCharCount = 0
        var totalAlphaCount = 0

        for (char in text) {
            val codePoint = char.code
            if (codePoint in TAMIL_UNICODE_START..TAMIL_UNICODE_END) {
                tamilCharCount++
                totalAlphaCount++
            } else if (char.isLetter()) {
                totalAlphaCount++
            }
        }

        if (totalAlphaCount == 0) return false
        val ratio = tamilCharCount.toFloat() / totalAlphaCount.toFloat()
        return ratio >= 0.35f
    }
}
