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
            throw IllegalStateException("GEMINI_API_KEY is not configured.")
        }

        val contentsArray = JSONArray()

        history.takeLast(16).forEach { msg ->
            val item = JSONObject()

            item.put(
                "role",
                if (msg.role == MessageRole.USER) "user" else "model"
            )

            val textObject = JSONObject()
            textObject.put("text", msg.text)

            val parts = JSONArray()
            parts.put(textObject)

            item.put("parts", parts)
            contentsArray.put(item)
        }

        val systemInstruction = JSONObject()
        val instructionParts = JSONArray()
        val instructionText = JSONObject()

        instructionText.put(
            "text",
            """
            You are JARVIS, an intelligent personal AI assistant.

            Address the user as $userPreferredName when natural.

            Understand English, Hindi and Roman Hindi.
            Reply in the same language and script as the user.
            Keep responses concise and helpful.

            Use Google Search for current information,
            recent news and facts that may have changed.
            Never pretend to search if you did not.
            """.trimIndent()
        )

        instructionParts.put(instructionText)
        systemInstruction.put("parts", instructionParts)

        val searchConfig = JSONObject()
        val searchTool = JSONObject()
        searchTool.put("google_search", searchConfig)

        val toolsArray = JSONArray()
        toolsArray.put(searchTool)

        val payload = JSONObject()
        payload.put("systemInstruction", systemInstruction)
        payload.put("contents", contentsArray)
        payload.put("tools", toolsArray)

        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.GEMINI_MODEL}:streamGenerateContent?alt=sse"

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(
                payload.toString().toRequestBody(
                    "application/json".toMediaType()
                )
            )
            .build()

        client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                throw IllegalStateException(
                    "Gemini API error (${response.code}): $errorBody"
                )
            }

            val body = response.body
                ?: throw IllegalStateException("Empty response body")

            BufferedReader(body.charStream()).use { reader ->

                var line: String?

                while (reader.readLine().also { line = it } != null) {

                    val trimmed = line?.trim().orEmpty()

                    if (!trimmed.startsWith("data:")) continue

                    val jsonString = trimmed.removePrefix("data:").trim()

                    if (jsonString.isEmpty() || jsonString == "[DONE]") {
                        continue
                    }

                    val chunk = extractTextChunk(jsonString)

                    if (chunk.isNotEmpty()) {
                        emit(chunk)
                    }
                }
            }
        }

    }.flowOn(Dispatchers.IO)

    private fun extractTextChunk(jsonString: String): String {
        return try {

            val root = JSONObject(jsonString)

            val candidates = root.optJSONArray("candidates")
                ?: return ""

            if (candidates.length() == 0) return ""

            val content = candidates
                .getJSONObject(0)
                .optJSONObject("content")
                ?: return ""

            val parts = content.optJSONArray("parts")
                ?: return ""

            val result = StringBuilder()

            for (i in 0 until parts.length()) {
                result.append(
                    parts.getJSONObject(i).optString("text", "")
                )
            }

            result.toString()

        } catch (_: Exception) {
            ""
        }
    }
}
