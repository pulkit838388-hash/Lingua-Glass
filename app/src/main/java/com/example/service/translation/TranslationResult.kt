package com.example.service.translation

data class TranslationResult(
    val translatedText: String,
    val detectedSourceLang: String? = null,
    val pronunciationOrRomanization: String? = null,
    val explanation: String? = null,
    val alternatives: List<String> = emptyList(),
    val providerUsed: String = "AI Smart"
)
