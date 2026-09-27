package com.tamilnotify.app.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamilnotify.app.ml.ModelStatus
import com.tamilnotify.app.ml.TranslationEngine
import com.tamilnotify.app.model.CapturedNotification
import com.tamilnotify.app.model.TranslationStatus
import com.tamilnotify.app.repository.NotificationStateHolder
import com.tamilnotify.app.ui.theme.Amber500
import com.tamilnotify.app.ui.theme.Emerald400
import com.tamilnotify.app.ui.theme.Emerald500
import com.tamilnotify.app.ui.theme.Orange400
import com.tamilnotify.app.ui.theme.Orange500
import com.tamilnotify.app.ui.theme.Rose500
import com.tamilnotify.app.ui.theme.Slate100
import com.tamilnotify.app.ui.theme.Slate200
import com.tamilnotify.app.ui.theme.Slate400
import com.tamilnotify.app.ui.theme.Slate600
import com.tamilnotify.app.ui.theme.Slate700
import com.tamilnotify.app.ui.theme.Slate800
import com.tamilnotify.app.ui.theme.Slate900
import java.util.Date

@Composable
fun LiveFeedScreen(
    isListenerGranted: Boolean,
    isPostGranted: Boolean,
    onOpenListenerSettings: () -> Unit,
    onRequestPostPermission: () -> Unit,
    onSendSampleNotification: () -> Unit,
    onDownloadModel: () -> Unit
) {
    val capturedList by NotificationStateHolder.notifications.collectAsState()
    val modelStatus by TranslationEngine.getInstance().modelStatus.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Permission Status Card
        item {
            PermissionStatusCard(
                isListenerGranted = isListenerGranted,
                isPostGranted = isPostGranted,
                onOpenListenerSettings = onOpenListenerSettings,
                onRequestPostPermission = onRequestPostPermission
            )
        }

        // ML Kit Model Status Banner
        item {
            ModelStatusCard(
                modelStatus = modelStatus,
                onDownloadModel = onDownloadModel
            )
        }

        // Quick Test Card
        item {
            QuickTestCard(onSendSampleNotification = onSendSampleNotification)
        }

        // Live Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Real-Time Interceptor Feed",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate100,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Slate700
                    ) {
                        Text(
                            text = "${capturedList.size}",
                            color = Orange500,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (capturedList.isNotEmpty()) {
                    IconButton(
                        onClick = { NotificationStateHolder.clear() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Feed Items or Empty State
        if (capturedList.isEmpty()) {
            item {
                EmptyCaptureCard(isListenerGranted = isListenerGranted)
            }
        } else {
            items(capturedList, key = { it.key }) { item ->
                NotificationFeedCard(item = item)
            }
        }
    }
}

@Composable
private fun ModelStatusCard(
    modelStatus: ModelStatus,
    onDownloadModel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (modelStatus is ModelStatus.Ready) Emerald500.copy(alpha = 0.5f) else Orange500.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when (modelStatus) {
                            is ModelStatus.Ready -> Icons.Default.CheckCircle
                            is ModelStatus.Downloading -> Icons.Default.CloudDownload
                            else -> Icons.Default.Translate
                        },
                        contentDescription = null,
                        tint = when (modelStatus) {
                            is ModelStatus.Ready -> Emerald400
                            is ModelStatus.Downloading -> Orange400
                            else -> Amber500
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "On-Device Tamil Model",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            text = when (modelStatus) {
                                is ModelStatus.Ready -> "Model active • Works 100% offline"
                                is ModelStatus.Downloading -> "Downloading model (~30MB)..."
                                is ModelStatus.Checking -> "Checking model status..."
                                is ModelStatus.Error -> "Download error: ${modelStatus.message}"
                                is ModelStatus.NotDownloaded -> "Download required for offline translation"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (modelStatus) {
                                is ModelStatus.Ready -> Emerald400
                                is ModelStatus.Downloading -> Orange400
                                else -> Slate400
                            }
                        )
                    }
                }

                if (modelStatus is ModelStatus.NotDownloaded || modelStatus is ModelStatus.Error) {
                    Button(
                        onClick = onDownloadModel,
                        colors = ButtonDefaults.buttonColors(containerColor = Orange500),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Slate900,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Download",
                            color = Slate900,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (modelStatus is ModelStatus.Downloading) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Orange500,
                    trackColor = Slate700
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    isListenerGranted: Boolean,
    isPostGranted: Boolean,
    onOpenListenerSettings: () -> Unit,
    onRequestPostPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System Access Status",
                style = MaterialTheme.typography.titleMedium,
                color = Slate100,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Notification Listener Access Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isListenerGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isListenerGranted) Emerald400 else Amber500,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Notification Listener Service",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate100
                        )
                        Text(
                            text = if (isListenerGranted) "Granted & active" else "Action required in Settings",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isListenerGranted) Emerald400 else Slate400
                        )
                    }
                }

                if (!isListenerGranted) {
                    Button(
                        onClick = onOpenListenerSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = Orange500),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Slate900,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Grant Access",
                            color = Slate900,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Post Notifications (Android 13+)
            if (!isPostGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Amber500,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Post Notifications (Android 13+)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate100
                            )
                            Text(
                                text = "Needed to repost Tamil translations",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onRequestPostPermission,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Allow",
                            color = Orange500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTestCard(onSendSampleNotification: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Verify Translation Pipeline",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Text(
                    text = "Sends a sample message with OTP, USD, & URL to verify token protection & translation",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onSendSampleNotification,
                colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Orange500,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Test",
                    color = Slate100,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmptyCaptureCard(isListenerGranted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isListenerGranted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = Slate600,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isListenerGranted) "Waiting for Notifications..." else "Notification Access Required",
                style = MaterialTheme.typography.titleMedium,
                color = Slate200,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isListenerGranted)
                    "Incoming messages from WhatsApp, Telegram, or Messages will be captured, translated to Tamil, and reposted."
                else
                    "Please tap 'Grant Access' above to allow Tamil Notify to intercept notifications.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun NotificationFeedCard(item: CapturedNotification) {
    val context = LocalContext.current
    val timeFormatted = DateFormat.getTimeFormat(context).format(Date(item.postTime))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // App Header & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Orange500.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.appName.take(1).uppercase(),
                            color = Orange500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${item.packageName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Notification Title
            if (item.title.isNotBlank()) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate100
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Original Text
            val displayText = when {
                !item.bigText.isNullOrBlank() -> item.bigText
                item.text.isNotBlank() -> item.text
                else -> "(No text content)"
            }

            Text(
                text = "Original: $displayText",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400
            )

            // Tamil Translation (if translated)
            if (!item.translatedText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Orange500.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = Orange500,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "தமிழ் மொழிபெயர்ப்பு",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Orange500
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.translatedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate100,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (item.translationStatus) {
                    TranslationStatus.TRANSLATED -> {
                        AttributeBadge(label = "✓ Translated & Reposted", color = Emerald400)
                    }
                    TranslationStatus.TRANSLATING -> {
                        AttributeBadge(label = "Translating...", color = Orange400)
                    }
                    TranslationStatus.SKIPPED -> {
                        AttributeBadge(
                            label = "Skipped (${item.reasonSkipped ?: "Filtered"})",
                            color = Slate400
                        )
                    }
                    TranslationStatus.FAILED -> {
                        AttributeBadge(
                            label = "Failed (${item.reasonSkipped ?: "Error"})",
                            color = Rose500
                        )
                    }
                    TranslationStatus.PENDING -> {
                        AttributeBadge(label = "Pending", color = Slate400)
                    }
                }

                if (item.isOngoing) {
                    AttributeBadge(label = "Ongoing", color = Amber500)
                }
            }
        }
    }
}

@Composable
private fun AttributeBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
