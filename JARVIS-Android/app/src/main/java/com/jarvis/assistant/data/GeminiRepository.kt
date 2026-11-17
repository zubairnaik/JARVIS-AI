package com.jarvis.assistant.data

import com.jarvis.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

class GeminiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun streamReply(
        history: List<ChatMessage>,
        userPreferredName: String
    ): Flow<String> = flow {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            throw IllegalStateException(
                "GEMINI_API_KEY is not configured. Add GEMINI_API_KEY=... in local.properties."
            )
        }

        val contentsArray = JSONArray()
        history.takeLast(16).forEach { msg ->
            contentsArray.put(
                JSONObject().apply {
                    put("role", if (msg.role == MessageRole.USER) "user" else "model")
                    put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", msg.text))
                    )
                }
            )
        }

        val systemInstruction = JSONObject().apply {
            put(
                "parts",
                JSONArray().put(
                    JSONObject().put(
                        "text",
                        """
                        You are JARVIS, an intelligent, composed, and articulate personal AI assistant inspired by Tony Stark's assistant.
                        Address the user respectfully as $userPreferredName when natural, without overdoing it.
                        Language intelligence:
                        - Fluently understand and respond in English, Hindi (Devanagari), and Roman Hindi (Hinglish).
                        - Automatically mirror the user's language and script: if they write in Roman Hindi (e.g., "bhai kal ka weather kaisa rahega"), reply naturally in Roman Hindi; if they write in Devanagari Hindi, reply in Devanagari Hindi; if in English, reply in crisp English.
                        - Keep responses concise, helpful, and voice-friendly unless the user requests deep detail.
                        """.trimIndent()
                    )
                )
            )
        }

        val payload = JSONObject().apply {
            put("systemInstruction", systemInstruction)
            put("contents", contentsArray)
        }

        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.GEMINI_MODEL}:streamGenerateContent?alt=sse"

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                throw IllegalStateException("Gemini API error (${response.code}): $errBody")
            }

            val body = response.body ?: throw IllegalStateException("Empty response body")
            BufferedReader(body.charStream()).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim().orEmpty()
                    if (!trimmed.startsWith("data:")) continue
                    val jsonStr = trimmed.removePrefix("data:").trim()
                    if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue

                    val chunk = extractTextChunk(jsonStr)
                    if (chunk.isNotEmpty()) {
                        emit(chunk)
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun extractTextChunk(jsonStr: String): String {
        return try {
            val root = JSONObject(jsonStr)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            buildString {
                for (i in 0 until parts.length()) {
                    append(parts.getJSONObject(i).optString("text"))
                }
            }
        } catch (_: Exception) {
            ""
        }
    }
}
