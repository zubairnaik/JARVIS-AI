package com.jarvis.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.data.ChatMessage
import com.jarvis.assistant.data.GeminiRepository
import com.jarvis.assistant.data.JarvisSettings
import com.jarvis.assistant.data.MessageRole
import com.jarvis.assistant.data.SettingsStore
import com.jarvis.assistant.ui.components.CoreState
import com.jarvis.assistant.voice.VoiceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class JarvisUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val streamingMessageId: String? = null,
    val isThinking: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val audioLevel: Float = 0f,
    val errorMessage: String? = null,
    val formattedTime: String = "",
    val formattedDate: String = "",
    val settings: JarvisSettings = JarvisSettings()
) {
    val coreState: CoreState
        get() = when {
            isListening -> CoreState.LISTENING
            isThinking || streamingMessageId != null -> CoreState.THINKING
            isSpeaking -> CoreState.SPEAKING
            else -> CoreState.IDLE
        }
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)
    private val geminiRepository = GeminiRepository()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _transient = MutableStateFlow(JarvisUiState())

    val uiState: StateFlow<JarvisUiState> = combine(
        _messages,
        _transient,
        settingsStore.settingsFlow
    ) { messages, transient, settings ->
        transient.copy(
            messages = messages,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = JarvisUiState()
    )

    private val voiceManager = VoiceManager(
        context = application,
        onListeningChanged = { listening ->
            _transient.update { it.copy(isListening = listening) }
        },
        onRmsLevelChanged = { level ->
            _transient.update { it.copy(audioLevel = level) }
        },
        onPartialResult = { partial ->
            _transient.update { it.copy(inputText = partial, errorMessage = null) }
        },
        onFinalResult = { finalText ->
            _transient.update { it.copy(inputText = "") }
            sendMessage(finalText)
        },
        onSpeechError = { err ->
            _transient.update { it.copy(errorMessage = err) }
        },
        onSpeakingChanged = { speaking ->
            _transient.update { it.copy(isSpeaking = speaking) }
        }
    )

    init {
        viewModelScope.launch {
            settingsStore.chatHistoryFlow.collect { saved ->
                if (_messages.value.isEmpty() && saved.isNotEmpty()) {
                    _messages.value = saved
                }
            }
        }
        startClockLoop()
    }

    private fun startClockLoop() {
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
        val dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.getDefault())
        viewModelScope.launch {
            while (true) {
                val now = ZonedDateTime.now()
                _transient.update {
                    it.copy(
                        formattedTime = now.format(timeFmt),
                        formattedDate = now.format(dateFmt)
                    )
                }
                delay(1000)
            }
        }
    }

    fun onInputChanged(text: String) {
        _transient.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun toggleListening() {
        if (_transient.value.isListening) {
            voiceManager.stopListening()
        } else {
            val locale = uiState.value.settings.recognitionLocale
            voiceManager.startListening(locale)
        }
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun clearError() {
        _transient.update { it.copy(errorMessage = null) }
    }

    fun sendMessage(overrideText: String? = null) {
        val prompt = (overrideText ?: _transient.value.inputText).trim()
        if (prompt.isEmpty() || _transient.value.isThinking) return

        voiceManager.stopSpeaking()

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            text = prompt,
            timestampMillis = System.currentTimeMillis()
        )
        val assistantId = UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessage(
            id = assistantId,
            role = MessageRole.ASSISTANT,
            text = "",
            timestampMillis = System.currentTimeMillis()
        )

        val historySnapshot = _messages.value + userMsg
        _messages.value = historySnapshot + assistantPlaceholder
        _transient.update {
            it.copy(
                inputText = "",
                isThinking = true,
                streamingMessageId = assistantId,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val currentSettings = uiState.value.settings
            val buffer = StringBuilder()

            try {
                geminiRepository.streamReply(
                    history = historySnapshot,
                    userPreferredName = currentSettings.userPreferredName
                ).collect { chunk ->
                    if (currentSettings.typingAnimationEnabled) {
                        for (ch in chunk) {
                            buffer.append(ch)
                            updateAssistantText(assistantId, buffer.toString())
                            delay(10)
                        }
                    } else {
                        buffer.append(chunk)
                        updateAssistantText(assistantId, buffer.toString())
                    }
                }

                val finalReply = buffer.toString().trim()
                updateAssistantText(assistantId, finalReply)
                settingsStore.saveHistory(_messages.value)

                if (currentSettings.voiceRepliesEnabled && currentSettings.autoSpeakResponses) {
                    voiceManager.speak(
                        text = finalReply,
                        rate = currentSettings.speechRate,
                        pitch = currentSettings.speechPitch,
                        fallbackLocaleTag = currentSettings.recognitionLocale
                    )
                }
            } catch (e: Exception) {
                _messages.update { list -> list.filterNot { it.id == assistantId && it.text.isBlank() } }
                _transient.update {
                    it.copy(errorMessage = e.message ?: "Unable to reach JARVIS core.")
                }
            } finally {
                _transient.update {
                    it.copy(isThinking = false, streamingMessageId = null)
                }
            }
        }
    }

    private fun updateAssistantText(id: String, text: String) {
        _messages.update { list ->
            list.map { msg -> if (msg.id == id) msg.copy(text = text) else msg }
        }
    }

    fun clearConversation() {
        voiceManager.stopSpeaking()
        _messages.value = emptyList()
        viewModelScope.launch {
            settingsStore.clearHistory()
        }
    }

    fun updateSettings(transform: (JarvisSettings) -> JarvisSettings) {
        viewModelScope.launch {
            settingsStore.updateSettings(transform)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
