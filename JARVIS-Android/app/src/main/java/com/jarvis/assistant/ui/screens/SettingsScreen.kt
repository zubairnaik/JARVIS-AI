package com.jarvis.assistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.data.JarvisSettings
import com.jarvis.assistant.ui.theme.JarvisColors

@Composable
fun SettingsScreen(
    settings: JarvisSettings,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onUpdateSettings: ((JarvisSettings) -> JarvisSettings) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(JarvisColors.DeepBlack, JarvisColors.DarkNavy)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to dashboard",
                    tint = JarvisColors.HoloCyan
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = JarvisColors.TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingCard(title = "Voice & Speech") {
                SettingToggleRow(
                    title = "Voice Responses (TTS)",
                    subtitle = "Speak JARVIS replies aloud",
                    checked = settings.voiceRepliesEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings { it.copy(voiceRepliesEnabled = enabled) }
                    }
                )
                SettingToggleRow(
                    title = "Auto-Speak Replies",
                    subtitle = "Automatically read new responses",
                    checked = settings.autoSpeakResponses,
                    onCheckedChange = { enabled ->
                        onUpdateSettings { it.copy(autoSpeakResponses = enabled) }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Speech Rate (${String.format("%.2fx", settings.speechRate)})",
                    color = JarvisColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = settings.speechRate,
                    onValueChange = { value ->
                        onUpdateSettings { it.copy(speechRate = value) }
                    },
                    valueRange = 0.7f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisColors.HoloCyan,
                        activeTrackColor = JarvisColors.CoreBlue
                    )
                )

                Text(
                    text = "Voice Pitch (${String.format("%.2f", settings.speechPitch)})",
                    color = JarvisColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = settings.speechPitch,
                    onValueChange = { value ->
                        onUpdateSettings { it.copy(speechPitch = value) }
                    },
                    valueRange = 0.7f..1.3f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisColors.HoloCyan,
                        activeTrackColor = JarvisColors.CoreBlue
                    )
                )
            }

            SettingCard(title = "Language Recognition") {
                Text(
                    text = "Primary voice input locale (JARVIS understands English, Hindi & Roman Hindi)",
                    color = JarvisColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "en-IN" to "English (IN)",
                        "hi-IN" to "Hindi",
                        "en-US" to "English (US)"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = settings.recognitionLocale == code,
                            onClick = {
                                onUpdateSettings { it.copy(recognitionLocale = code) }
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisColors.CoreBlue.copy(alpha = 0.25f),
                                selectedLabelColor = JarvisColors.HoloCyan,
                                labelColor = JarvisColors.TextSecondary
                            )
                        )
                    }
                }
            }

            SettingCard(title = "Interface & Persona") {
                SettingToggleRow(
                    title = "Typing Animation",
                    subtitle = "Stream characters progressively",
                    checked = settings.typingAnimationEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings { it.copy(typingAnimationEnabled = enabled) }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.userPreferredName,
                    onValueChange = { name ->
                        onUpdateSettings { it.copy(userPreferredName = name) }
                    },
                    label = { Text("Preferred Form of Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisColors.HoloCyan,
                        unfocusedBorderColor = JarvisColors.BorderHairline,
                        focusedTextColor = JarvisColors.TextPrimary,
                        unfocusedTextColor = JarvisColors.TextPrimary
                    )
                )
            }

            SettingCard(title = "Permissions & Data") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Microphone Access",
                            color = JarvisColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (hasMicPermission) "Granted" else "Permission required for voice",
                            color = if (hasMicPermission) JarvisColors.HoloCyan else JarvisColors.AlertCoral,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (!hasMicPermission) {
                        Button(
                            onClick = onRequestMicPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisColors.CoreBlue,
                                contentColor = JarvisColors.DeepBlack
                            )
                        ) {
                            Text("Grant")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClearHistory,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisColors.AlertCoral.copy(alpha = 0.18f),
                        contentColor = JarvisColors.AlertCoral
                    )
                ) {
                    Text("Clear Conversation History")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = JarvisColors.ElevatedNavy,
        border = BorderStroke(1.dp, JarvisColors.BorderHairline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = JarvisColors.HoloCyan
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = JarvisColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = JarvisColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = JarvisColors.DeepBlack,
                checkedTrackColor = JarvisColors.HoloCyan
            )
        )
    }
}
