package com.jarvis.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvis.assistant.ui.screens.DashboardScreen
import com.jarvis.assistant.ui.screens.SettingsScreen
import com.jarvis.assistant.ui.theme.JarvisTheme
import com.jarvis.assistant.viewmodel.JarvisViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            JarvisTheme {
                val viewModel: JarvisViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                var showSettings by remember { mutableStateOf(false) }
                var hasMicPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasMicPermission = granted
                    if (granted) {
                        viewModel.toggleListening()
                    }
                }

                if (showSettings) {
                    SettingsScreen(
                        settings = uiState.settings,
                        hasMicPermission = hasMicPermission,
                        onRequestMicPermission = {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onUpdateSettings = viewModel::updateSettings,
                        onClearHistory = viewModel::clearConversation,
                        onBack = { showSettings = false }
                    )
                } else {
                    DashboardScreen(
                        state = uiState,
                        onInputChanged = viewModel::onInputChanged,
                        onSendClick = { viewModel.sendMessage() },
                        onMicClick = {
                            if (hasMicPermission) {
                                viewModel.toggleListening()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopSpeakingClick = viewModel::stopSpeaking,
                        onClearChatClick = viewModel::clearConversation,
                        onOpenSettingsClick = { showSettings = true },
                        onDismissError = viewModel::clearError
                    )
                }
            }
        }
    }
}
