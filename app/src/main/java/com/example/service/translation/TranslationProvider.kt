package com.example.service.translation

interface TranslationProvider {
    val providerName: String

    suspend fun translate(
        text: String,
        sourceLangCode: String,
        targetLangCode: String,
        tone: String? = null
    ): TranslationResult

    suspend fun detectLanguage(text: String): String?
}
