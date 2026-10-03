package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lingua_glass_prefs")

enum class AppThemeMode {
    AMOLED_BLACK,
    DARK_GLASS,
    OBSIDIAN
}

enum class AccentPalette(val id: String, val displayName: String, val hex: Long) {
    CYAN("cyan", "Electric Cyan", 0xFF00F0FF),
    VIOLET("violet", "Royal Violet", 0xFF9D4EDD),
    EMERALD("emerald", "Neon Jade", 0xFF00F5A0),
    AMBER("amber", "Sunset Amber", 0xFFFF9E00),
    ROSE("rose", "Cosmic Rose", 0xFFFF3366)
}

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.AMOLED_BLACK,
    val accentPalette: AccentPalette = AccentPalette.CYAN,
    val glassTransparency: Float = 0.85f,
    val floatingBubbleSizeDp: Int = 58,
    val floatingBubbleOpacity: Float = 0.90f,
    val isFloatingBubbleEnabled: Boolean = false,
    val defaultSourceLang: String = "auto",
    val defaultTargetLang: String = "es",
    val autoDetectSource: Boolean = true,
    val autoCopyTranslated: Boolean = false,
    val autoSpeakTranslation: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val translationEngine: String = "gemini", // "gemini" or "cloud"
    val customApiKey: String = ""
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val GLASS_TRANSPARENCY = floatPreferencesKey("glass_transparency")
        val FLOATING_BUBBLE_SIZE = floatPreferencesKey("floating_bubble_size")
        val FLOATING_BUBBLE_OPACITY = floatPreferencesKey("floating_bubble_opacity")
        val FLOATING_BUBBLE_ENABLED = booleanPreferencesKey("floating_bubble_enabled")
        val DEFAULT_SOURCE_LANG = stringPreferencesKey("default_source_lang")
        val DEFAULT_TARGET_LANG = stringPreferencesKey("default_target_lang")
        val AUTO_DETECT_SOURCE = booleanPreferencesKey("auto_detect_source")
        val AUTO_COPY_TRANSLATED = booleanPreferencesKey("auto_copy_translated")
        val AUTO_SPEAK_TRANSLATION = booleanPreferencesKey("auto_speak_translation")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val TRANSLATION_ENGINE = stringPreferencesKey("translation_engine")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.AMOLED_BLACK.name
        val themeMode = try {
            AppThemeMode.valueOf(themeModeStr)
        } catch (_: Exception) {
            AppThemeMode.AMOLED_BLACK
        }

        val paletteStr = preferences[PreferencesKeys.ACCENT_PALETTE] ?: AccentPalette.CYAN.id
        val accentPalette = AccentPalette.values().firstOrNull { it.id == paletteStr } ?: AccentPalette.CYAN

        UserPreferences(
            themeMode = themeMode,
            accentPalette = accentPalette,
            glassTransparency = preferences[PreferencesKeys.GLASS_TRANSPARENCY] ?: 0.85f,
            floatingBubbleSizeDp = (preferences[PreferencesKeys.FLOATING_BUBBLE_SIZE] ?: 58f).toInt(),
            floatingBubbleOpacity = preferences[PreferencesKeys.FLOATING_BUBBLE_OPACITY] ?: 0.90f,
            isFloatingBubbleEnabled = preferences[PreferencesKeys.FLOATING_BUBBLE_ENABLED] ?: false,
            defaultSourceLang = preferences[PreferencesKeys.DEFAULT_SOURCE_LANG] ?: "auto",
            defaultTargetLang = preferences[PreferencesKeys.DEFAULT_TARGET_LANG] ?: "es",
            autoDetectSource = preferences[PreferencesKeys.AUTO_DETECT_SOURCE] ?: true,
            autoCopyTranslated = preferences[PreferencesKeys.AUTO_COPY_TRANSLATED] ?: false,
            autoSpeakTranslation = preferences[PreferencesKeys.AUTO_SPEAK_TRANSLATION] ?: false,
            hapticFeedbackEnabled = preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] ?: true,
            translationEngine = preferences[PreferencesKeys.TRANSLATION_ENGINE] ?: "gemini",
            customApiKey = preferences[PreferencesKeys.CUSTOM_API_KEY] ?: ""
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentPalette(palette: AccentPalette) {
        context.dataStore.edit { it[PreferencesKeys.ACCENT_PALETTE] = palette.id }
    }

    suspend fun setGlassTransparency(transparency: Float) {
        context.dataStore.edit { it[PreferencesKeys.GLASS_TRANSPARENCY] = transparency }
    }

    suspend fun setFloatingBubbleSize(sizeDp: Int) {
        context.dataStore.edit { it[PreferencesKeys.FLOATING_BUBBLE_SIZE] = sizeDp.toFloat() }
    }

    suspend fun setFloatingBubbleOpacity(opacity: Float) {
        context.dataStore.edit { it[PreferencesKeys.FLOATING_BUBBLE_OPACITY] = opacity }
    }

    suspend fun setFloatingBubbleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.FLOATING_BUBBLE_ENABLED] = enabled }
    }

    suspend fun setDefaultLanguages(source: String, target: String) {
        context.dataStore.edit {
            it[PreferencesKeys.DEFAULT_SOURCE_LANG] = source
            it[PreferencesKeys.DEFAULT_TARGET_LANG] = target
        }
    }

    suspend fun setAutoDetectSource(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_DETECT_SOURCE] = enabled }
    }

    suspend fun setAutoCopyTranslated(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_COPY_TRANSLATED] = enabled }
    }

    suspend fun setAutoSpeakTranslation(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_SPEAK_TRANSLATION] = enabled }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] = enabled }
    }

    suspend fun setTranslationEngine(engine: String) {
        context.dataStore.edit { it[PreferencesKeys.TRANSLATION_ENGINE] = engine }
    }

    suspend fun setCustomApiKey(key: String) {
        context.dataStore.edit { it[PreferencesKeys.CUSTOM_API_KEY] = key }
    }
}
