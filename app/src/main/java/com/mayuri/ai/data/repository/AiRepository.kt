package com.mayuri.ai.data.repository

import com.mayuri.ai.data.local.SecurePreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class AiIntentResult(
    val actionType: String,      // "CONVERSATION", "CODE", "DEVICE_CONTROL", "CALL", "OPEN_APP", "SOCIAL"
    val target: String? = null,  // Target app name, setting type, or contact name
    val reply: String            // Voice response in sweet, polite Hindi/Hinglish
)

class AiRepository(private val prefs: SecurePreferencesManager) {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        You are MAYURI, an intelligent, sweet, and caring female AI voice assistant for Android.
        You understand Hindi, English, and Hinglish. Always speak respectfully and politely.
        When asked to perform tasks, return a strict JSON response with:
        - actionType: "CONVERSATION" | "CODE" | "DEVICE_CONTROL" | "CALL" | "OPEN_APP" | "SOCIAL"
        - target: name of application, contact, flashlight parameter, or volume direction.
        - reply: sweet, polite Hindi/Hinglish response to be spoken aloud.
        
        Example outputs:
        {"actionType": "DEVICE_CONTROL", "target": "flashlight_on", "reply": "Ji, main abhi flashlight on kar rahi hoon."}
        {"actionType": "OPEN_APP", "target": "youtube", "reply": "Zaroor! YouTube khola ja raha hai."}
        {"actionType": "CALL", "target": "Rahul", "reply": "Ji, main Rahul ko call mila rahi hoon."}
        {"actionType": "CONVERSATION", "target": null, "reply": "Main aapse baat karne ke liye hamesha tayar hoon!"}
        Return ONLY valid JSON.
    """.trimIndent()

    suspend fun processQuery(userInput: String): Result<AiIntentResult> = withContext(Dispatchers.IO) {
        val provider = prefs.getProvider()
        return@withContext if (provider == "OPENAI") {
            processWithOpenAi(userInput)
        } else {
            processWithGemini(userInput)
        }
    }

    private fun processWithGemini(userInput: String): Result<AiIntentResult> {
        val apiKey = prefs.getGeminiApiKey() ?: return Result.failure(Exception("Gemini API Key missing."))
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val requestBodyJson = """
            {
                "contents": [
                    {
                        "parts": [
                            {"text": "$systemPrompt\n\nUser: $userInput"}
                        ]
                    }
                ],
                "generationConfig": {
                    "response_mime_type": "application/json"
                }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return Result.failure(Exception("Gemini Error: ${response.code} $responseString"))
            }

            // Extract the generated JSON candidate
            val jsonText = responseString
                .substringAfter("\"text\": \"")
                .substringBeforeLast("\"")
                .replace("\\n", "\n")
                .replace("\\\"", "\"")

            val intentResult = json.decodeFromString<AiIntentResult>(jsonText)
            Result.success(intentResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun processWithOpenAi(userInput: String): Result<AiIntentResult> {
        val apiKey = prefs.getOpenAiApiKey() ?: return Result.failure(Exception("OpenAI API Key missing."))
        val url = "https://api.openai.com/v1/chat/completions"

        val requestBodyJson = """
            {
                "model": "gpt-4o-mini",
                "response_format": { "type": "json_object" },
                "messages": [
                    {"role": "system", "content": "$systemPrompt"},
                    {"role": "user", "content": "$userInput"}
                ]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return Result.failure(Exception("OpenAI Error: ${response.code} $responseString"))
            }

            val contentJson = responseString
                .substringAfter("\"content\": \"")
                .substringBeforeLast("\"")
                .replace("\\n", "\n")
                .replace("\\\"", "\"")

            val intentResult = json.decodeFromString<AiIntentResult>(contentJson)
            Result.success(intentResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Direct Code Studio Code Generation
    suspend fun generateCode(prompt: String, language: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = prefs.getGeminiApiKey() ?: prefs.getOpenAiApiKey()
        ?: return@withContext Result.failure(Exception("Kripya Settings me jakar API Key configure karein."))

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val payload = """
            {
                "contents": [
                    {
                        "parts": [
                            {"text": "You are a lead software engineer. Write clean, production-ready $language code with explanations for: $prompt. Output the code inside standard markdown codeblocks."}
                        ]
                    }
                ]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            val output = body.substringAfter("\"text\": \"").substringBeforeLast("\"")
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
            Result.success(output)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}