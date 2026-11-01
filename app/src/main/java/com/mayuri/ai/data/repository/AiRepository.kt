package com.mayuri.ai.data.repository

import com.mayuri.ai.data.local.SecurePreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
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

@Serializable
private data class GeminiPart(val text: String = "")

@Serializable
private data class GeminiContent(val parts: List<GeminiPart> = emptyList())

@Serializable
private data class GeminiCandidate(val content: GeminiContent? = null)

@Serializable
private data class GeminiPromptFeedback(val blockReason: String? = null)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val promptFeedback: GeminiPromptFeedback? = null
)

@Serializable
private data class OpenAiMessage(val content: String? = null)

@Serializable
private data class OpenAiChoice(val message: OpenAiMessage? = null)

@Serializable
private data class OpenAiResponse(val choices: List<OpenAiChoice> = emptyList())

class AiRepository(private val prefs: SecurePreferencesManager) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json".toMediaType()

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
        {"actionType": "CONVERSATION", "target": null, "reply": "Main aapse baat karne ke liye hamesha tayyar hoon!"}
        Return ONLY valid JSON.
    """.trimIndent()

    suspend fun processQuery(userInput: String): Result<AiIntentResult> = withContext(Dispatchers.IO) {
        val raw: Result<String> = if (prefs.getProvider() == "OPENAI") {
            val apiKey = prefs.getOpenAiApiKey()
                ?: return@withContext Result.failure(
                    Exception("OpenAI API key set nahi hai. Settings me key daalein.")
                )
            callOpenAi(apiKey, userInput, expectJson = true)
        } else {
            val apiKey = prefs.getGeminiApiKey()
                ?: return@withContext Result.failure(
                    Exception("Gemini API key set nahi hai. Settings me key daalein.")
                )
            callGemini(apiKey, userInput, expectJson = true)
        }

        raw.mapCatching { text ->
            json.decodeFromString<AiIntentResult>(extractJsonBlock(text))
        }
    }

    suspend fun generateCode(prompt: String, language: String): Result<String> =
        withContext(Dispatchers.IO) {
            val instruction = "You are a lead software engineer. Write clean, production-ready " +
                "$language code with a short explanation for: $prompt. " +
                "Output the code inside a standard markdown code block."

            val geminiKey = prefs.getGeminiApiKey()
            val openAiKey = prefs.getOpenAiApiKey()

            when {
                geminiKey != null -> callGemini(geminiKey, instruction, expectJson = false)
                openAiKey != null -> callOpenAi(openAiKey, instruction, expectJson = false)
                else -> Result.failure(
                    Exception("Kripya Settings me jakar API key configure karein.")
                )
            }
        }

    // ---------- Gemini ----------

    private fun callGemini(apiKey: String, prompt: String, expectJson: Boolean): Result<String> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent"

        val payload = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", prompt) }
                    }
                }
            }
            if (expectJson) {
                putJsonObject("generationConfig") {
                    put("responseMimeType", "application/json")
                }
            }
        }.toString()

        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", apiKey)
            .post(payload.toRequestBody(mediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Gemini error ${response.code}: ${body.take(300)}")
                    )
                }
                val parsed = json.decodeFromString<GeminiResponse>(body)
                val text = parsed.candidates.firstOrNull()
                    ?.content?.parts?.firstOrNull()?.text
                if (text.isNullOrBlank()) {
                    val reason = parsed.promptFeedback?.blockReason ?: "koi jawab nahi mila"
                    Result.failure(Exception("Gemini se jawab nahi aaya ($reason)."))
                } else {
                    Result.success(text)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- OpenAI ----------

    private fun callOpenAi(apiKey: String, prompt: String, expectJson: Boolean): Result<String> {
        val url = "https://api.openai.com/v1/chat/completions"

        val payload = buildJsonObject {
            put("model", OPENAI_MODEL)
            if (expectJson) {
                putJsonObject("response_format") { put("type", "json_object") }
            }
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
        }.toString()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toRequestBody(mediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("OpenAI error ${response.code}: ${body.take(300)}")
                    )
                }
                val parsed = json.decodeFromString<OpenAiResponse>(body)
                val text = parsed.choices.firstOrNull()?.message?.content
                if (text.isNullOrBlank()) {
                    Result.failure(Exception("OpenAI se jawab nahi aaya."))
                } else {
                    Result.success(text)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Models sometimes wrap JSON in ```json fences or add prose around it. */
    private fun extractJsonBlock(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.removePrefix("```json").removePrefix("```")
            text = text.substringBeforeLast("```").trim()
        }
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }

    private companion object {
        const val GEMINI_MODEL = "gemini-3.8-flash"
        const val OPENAI_MODEL = "gpt-4o-mini"
    }
}
