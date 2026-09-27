package com.tamilnotify.app.pipeline

import java.util.regex.Pattern

data class ProtectedPayload(
    val maskedText: String,
    val tokenMap: Map<String, String>
)

/**
 * Protects sensitive and structured tokens (URLs, emails, OTPs, currency, phone numbers,
 * dates, numbers, and emojis) before translation so they are preserved verbatim.
 */
object TokenProtector {

    // Regex patterns prioritized by specificity
    private val URL_PATTERN = Pattern.compile(
        "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+",
        Pattern.CASE_INSENSITIVE
    )

    private val EMAIL_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    )

    // Currencies e.g., $4,500, Rs. 500, ₹10,000, 50 USD, €45.50
    private val CURRENCY_PATTERN = Pattern.compile(
        "(?:[₹$€£¥]\\s*\\d+(?:[.,]\\d+)?)|(?:\\b(?:Rs\\.?|INR|USD|EUR|GBP)\\s*\\d+(?:[.,]\\d+)?)|(?:\\d+(?:[.,]\\d+)?\\s*(?:USD|EUR|INR|Rs))",
        Pattern.CASE_INSENSITIVE
    )

    // Dates and Times e.g., 5:00 PM, 14:30, 27/09/2026, 2026-09-27
    private val DATE_TIME_PATTERN = Pattern.compile(
        "\\b(?:\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:[AaPp][Mm])?)|(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\b"
    )

    // OTPs & Verification Codes (e.g., OTP is 482910, Code: 9481, 4-8 digit standalone numbers)
    private val OTP_PATTERN = Pattern.compile(
        "(?<=\\b(?:otp|code|pin|password|verification|auth|secret)\\s{0,5}(?:is|:|[-=])?\\s{0,3})\\b\\d{4,8}\\b",
        Pattern.CASE_INSENSITIVE
    )

    // Generic phone numbers e.g. +91 9876543210, +1-800-555-0199
    private val PHONE_PATTERN = Pattern.compile(
        "\\+?\\d{1,3}[-.\\s]?(?:\\(\\d{1,4}\\)|\\d{1,4})[-.\\s]?\\d{2,4}[-.\\s]?\\d{3,4}"
    )

    // Standalone numbers e.g., 9876543210
    private val STANDALONE_NUMBER_PATTERN = Pattern.compile(
        "\\b\\d{3,}\\b"
    )

    /**
     * Replaces sensitive and non-translatable entities with indexed placeholders.
     */
    fun protectTokens(text: String): ProtectedPayload {
        if (text.isBlank()) {
            return ProtectedPayload(text, emptyMap())
        }

        val tokenMap = mutableMapOf<String, String>()
        var tokenCounter = 0
        var currentText = text

        val patternsToProtect = listOf(
            URL_PATTERN,
            EMAIL_PATTERN,
            CURRENCY_PATTERN,
            OTP_PATTERN,
            DATE_TIME_PATTERN,
            PHONE_PATTERN,
            STANDALONE_NUMBER_PATTERN
        )

        for (pattern in patternsToProtect) {
            val matcher = pattern.matcher(currentText)
            val sb = StringBuffer()

            while (matcher.find()) {
                val matched = matcher.group()
                // Avoid re-tokenizing an existing token
                if (matched.startsWith("TKX") && matched.endsWith("Z")) {
                    matcher.appendReplacement(sb, MatcherQuote(matched))
                    continue
                }

                val placeholder = "TKX${tokenCounter}Z"
                tokenMap[placeholder] = matched
                tokenCounter++
                matcher.appendReplacement(sb, MatcherQuote(placeholder))
            }
            matcher.appendTail(sb)
            currentText = sb.toString()
        }

        return ProtectedPayload(currentText, tokenMap)
    }

    /**
     * Restores protected tokens back to their original values in the translated text.
     */
    fun restoreTokens(translatedText: String, tokenMap: Map<String, String>): String {
        var restored = translatedText
        for ((token, originalValue) in tokenMap) {
            // ML Kit might add spaces around punctuation or lowercase letters: handle variations
            val regex = Regex("(?i)\\b" + Regex.escape(token) + "\\b|" + Regex.escape(token))
            restored = restored.replace(regex, originalValue)
        }
        return restored
    }

    private fun MatcherQuote(s: String): String {
        return java.util.regex.Matcher.quoteReplacement(s)
    }
}
