package com.tamilnotify.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.nl.translate.TranslateLanguage
import com.tamilnotify.app.data.DisplayMode
import com.tamilnotify.app.data.UserSettings
import com.tamilnotify.app.ml.ModelStatus
import com.tamilnotify.app.ml.TranslationEngine
import com.tamilnotify.app.pipeline.TokenProtector
import com.tamilnotify.app.ui.theme.Emerald400
import com.tamilnotify.app.ui.theme.Orange500
import com.tamilnotify.app.ui.theme.Rose500
import com.tamilnotify.app.ui.theme.Slate100
import com.tamilnotify.app.ui.theme.Slate200
import com.tamilnotify.app.ui.theme.Slate400
import com.tamilnotify.app.ui.theme.Slate700
import com.tamilnotify.app.ui.theme.Slate800
import com.tamilnotify.app.ui.theme.Slate900
import kotlinx.coroutines.launch

val SUPPORTED_LANGUAGES = listOf(
    TranslateLanguage.TAMIL to "Tamil (தமிழ்)",
    TranslateLanguage.HINDI to "Hindi (हिन्दी)",
    TranslateLanguage.TELUGU to "Telugu (తెలుగు)",
    TranslateLanguage.MALAYALAM to "Malayalam (മലയാളം)",
    TranslateLanguage.KANNADA to "Kannada (ಕನ್ನಡ)",
    TranslateLanguage.MARATHI to "Marathi (मराठी)",
    TranslateLanguage.BENGALI to "Bengali (বাংলা)",
    TranslateLanguage.SPANISH to "Spanish (Español)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userSettings: UserSettings,
    onToggleMaster: (Boolean) -> Unit,
    onChangeTargetLanguage: (String) -> Unit,
    onChangeDisplayMode: (DisplayMode) -> Unit,
    onToggleRequireWifi: (Boolean) -> Unit,
    onDownloadModel: () -> Unit,
    onDeleteModel: () -> Unit
) {
    val modelStatus by TranslationEngine.getInstance().modelStatus.collectAsState()
    val scope = rememberCoroutineScope()

    // Test Sandbox States
    var testInputText by remember {
        mutableStateOf("Please send $450 to Rahul by 5:00 PM. OTP is 492018. Receipt: https://pay.me/123")
    }
    var testOutputText by remember { mutableStateOf("") }
    var isTestingTranslation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Translation Toggle
        item {
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
                            text = "Automatic Background Translation",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            text = "Intercept and translate notifications from enabled apps",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }

                    Switch(
                        checked = userSettings.isTranslationEnabled,
                        onCheckedChange = onToggleMaster,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Slate900,
                            checkedTrackColor = Orange500,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate700
                        )
                    )
                }
            }
        }

        // Target Language Selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = Orange500,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Target Language",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Notifications in English will be translated into this language",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    var languageDropdownExpanded by remember { mutableStateOf(false) }
                    val currentLangName = SUPPORTED_LANGUAGES.firstOrNull { it.first == userSettings.targetLanguage }?.second ?: "Tamil (தமிழ்)"

                    ExposedDropdownMenuBox(
                        expanded = languageDropdownExpanded,
                        onExpandedChange = { languageDropdownExpanded = !languageDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentLangName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageDropdownExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate900,
                                unfocusedContainerColor = Slate900,
                                focusedBorderColor = Orange500,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate100
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = languageDropdownExpanded,
                            onDismissRequest = { languageDropdownExpanded = false }
                        ) {
                            SUPPORTED_LANGUAGES.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onChangeTargetLanguage(code)
                                        languageDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Notification Display Mode
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Notification Display Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose how the reposted notification text is presented",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isBoth = userSettings.displayMode == DisplayMode.BOTH
                        Button(
                            onClick = { onChangeDisplayMode(DisplayMode.BOTH) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBoth) Orange500 else Slate700
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Tamil + Original",
                                color = if (isBoth) Slate900 else Slate200,
                                fontWeight = if (isBoth) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }

                        val isTamilOnly = userSettings.displayMode == DisplayMode.TAMIL_ONLY
                        Button(
                            onClick = { onChangeDisplayMode(DisplayMode.TAMIL_ONLY) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTamilOnly) Orange500 else Slate700
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Tamil Only",
                                color = if (isTamilOnly) Slate900 else Slate200,
                                fontWeight = if (isTamilOnly) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Offline Model Storage Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Offline Translation Model (~30MB)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enables private, zero-latency on-device translation without sending notification contents to cloud servers",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (modelStatus) {
                                is ModelStatus.Ready -> "✓ Model downloaded & active"
                                is ModelStatus.Downloading -> "Downloading..."
                                is ModelStatus.NotDownloaded -> "Not downloaded"
                                is ModelStatus.Checking -> "Checking..."
                                is ModelStatus.Error -> "Error"
                            },
                            color = if (modelStatus is ModelStatus.Ready) Emerald400 else Slate400,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (modelStatus is ModelStatus.NotDownloaded || modelStatus is ModelStatus.Error) {
                                Button(
                                    onClick = onDownloadModel,
                                    colors = ButtonDefaults.buttonColors(containerColor = Orange500),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Slate900,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", color = Slate900, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (modelStatus is ModelStatus.Ready) {
                                OutlinedButton(
                                    onClick = onDeleteModel,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Rose500,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", color = Rose500, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive Translation Sandbox
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = Orange500,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interactive Translation Sandbox",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Test translation with token protection (URLs, OTPs, currency, dates preserved)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = testInputText,
                        onValueChange = { testInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Slate900,
                            unfocusedContainerColor = Slate900,
                            focusedBorderColor = Orange500,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100
                        ),
                        shape = RoundedCornerShape(10.dp),
                        label = { Text("English Input", color = Slate400) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isTestingTranslation = true
                                val protected = TokenProtector.protectTokens(testInputText)
                                val res = TranslationEngine.getInstance().translate(
                                    protected.maskedText,
                                    userSettings.targetLanguage
                                )
                                res.fold(
                                    onSuccess = { translated ->
                                        testOutputText = TokenProtector.restoreTokens(translated, protected.tokenMap)
                                    },
                                    onFailure = { err ->
                                        testOutputText = "Error: ${err.localizedMessage ?: "Translation failed"}"
                                    }
                                )
                                isTestingTranslation = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Orange500),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isTestingTranslation && testInputText.isNotBlank()
                    ) {
                        if (isTestingTranslation) {
                            CircularProgressIndicator(
                                color = Slate900,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Translating...", color = Slate900, fontSize = 12.sp)
                        } else {
                            Text("Translate on Device", color = Slate900, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (testOutputText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Orange500.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Tamil Output:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Orange500,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = testOutputText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate100
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
