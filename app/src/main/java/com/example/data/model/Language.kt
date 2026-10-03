package com.example.data.model

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String,
    val isRtl: Boolean = false,
    val ttsLocaleTag: String = code
)

object Languages {
    val AUTO = Language(
        code = "auto",
        name = "Auto Detect",
        nativeName = "Detect Language",
        flag = "🌐"
    )

    val ALL: List<Language> = listOf(
        AUTO,
        Language("en", "English", "English", "🇺🇸", ttsLocaleTag = "en-US"),
        Language("es", "Spanish", "Español", "🇪🇸", ttsLocaleTag = "es-ES"),
        Language("hi", "Hindi", "हिन्दी", "🇮🇳", ttsLocaleTag = "hi-IN"),
        Language("fr", "French", "Français", "🇫🇷", ttsLocaleTag = "fr-FR"),
        Language("de", "German", "Deutsch", "🇩🇪", ttsLocaleTag = "de-DE"),
        Language("zh", "Chinese (Simplified)", "简体中文", "🇨🇳", ttsLocaleTag = "zh-CN"),
        Language("ja", "Japanese", "日本語", "🇯🇵", ttsLocaleTag = "ja-JP"),
        Language("ko", "Korean", "한국어", "🇰🇷", ttsLocaleTag = "ko-KR"),
        Language("ar", "Arabic", "العربية", "🇸🇦", isRtl = true, ttsLocaleTag = "ar-SA"),
        Language("pt", "Portuguese", "Português", "🇧🇷", ttsLocaleTag = "pt-BR"),
        Language("ru", "Russian", "Русский", "🇷🇺", ttsLocaleTag = "ru-RU"),
        Language("it", "Italian", "Italiano", "🇮🇹", ttsLocaleTag = "it-IT"),
        Language("pa", "Punjabi", "ਪੰਜਾਬੀ", "🇮🇳", ttsLocaleTag = "pa-IN"),
        Language("ur", "Urdu", "اردو", "🇵🇰", isRtl = true, ttsLocaleTag = "ur-PK"),
        Language("bn", "Bengali", "বাংলা", "🇧🇩", ttsLocaleTag = "bn-BD"),
        Language("ta", "Tamil", "தமிழ்", "🇮🇳", ttsLocaleTag = "ta-IN"),
        Language("te", "Telugu", "తెలుగు", "🇮🇳", ttsLocaleTag = "te-IN"),
        Language("mr", "Marathi", "मराठी", "🇮🇳", ttsLocaleTag = "mr-IN"),
        Language("gu", "Gujarati", "ગુજરાતી", "🇮🇳", ttsLocaleTag = "gu-IN"),
        Language("ml", "Malayalam", "മലയാളം", "🇮🇳", ttsLocaleTag = "ml-IN"),
        Language("kn", "Kannada", "ಕನ್ನಡ", "🇮🇳", ttsLocaleTag = "kn-IN"),
        Language("tr", "Turkish", "Türkçe", "🇹🇷", ttsLocaleTag = "tr-TR"),
        Language("nl", "Dutch", "Nederlands", "🇳🇱", ttsLocaleTag = "nl-NL"),
        Language("pl", "Polish", "Polski", "🇵🇱", ttsLocaleTag = "pl-PL"),
        Language("vi", "Vietnamese", "Tiếng Việt", "🇻🇳", ttsLocaleTag = "vi-VN"),
        Language("th", "Thai", "ไทย", "🇹🇭", ttsLocaleTag = "th-TH"),
        Language("id", "Indonesian", "Bahasa Indonesia", "🇮🇩", ttsLocaleTag = "id-ID"),
        Language("fa", "Persian", "فارسی", "🇮🇷", isRtl = true, ttsLocaleTag = "fa-IR"),
        Language("he", "Hebrew", "עברית", "🇮🇱", isRtl = true, ttsLocaleTag = "he-IL"),
        Language("el", "Greek", "Ελληνικά", "🇬🇷", ttsLocaleTag = "el-GR"),
        Language("sv", "Swedish", "Svenska", "🇸🇪", ttsLocaleTag = "sv-SE"),
        Language("uk", "Ukrainian", "Українська", "🇺🇦", ttsLocaleTag = "uk-UA"),
        Language("ro", "Romanian", "Română", "🇷🇴", ttsLocaleTag = "ro-RO"),
        Language("cs", "Czech", "Čeština", "🇨🇿", ttsLocaleTag = "cs-CZ"),
        Language("hu", "Hungarian", "Magyar", "🇭🇺", ttsLocaleTag = "hu-HU"),
        Language("da", "Danish", "Dansk", "🇩🇰", ttsLocaleTag = "da-DK"),
        Language("fi", "Finnish", "Suomi", "🇫🇮", ttsLocaleTag = "fi-FI"),
        Language("no", "Norwegian", "Norsk", "🇳🇴", ttsLocaleTag = "no-NO"),
        Language("ms", "Malay", "Bahasa Melayu", "🇲🇾", ttsLocaleTag = "ms-MY"),
        Language("sw", "Swahili", "Kiswahili", "🇰🇪", ttsLocaleTag = "sw-KE"),
        Language("fil", "Filipino", "Filipino", "🇵🇭", ttsLocaleTag = "fil-PH")
    )

    fun findByCode(code: String): Language {
        return ALL.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: Language(
            code = code,
            name = code.uppercase(),
            nativeName = code,
            flag = "🌐"
        )
    }

    val POPULAR: List<Language> = listOf(
        findByCode("en"),
        findByCode("es"),
        findByCode("hi"),
        findByCode("fr"),
        findByCode("de"),
        findByCode("zh"),
        findByCode("ja"),
        findByCode("ar")
    )
}
