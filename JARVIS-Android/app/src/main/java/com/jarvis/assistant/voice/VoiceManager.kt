package com.jarvis.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class VoiceManager(
    private val context: Context,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onRmsLevelChanged: (Float) -> Unit,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onSpeechError: (String) -> Unit,
    private val onSpeakingChanged: (Boolean) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                textToSpeech?.language = Locale("en", "IN")
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingChanged(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingChanged(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeakingChanged(false)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        onSpeakingChanged(false)
                    }
                })
            }
        }
    }

    fun startListening(localeTag: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onSpeechError("Speech recognition service is unavailable on this device.")
            return
        }

        stopSpeaking()
        destroyRecognizer()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onListeningChanged(true)
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    onRmsLevelChanged(normalized)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    onListeningChanged(false)
                    onRmsLevelChanged(0f)
                }

                override fun onError(error: Int) {
                    onListeningChanged(false)
                    onRmsLevelChanged(0f)
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                        else -> "Speech recognition interrupted (code $error)"
                    }
                    onSpeechError(message)
                }

                override fun onResults(results: Bundle?) {
                    onListeningChanged(false)
                    onRmsLevelChanged(0f)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty().trim()
                    if (text.isNotEmpty()) {
                        onFinalResult(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches =
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull().orEmpty()
                    if (partial.isNotEmpty()) {
                        onPartialResult(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        onListeningChanged(false)
        onRmsLevelChanged(0f)
    }

    fun speak(text: String, rate: Float, pitch: Float, fallbackLocaleTag: String) {
        if (!isTtsReady || text.isBlank()) return

        val containsDevanagari = text.any { it in '\u0900'..'\u097F' }
        val targetLocale = when {
            containsDevanagari -> Locale("hi", "IN")
            else -> Locale.forLanguageTag(fallbackLocaleTag)
        }

        textToSpeech?.language = targetLocale
        textToSpeech?.setSpeechRate(rate.coerceIn(0.6f, 1.8f))
        textToSpeech?.setPitch(pitch.coerceIn(0.6f, 1.6f))

        val cleanText = text
            .replace(Regex("[*_`#>]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        textToSpeech?.speak(
            cleanText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            UUID.randomUUID().toString()
        )
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        onSpeakingChanged(false)
    }

    private fun destroyRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun release() {
        destroyRecognizer()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
