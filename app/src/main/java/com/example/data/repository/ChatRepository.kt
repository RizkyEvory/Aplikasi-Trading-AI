package com.example.data.repository

import com.example.data.api.OpenRouterChatRequest
import com.example.data.api.OpenRouterMessage
import com.example.data.api.RetrofitClient
import com.example.data.local.ChatDao
import com.example.data.model.ChatMessage
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ChatRepository(
    private val chatDao: ChatDao,
    private val keyManager: KeyManager
) {

    val chatMessagesStream: Flow<List<ChatMessage>> = chatDao.getAllChat()

    suspend fun clearHistory() {
        chatDao.clear()
    }

    suspend fun sendMessage(content: String): String = withContext(Dispatchers.IO) {
        // Save user message
        chatDao.insert(ChatMessage(sender = "USER", content = content))

        val orKey = keyManager.getApiKey("OPENROUTER")
        var responseText = ""

        if (orKey.isNotBlank()) {
            try {
                val flowMessages = mutableListOf<OpenRouterMessage>()
                // Fetch context
                flowMessages.add(OpenRouterMessage("system", "You are the TradeAI Pro Assistant developed by M4DI~UciH4. Your accent is golden, professional, explaining high-tier technical scalp signals, trading strategies, FVG, order blocks, indicators (EMA cross, RSI, MACD, Bollinger Bands), and general market analysis. Answer directly and precisely."))
                flowMessages.add(OpenRouterMessage("user", content))

                val request = OpenRouterChatRequest(
                    model = "openrouter/free",
                    messages = flowMessages
                )
                val response = RetrofitClient.openRouterService.checkCompletion(
                    bearerToken = "Bearer $orKey",
                    request = request
                )
                val text = response.choices?.firstOrNull()?.message?.content
                if (text != null && text.isNotBlank()) {
                    responseText = text
                } else if (response.error != null) {
                    keyManager.markLimitReached("OPENROUTER")
                }
            } catch (e: Exception) {
                // fall down to fallback
            }
        }

        // --- FALLBACK to Workspace Gemini API if OpenRouter key is empty/fails ---
        if (responseText.isBlank()) {
            responseText = callGeminiFallback(content)
        }

        // Save AI response
        chatDao.insert(ChatMessage(sender = "AI", content = responseText))
        return@withContext responseText
    }

    private fun callGeminiFallback(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return "TradeAI Advisor: Please set your OpenRouter API Key or Gemini API Key in Settings to enable live neural analyses."
        }

        return try {
            val client = OkHttpClient()
            // We use the recommended gemini-3.5-flash as per skill guidelines
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            // Construct JSON manually to keep it free from complex class dependencies
            val jsonPayload = """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "You are TradeAI Pro Chatbot Advisor, built by M4DI~UciH4. Analyze: $prompt"
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonPayload.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val rawBody = response.body?.string() ?: ""
                parseGeminiResponse(rawBody)
            } else {
                "Neural Engine Offline (API returned: ${response.code}). Check your internet connection or keys."
            }
        } catch (e: Exception) {
            "Analysis Error: Could not reach neural networks. Technical: ${e.localizedMessage}"
        }
    }

    private fun parseGeminiResponse(json: String): String {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val mapAdapter = moshi.adapter(Map::class.java)
            val map = mapAdapter.fromJson(json) as? Map<*, *>
            val candidates = map?.get("candidates") as? List<*>
            val firstCandidate = candidates?.firstOrNull() as? Map<*, *>
            val content = firstCandidate?.get("content") as? Map<*, *>
            val parts = content?.get("parts") as? List<*>
            val firstPart = parts?.firstOrNull() as? Map<*, *>
            val text = firstPart?.get("text") as? String
            text ?: "Unable to read neural signal."
        } catch (e: Exception) {
            "Response Decoding Error."
        }
    }
}
