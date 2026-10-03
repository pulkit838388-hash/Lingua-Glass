package com.example.service.translation

import com.example.data.local.TranslationDao
import com.example.data.model.Languages
import com.example.data.model.TranslationItem
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TranslationRepository(
    private val translationDao: TranslationDao,
    private val preferencesRepository: UserPreferencesRepository
) {

    private val geminiProvider = GeminiTranslationProvider {
        // Will get from preferences if set
        ""
    }
    private val cloudProvider = CloudTranslationFallbackProvider()

    val allHistory: Flow<List<TranslationItem>> = translationDao.getAllHistory()
    val favorites: Flow<List<TranslationItem>> = translationDao.getFavorites()

    fun search(query: String): Flow<List<TranslationItem>> = translationDao.searchTranslations(query)

    suspend fun translate(
        text: String,
        sourceLangCode: String,
        targetLangCode: String,
        tone: String? = null,
        saveToHistory: Boolean = true
    ): TranslationResult {
        if (text.isBlank()) {
            return TranslationResult(translatedText = "")
        }

        val prefs = preferencesRepository.userPreferencesFlow.first()
        val preferredEngine = prefs.translationEngine

        var result: TranslationResult? = null
        var lastError: Exception? = null

        if (preferredEngine == "gemini") {
            try {
                // If user configured custom key
                val providerWithCustomKey = if (prefs.customApiKey.isNotBlank()) {
                    GeminiTranslationProvider { prefs.customApiKey }
                } else {
                    geminiProvider
                }
                result = providerWithCustomKey.translate(text, sourceLangCode, targetLangCode, tone)
            } catch (e: Exception) {
                lastError = e
            }
        }

        // Fallback to cloud translation if Gemini failed or user selected cloud
        if (result == null) {
            try {
                result = cloudProvider.translate(text, sourceLangCode, targetLangCode, tone)
            } catch (e: Exception) {
                if (lastError != null) {
                    throw RuntimeException("Translation error: ${lastError.message ?: e.message}")
                } else {
                    throw e
                }
            }
        }

        if (saveToHistory && result.translatedText.isNotBlank()) {
            val sourceLang = Languages.findByCode(
                if (sourceLangCode == "auto" && !result.detectedSourceLang.isNullOrEmpty()) {
                    result.detectedSourceLang
                } else {
                    sourceLangCode
                }
            )
            val targetLang = Languages.findByCode(targetLangCode)

            val item = TranslationItem(
                sourceText = text.trim(),
                translatedText = result.translatedText.trim(),
                sourceLangCode = sourceLang.code,
                targetLangCode = targetLang.code,
                sourceLangName = sourceLang.name,
                targetLangName = targetLang.name,
                notesOrExplanation = result.explanation ?: result.pronunciationOrRomanization,
                providerUsed = result.providerUsed
            )
            translationDao.insertTranslation(item)
        }

        return result
    }

    suspend fun toggleFavorite(item: TranslationItem) {
        translationDao.updateFavorite(item.id, !item.isFavorite)
    }

    suspend fun deleteItem(item: TranslationItem) {
        translationDao.deleteTranslation(item)
    }

    suspend fun clearHistory(keepFavorites: Boolean = true) {
        if (keepFavorites) {
            translationDao.clearNonFavorites()
        } else {
            translationDao.clearAll()
        }
    }
}
