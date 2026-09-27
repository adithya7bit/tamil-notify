package com.tamilnotify.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.tamilnotify.app.data.AppSettingsRepository
import com.tamilnotify.app.data.UserSettings
import com.tamilnotify.app.ml.TranslationEngine
import com.tamilnotify.app.notification.NotificationReposter
import com.tamilnotify.app.ui.screens.MainScreen
import com.tamilnotify.app.ui.theme.TamilNotifyTheme
import com.tamilnotify.app.util.NotificationPermissionHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val TEST_CHANNEL_ID = "tamil_notify_test_channel"
        const val TEST_NOTIFICATION_ID = 9001
    }

    private lateinit var settingsRepository: AppSettingsRepository
    private val translationEngine = TranslationEngine.getInstance()

    private var isListenerGranted by mutableStateOf(false)
    private var isPostGranted by mutableStateOf(false)

    private val postNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPostGranted = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = AppSettingsRepository(applicationContext)

        createNotificationChannels()

        setContent {
            val userSettings by settingsRepository.settingsFlow.collectAsState(initial = UserSettings())

            TamilNotifyTheme {
                MainScreen(
                    userSettings = userSettings,
                    isListenerGranted = isListenerGranted,
                    isPostGranted = isPostGranted,
                    onOpenListenerSettings = {
                        NotificationPermissionHelper.openNotificationListenerSettings(this)
                    },
                    onRequestPostPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onSendSampleNotification = {
                        postSampleNotification()
                    },
                    onToggleMaster = { enabled ->
                        lifecycleScope.launch {
                            settingsRepository.setTranslationEnabled(enabled)
                        }
                    },
                    onChangeTargetLanguage = { languageCode ->
                        lifecycleScope.launch {
                            settingsRepository.setTargetLanguage(languageCode)
                            translationEngine.checkModelStatus(languageCode)
                        }
                    },
                    onChangeDisplayMode = { mode ->
                        lifecycleScope.launch {
                            settingsRepository.setDisplayMode(mode)
                        }
                    },
                    onToggleRequireWifi = { requireWifi ->
                        lifecycleScope.launch {
                            settingsRepository.setRequireWifiForDownload(requireWifi)
                        }
                    },
                    onTogglePackage = { packageName, enabled ->
                        lifecycleScope.launch {
                            settingsRepository.togglePackageEnabled(packageName, enabled)
                        }
                    },
                    onSetAllPackages = { packages ->
                        lifecycleScope.launch {
                            settingsRepository.setEnabledPackages(packages)
                        }
                    },
                    onDownloadModel = {
                        lifecycleScope.launch {
                            translationEngine.downloadModel(
                                targetLanguage = userSettings.targetLanguage,
                                requireWifi = userSettings.requireWifiForDownload
                            )
                        }
                    },
                    onDeleteModel = {
                        lifecycleScope.launch {
                            translationEngine.deleteModel(userSettings.targetLanguage)
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-evaluate permissions when user returns to the app
        isListenerGranted = NotificationPermissionHelper.isNotificationListenerGranted(this)
        isPostGranted = NotificationPermissionHelper.isPostNotificationsGranted(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel 1: Incoming verification test channel
            val testChannel = NotificationChannel(
                TEST_CHANNEL_ID,
                "Tamil Notify Test Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for test notifications"
            }
            manager.createNotificationChannel(testChannel)

            // Channel 2: Reposted translations channel
            NotificationReposter.createNotificationChannel(this)
        }
    }

    private fun postSampleNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sampleNotification = NotificationCompat.Builder(this, TEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Priya (Accounts)")
            .setContentText("Please send the invoice for $4,500 by 5:00 PM today.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Please send the invoice for $4,500 by 5:00 PM today. OTP verification code is 849201. Review details at https://pay.example.com/inv/12345"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(TEST_NOTIFICATION_ID, sampleNotification)
    }
}
