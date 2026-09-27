package com.tamilnotify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tamilnotify.app.data.DisplayMode
import com.tamilnotify.app.data.UserSettings
import com.tamilnotify.app.repository.NotificationStateHolder
import com.tamilnotify.app.ui.theme.Amber500
import com.tamilnotify.app.ui.theme.Emerald400
import com.tamilnotify.app.ui.theme.Emerald500
import com.tamilnotify.app.ui.theme.Orange500
import com.tamilnotify.app.ui.theme.Slate400
import com.tamilnotify.app.ui.theme.Slate700
import com.tamilnotify.app.ui.theme.Slate800
import com.tamilnotify.app.ui.theme.Slate900

enum class NavigationTab(val title: String) {
    FEED("Live Feed"),
    APPS("Apps"),
    SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userSettings: UserSettings,
    isListenerGranted: Boolean,
    isPostGranted: Boolean,
    onOpenListenerSettings: () -> Unit,
    onRequestPostPermission: () -> Unit,
    onSendSampleNotification: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onChangeTargetLanguage: (String) -> Unit,
    onChangeDisplayMode: (DisplayMode) -> Unit,
    onToggleRequireWifi: (Boolean) -> Unit,
    onTogglePackage: (String, Boolean) -> Unit,
    onSetAllPackages: (Set<String>) -> Unit,
    onDownloadModel: () -> Unit,
    onDeleteModel: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.FEED) }
    val isConnected by NotificationStateHolder.isServiceConnected.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Orange500),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "App Logo",
                                tint = Slate900,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tamil Notify",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "அறிவிப்பு மொழிபெயர்ப்பாளர்",
                                style = MaterialTheme.typography.labelSmall,
                                color = Orange500
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate900
                ),
                actions = {
                    // System service live status indicator badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isListenerGranted && isConnected) Emerald500.copy(alpha = 0.15f) else Amber500.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isListenerGranted && isConnected) Emerald500 else Amber500
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isListenerGranted && isConnected) Emerald400 else Amber500)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isListenerGranted && isConnected) "Listening" else "Action Needed",
                                color = if (isListenerGranted && isConnected) Emerald400 else Amber500,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Slate800,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.FEED,
                    onClick = { selectedTab = NavigationTab.FEED },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Feed") },
                    label = { Text("Feed") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Slate900,
                        selectedTextColor = Orange500,
                        indicatorColor = Orange500,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == NavigationTab.APPS,
                    onClick = { selectedTab = NavigationTab.APPS },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") },
                    label = { Text("Apps") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Slate900,
                        selectedTextColor = Orange500,
                        indicatorColor = Orange500,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == NavigationTab.SETTINGS,
                    onClick = { selectedTab = NavigationTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Slate900,
                        selectedTextColor = Orange500,
                        indicatorColor = Orange500,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400
                    )
                )
            }
        },
        containerColor = Slate900
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavigationTab.FEED -> {
                    LiveFeedScreen(
                        isListenerGranted = isListenerGranted,
                        isPostGranted = isPostGranted,
                        onOpenListenerSettings = onOpenListenerSettings,
                        onRequestPostPermission = onRequestPostPermission,
                        onSendSampleNotification = onSendSampleNotification,
                        onDownloadModel = onDownloadModel
                    )
                }
                NavigationTab.APPS -> {
                    AppSelectionScreen(
                        enabledPackages = userSettings.enabledPackages,
                        onTogglePackage = onTogglePackage,
                        onSetAllPackages = onSetAllPackages
                    )
                }
                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        userSettings = userSettings,
                        onToggleMaster = onToggleMaster,
                        onChangeTargetLanguage = onChangeTargetLanguage,
                        onChangeDisplayMode = onChangeDisplayMode,
                        onToggleRequireWifi = onToggleRequireWifi,
                        onDownloadModel = onDownloadModel,
                        onDeleteModel = onDeleteModel
                    )
                }
            }
        }
    }
}
