package com.tamilnotify.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tamil_notify_settings")

enum class DisplayMode {
    BOTH,       // Show original English and Tamil translation
    TAMIL_ONLY  // Show only Tamil translation
}

data class UserSettings(
    val isTranslationEnabled: Boolean = true,
    val targetLanguage: String = TranslateLanguage.TAMIL,
    val enabledPackages: Set<String> = emptySet(),
    val displayMode: DisplayMode = DisplayMode.BOTH,
    val requireWifiForDownload: Boolean = false
)

class AppSettingsRepository(private val context: Context) {

    companion object {
        private val KEY_TRANSLATION_ENABLED = booleanPreferencesKey("is_translation_enabled")
        private val KEY_TARGET_LANGUAGE = stringPreferencesKey("target_language")
        private val KEY_ENABLED_PACKAGES = stringSetPreferencesKey("enabled_packages")
        private val KEY_DISPLAY_MODE = stringPreferencesKey("display_mode")
        private val KEY_REQUIRE_WIFI = booleanPreferencesKey("require_wifi")

        // Default popular communication apps enabled initially for instant utility
        val DEFAULT_POPULAR_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.facebook.orca",
            "com.instagram.android",
            "com.google.android.gm"
        )
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val isEnabled = preferences[KEY_TRANSLATION_ENABLED] ?: true
        val targetLang = preferences[KEY_TARGET_LANGUAGE] ?: TranslateLanguage.TAMIL
        val enabledPkgs = preferences[KEY_ENABLED_PACKAGES] ?: DEFAULT_POPULAR_PACKAGES
        val displayModeStr = preferences[KEY_DISPLAY_MODE] ?: DisplayMode.BOTH.name
        val displayMode = try {
            DisplayMode.valueOf(displayModeStr)
        } catch (_: Exception) {
            DisplayMode.BOTH
        }
        val requireWifi = preferences[KEY_REQUIRE_WIFI] ?: false

        UserSettings(
            isTranslationEnabled = isEnabled,
            targetLanguage = targetLang,
            enabledPackages = enabledPkgs,
            displayMode = displayMode,
            requireWifiForDownload = requireWifi
        )
    }

    suspend fun setTranslationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TRANSLATION_ENABLED] = enabled
        }
    }

    suspend fun setTargetLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TARGET_LANGUAGE] = languageCode
        }
    }

    suspend fun togglePackageEnabled(packageName: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_ENABLED_PACKAGES]?.toMutableSet() ?: DEFAULT_POPULAR_PACKAGES.toMutableSet()
            if (enabled) {
                current.add(packageName)
            } else {
                current.remove(packageName)
            }
            preferences[KEY_ENABLED_PACKAGES] = current
        }
    }

    suspend fun setEnabledPackages(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ENABLED_PACKAGES] = packages
        }
    }

    suspend fun setDisplayMode(mode: DisplayMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DISPLAY_MODE] = mode.name
        }
    }

    suspend fun setRequireWifiForDownload(requireWifi: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REQUIRE_WIFI] = requireWifi
        }
    }
}
