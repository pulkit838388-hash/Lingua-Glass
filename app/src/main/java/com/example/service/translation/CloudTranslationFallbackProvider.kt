package com.example.service.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class CloudTranslationFallbackProvider : TranslationProvider {

    override val providerName: String = "Fast Multilingual Cloud"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun translate(
        text: String,
        sourceLangCode: String,
        targetLangCode: String,
        tone: String?
    ): TranslationResult = withContext(Dispatchers.IO) {
        val src = if (sourceLangCode == "auto") "Autodetect" else sourceLangCode
        val tgt = targetLangCode

        val encodedText = URLEncoder.encode(text, "UTF-8")
        val langPair = if (src == "Autodetect") tgt else "$src|$tgt"
        val url = "https://api.mymemory.translated.net/get?q=$encodedText&langpair=$langPair"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "LinguaGlass-Android/1.0")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Cloud translation service returned error HTTP ${response.code}")
        }

        val bodyString = response.body?.string() ?: throw RuntimeException("Empty response from translation provider")
        val json = JSONObject(bodyString)
        val responseData = json.optJSONObject("responseData")
        val translatedText = responseData?.optString("translatedText", "") ?: ""

        if (translatedText.isBlank()) {
            throw RuntimeException("Could not retrieve translation from cloud service")
        }

        // Match detected language or alternatives if present in 'matches'
        val matchesArray = json.optJSONArray("matches")
        val alternatives = mutableListOf<String>()
        if (matchesArray != null) {
            for (i in 0 until minOf(3, matchesArray.length())) {
                val matchObj = matchesArray.optJSONObject(i)
                val matchTrans = matchObj?.optString("translation", "")?.trim()
                if (!matchTrans.isNullOrEmpty() && matchTrans != translatedText && !alternatives.contains(matchTrans)) {
                    alternatives.add(matchTrans)
                }
            }
        }

        TranslationResult(
            translatedText = translatedText,
            detectedSourceLang = if (sourceLangCode == "auto") "auto" else sourceLangCode,
            alternatives = alternatives,
            explanation = "Translated via Fast Multilingual Cloud Network",
            providerUsed = "Fast Cloud Network"
        )
    }

    override suspend fun detectLanguage(text: String): String? {
        return null
    }
}
