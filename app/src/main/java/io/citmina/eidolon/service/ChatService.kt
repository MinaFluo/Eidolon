package io.citmina.eidolon.service

import io.citmina.eidolon.data.ChatMessage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ChatService(private val client: OkHttpClient) {
    companion object {
        private const val BASE_URL = "https://yunwu.ai/v1/chat/completions"
    }

    suspend fun sendChatRequest(messages: List<ChatMessage>, model: String, apiKey: String, onChunk: suspend (String) -> Unit): String? {
        val jsonMessages = JSONArray()
        messages.forEach { message ->
            val jsonMessage = JSONObject().apply {
                put("role", if (message.isUser) "user" else "assistant")
                put("content", message.content)
            }
            jsonMessages.put(jsonMessage)
        }

        val jsonBody = JSONObject().apply {
            put("model", model)
            put("messages", jsonMessages)
            put("stream", true)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("请求失败: ${response.code}")
            var fullResponse = ""
            response.body?.source()?.let { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue
                    if (line.startsWith("data: ")) {
                        val json = line.substring(6)
                        if (json == "[DONE]") continue
                        val jsonObject = JSONObject(json)
                        val choices = jsonObject.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val delta = choices.getJSONObject(0).getJSONObject("delta")
                            if (delta.has("content")) {
                                val content = delta.getString("content")
                                fullResponse += content
                                onChunk(content)
                            }
                        }
                    }
                }
            }
            fullResponse
        }
    }
} 