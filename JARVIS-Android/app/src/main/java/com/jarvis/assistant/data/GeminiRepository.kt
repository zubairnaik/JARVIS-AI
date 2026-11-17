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
                "GEMINI_API_KEY is not configured. Add GEMINI_API_KEY in GitHub Actions Secrets."
            )
        }

        val contentsArray = JSONArray()

        history.takeLast(16).forEach { msg ->
            contentsArray.put(
                JSONObject().apply {
                    put(
                        "role",
                        if (msg.role == MessageRole.USER) "user" else "model"
                    )
                    put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put
