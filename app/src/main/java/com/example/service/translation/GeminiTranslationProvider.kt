package com.example.service.translation

import com.example.BuildConfig
import com.example.data.model.Languages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiTranslationProvider(
    private val customApiKeyProvider: () -> String = { "" }
) : TranslationProvider {

    override val providerName: String = "Gemini AI Smart"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getActiveApiKey(): String {
        val customKey = customApiKeyProvider().trim()
        if (customKey.isNotEmpty() && customKey != "MY_GEMINI_API_KEY") {
            return customKey
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        return if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    override suspend fun translate(
        text: String,
        sourceLangCode: String,
        targetLangCode: String,
        tone: String?
    ): TranslationResult = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isEmpty()) {
            throw IllegalStateException("Gemini API key is not configured. Please add your key in Settings or AI Studio Secrets.")
        }

        val sourceLangObj = Languages.findByCode(sourceLangCode)
        val targetLangObj = Languages.findByCode(targetLangCode)

        val sourceDesc = if (sourceLangCode == "auto") "automatically detected language" else "${sourceLangObj.name} (${sourceLangObj.code})"
        val targetDesc = "${targetLangObj.name} (${targetLangObj.code})"

        val toneInstruction = if (!tone.isNullOrBlank()) "Use a $tone tone." else "Ensure natural, native-level phrasing."

        val prompt = """
            You are Lingua Glass, a high-precision linguistic translation engine.
            Translate the following text from $sourceDesc into $targetDesc.
            $toneInstruction
            
            Text to translate:
            \"\"\"
            $text
            \"\"\"
            
            Return ONLY a valid JSON object with the following fields:
            {
              "translatedText": "the translated text",
              "detectedLanguage": "ISO 639-1 code of source language (e.g. en, es, zh, ja, hi, etc.)",
              "pronunciation": "pinyin, romanization, or phonetic transcription if source or target uses non-Latin script, else null",
              "explanation": "brief contextual note, nuance, or formality note if helpful, else null",
              "alternatives": ["alternative phrasing 1", "alternative phrasing 2"]
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", prompt)
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val generationConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            }
            put("generationConfig", generationConfig)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "HTTP ${response.code}"
            throw RuntimeException("Gemini API call failed (${response.code}): $errorBody")
        }

        val responseString = response.body?.string() ?: throw RuntimeException("Empty response from Gemini")
        val rootJson = JSONObject(responseString)
        val candidates = rootJson.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text", "") ?: ""

        val parsedResult = try {
            JSONObject(rawText)
        } catch (_: Exception) {
            JSONObject().put("translatedText", rawText.trim())
        }

        val translated = parsedResult.optString("translatedText", rawText).trim()
        val detected = parsedResult.optString("detectedLanguage", sourceLangCode)
        val pronunciation = if (parsedResult.isNull("pronunciation")) null else parsedResult.optString("pronunciation", "").ifEmpty { null }
        val explanation = if (parsedResult.isNull("explanation")) null else parsedResult.optString("explanation", "").ifEmpty { null }
        
        val alternativesList = mutableListOf<String>()
        val altArray = parsedResult.optJSONArray("alternatives")
        if (altArray != null) {
            for (i in 0 until altArray.length()) {
                val alt = altArray.optString(i, "").trim()
                if (alt.isNotEmpty() && alt != translated) {
                    alternativesList.add(alt)
                }
            }
        }

        TranslationResult(
            translatedText = translated,
            detectedSourceLang = detected,
            pronunciationOrRomanization = pronunciation,
            explanation = explanation,
            alternatives = alternativesList,
            providerUsed = "Gemini AI 3.5 Flash"
        )
    }

    override suspend fun detectLanguage(text: String): String? = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isEmpty()) return@withContext null

        try {
            val prompt = "Identify the language of the following text. Respond ONLY with the 2-letter ISO 639-1 language code (e.g., en, es, fr, zh, ja, hi, ar, de, etc.):\n\n$text"
            val jsonRequest = JSONObject().apply {
                val contents = JSONArray().apply {
                    val content = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(content)
                }
                put("contents", contents)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonRequest.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val cand = JSONObject(body).optJSONArray("candidates")?.optJSONObject(0)
                val code = cand?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "")?.trim()?.lowercase()
                if (!code.isNullOrEmpty() && code.length in 2..5) {
                    return@withContext code
                }
            }
        } catch (_: Exception) {
            // Ignore error and return null
        }
        null
    }
}
