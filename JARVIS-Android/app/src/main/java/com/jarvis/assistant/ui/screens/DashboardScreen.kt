package com.jarvis.assistant.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.data.ChatMessage
import com.jarvis.assistant.data.MessageRole
import com.jarvis.assistant.ui.components.JarvisCore
import com.jarvis.assistant.ui.theme.JarvisColors
import com.jarvis.assistant.viewmodel.JarvisUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    state: JarvisUiState,
    onInputChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit,
    onStopSpeakingClick: () -> Unit,
    onClearChatClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onDismissError: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        JarvisColors.DeepBlack,
                        JarvisColors.DarkNavy,
                        JarvisColors.DeepBlack
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with Date & Time and primary actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "JARVIS",
                        style = MaterialTheme.typography.titleMedium,
                        color = JarvisColors.TextPrimary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "${state.formattedTime} · ${state.formattedDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisColors.TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isSpeaking) {
                        IconButton(onClick = onStopSpeakingClick) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeOff,
                                contentDescription = "Stop voice output",
                                tint = JarvisColors.HoloCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = onClearChatClick,
                        enabled = state.messages.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear conversation",
                            tint = if (state.messages.isNotEmpty()) {
                                JarvisColors.TextSecondary
                            } else {
                                JarvisColors.TextSecondary.copy(alpha = 0.35f)
                            }
                        )
                    }

                    IconButton(onClick = onOpenSettingsClick) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Open settings",
                            tint = JarvisColors.HoloCyan
                        )
                    }
                }
            }

            // Center Holographic AI Core
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                JarvisCore(
                    coreState = state.coreState,
                    audioLevel = state.audioLevel,
                    size = if (state.messages.isEmpty()) 210.dp else 148.dp
                )
            }

            // Error notice if speech or API fails
            AnimatedVisibility(visible = state.errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    color = JarvisColors.ElevatedNavy,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        JarvisColors.AlertCoral.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = JarvisColors.AlertCoral,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissError) {
                            Text("Dismiss", color = JarvisColors.TextPrimary)
                        }
                    }
                }
            }

            // Chat History Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Ready to assist in English, Hindi, or Roman Hindi.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickPromptButton(
                                    label = "Brief me today",
                                    onClick = { onInputChanged("Give me a quick morning briefing."); onSendClick() }
                                )
                                QuickPromptButton(
                                    label = "Aaj ka plan?",
                                    onClick = { onInputChanged("Aaj ka schedule kaise plan karein?"); onSendClick() }
                                )
                            }
                        }
                    }
                } else {
                    items(state.messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            isStreaming = msg.id == state.streamingMessageId
                        )
                    }
                }
            }

            // Bottom Thumb-Zone Input & Microphone Dock
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = JarvisColors.DarkNavy.copy(alpha = 0.92f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, JarvisColors.BorderHairline)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = onMicClick,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (state.isListening) {
                                JarvisColors.AlertCoral
                            } else {
                                JarvisColors.CoreBlue
                            },
                            contentColor = JarvisColors.DeepBlack
                        )
                    ) {
                        Icon(
                            imageVector = if (state.isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = if (state.isListening) "Stop listening" else "Start voice input"
                        )
                    }

                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = onInputChanged,
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = if (state.isListening) "Listening..." else "Message JARVIS...",
                                color = JarvisColors.TextSecondary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisColors.HoloCyan,
                            unfocusedBorderColor = JarvisColors.BorderHairline,
                            focusedTextColor = JarvisColors.TextPrimary,
                            unfocusedTextColor = JarvisColors.TextPrimary,
                            focusedContainerColor = JarvisColors.DeepBlack.copy(alpha = 0.6f),
                            unfocusedContainerColor = JarvisColors.DeepBlack.copy(alpha = 0.6f)
                        )
                    )

                    FilledIconButton(
                        onClick = onSendClick,
                        enabled = state.inputText.isNotBlank() && !state.isThinking,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = JarvisColors.HoloCyan,
                            contentColor = JarvisColors.DeepBlack,
                            disabledContainerColor = JarvisColors.ElevatedNavy,
                            disabledContentColor = JarvisColors.TextSecondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send message"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPromptButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = JarvisColors.ElevatedNavy,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisColors.BorderHairline)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = JarvisColors.ArcGlow,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isStreaming: Boolean
) {
    val isUser = message.role == MessageRole.USER
    val timeText = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(message.timestampMillis))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isUser) JarvisColors.CoreBlue.copy(alpha = 0.18f)
                    else JarvisColors.ElevatedNavy
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) JarvisColors.HoloCyan.copy(alpha = 0.4f)
                    else JarvisColors.BorderHairline,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isUser) "You" else "JARVIS",
                    color = if (isUser) JarvisColors.ArcGlow else JarvisColors.HoloCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = timeText,
                    color = JarvisColors.TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isStreaming && message.text.isEmpty()) "Thinking..." else message.text + if (isStreaming) " ▌" else "",
                color = JarvisColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
