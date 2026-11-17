package com.jarvis.assistant.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "jarvis_preferences")

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val timestampMillis: Long
)

enum class MessageRole {
    USER, ASSISTANT
}

data class JarvisSettings(
    val voiceRepliesEnabled: Boolean = true,
    val autoSpeakResponses: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 0.95f,
    val recognitionLocale: String = "en-IN",
    val typingAnimationEnabled: Boolean = true,
    val userPreferredName: String = "Sir"
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val VOICE_REPLIES = booleanPreferencesKey("voice_replies")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val LOCALE = stringPreferencesKey("recognition_locale")
        val TYPING_ANIM = booleanPreferencesKey("typing_animation")
        val PREFERRED_NAME = stringPreferencesKey("preferred_name")
        val CHAT_HISTORY = stringPreferencesKey("chat_history_json")
    }

    val settingsFlow: Flow<JarvisSettings> = context.dataStore.data.map { prefs ->
        JarvisSettings(
            voiceRepliesEnabled = prefs[Keys.VOICE_REPLIES] ?: true,
            autoSpeakResponses = prefs[Keys.AUTO_SPEAK] ?: true,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            speechPitch = prefs[Keys.SPEECH_PITCH] ?: 0.95f,
            recognitionLocale = prefs[Keys.LOCALE] ?: "en-IN",
            typingAnimationEnabled = prefs[Keys.TYPING_ANIM] ?: true,
            userPreferredName = prefs[Keys.PREFERRED_NAME] ?: "Sir"
        )
    }

    val chatHistoryFlow: Flow<List<ChatMessage>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.CHAT_HISTORY] ?: "[]"
        parseHistory(raw)
    }

    suspend fun updateSettings(transform: (JarvisSettings) -> JarvisSettings) {
        context.dataStore.edit { prefs ->
            val current = JarvisSettings(
                voiceRepliesEnabled = prefs[Keys.VOICE_REPLIES] ?: true,
                autoSpeakResponses = prefs[Keys.AUTO_SPEAK] ?: true,
                speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
                speechPitch = prefs[Keys.SPEECH_PITCH] ?: 0.95f,
                recognitionLocale = prefs[Keys.LOCALE] ?: "en-IN",
                typingAnimationEnabled = prefs[Keys.TYPING_ANIM] ?: true,
                userPreferredName = prefs[Keys.PREFERRED_NAME] ?: "Sir"
            )
            val next = transform(current)
            prefs[Keys.VOICE_REPLIES] = next.voiceRepliesEnabled
            prefs[Keys.AUTO_SPEAK] = next.autoSpeakResponses
            prefs[Keys.SPEECH_RATE] = next.speechRate
            prefs[Keys.SPEECH_PITCH] = next.speechPitch
            prefs[Keys.LOCALE] = next.recognitionLocale
            prefs[Keys.TYPING_ANIM] = next.typingAnimationEnabled
            prefs[Keys.PREFERRED_NAME] = next.userPreferredName
        }
    }

    suspend fun saveHistory(messages: List<ChatMessage>) {
        val array = JSONArray()
        messages.takeLast(100).forEach { msg ->
            array.put(
                JSONObject().apply {
                    put("id", msg.id)
                    put("role", msg.role.name)
                    put("text", msg.text)
                    put("timestampMillis", msg.timestampMillis)
                }
            )
        }
        context.dataStore.edit { prefs ->
            prefs[Keys.CHAT_HISTORY] = array.toString()
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs[Keys.CHAT_HISTORY] = "[]"
        }
    }

    private fun parseHistory(raw: String): List<ChatMessage> {
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        ChatMessage(
                            id = obj.getString("id"),
                            role = MessageRole.valueOf(obj.getString("role")),
                            text = obj.getString("text"),
                            timestampMillis = obj.getLong("timestampMillis")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
