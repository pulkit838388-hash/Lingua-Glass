# Lingua Glass — Premium AMOLED Black & Liquid Glass AI Translator

**Lingua Glass** is an Android translation application featuring an **AMOLED Black** and **Liquid Glass** aesthetic, with a system-wide floating translator, real-time voice conversation mode, and multi-engine translation support.

---

## ✨ Features

- **AMOLED Black & Liquid Glass UI**: Pure `#000000` AMOLED dark design with frosted glass reflections, rounded corners, and customizable neon accent palettes (Electric Cyan, Royal Violet, Neon Jade, Sunset Amber, Cosmic Rose).
- **Floating Translator (System-Wide Overlay & Sandbox)**:
  - Circular draggable Liquid Glass bubble that floats on top of other apps (WhatsApp, Chrome, Twitter/X, Instagram, Slack).
  - Tap-to-expand compact floating translation window with one-tap clipboard translation, language swap, and voice pronunciation.
  - Interactive in-app sandbox preview for drag-and-drop testing.
  - Text selection menu support (`ACTION_PROCESS_TEXT`) to translate selected text from any Android app.
- **60+ World & Regional Languages**: Full support for English, Spanish, Hindi, Punjabi, Urdu, Bengali, Tamil, Telugu, Marathi, Gujarati, Kannada, Malayalam, Chinese, Japanese, Korean, Arabic, French, German, Russian, and more with native script display and auto-detection.
- **Bilingual Voice & Conversation Mode**: Two-speaker interactive split view with real-time speech recognition, animated audio waveform pulses, and automatic Text-to-Speech (TTS) pronunciation. Includes a 180° Face-to-Face flip mode.
- **On-Device Room Database**: Local SQLite persistence for translation history and favorite bookmarks with instant live search and zero cloud telemetry.
- **AI Engine Architecture**: Powered by Gemini 3.5 Flash for context-aware translations, idioms, and grammar notes, with a reliable multilingual cloud fallback.

---

## 📱 APK File & Installation

The debug APK is compiled and ready for testing:

- **Path:** `app/build/outputs/apk/debug/app-debug.apk`
- **Application ID:** `com.aistudio.linguaglass.wtrxbk`
- **Min Android SDK:** Android 7.0 (API level 24)
- **Target Android SDK:** Android 15 (API level 36)

### Sideloading via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛠️ Tech Stack & Architecture

- **UI Toolkit:** Jetpack Compose with Material 3
- **Language:** Kotlin 2.2
- **Concurrency:** Kotlin Coroutines & Reactive Flow
- **Local Database:** Room Database with KSP (Kotlin Symbol Processing)
- **Preferences:** Android Jetpack DataStore Preferences
- **Networking:** OkHttp & Retrofit
- **Speech & Audio:** Android SpeechRecognizer & TextToSpeech (TTS)
- **Overlay Window:** Android WindowManager & Foreground Service

---

## 🚀 Building from Source

```bash
# Clone the repository
git clone https://github.com/appsteck/lingua-Glass-apk.git

# Build debug APK
gradle assembleDebug
```

---

## 🔒 Privacy & Permissions

- `SYSTEM_ALERT_WINDOW`: Enables the floating Liquid Glass translation bubble over other applications.
- `RECORD_AUDIO`: Voice input for single-mic and dual-speaker conversation mode.
- `FOREGROUND_SERVICE`: Keeps the floating translation service responsive in the background.
- `INTERNET`: Communicates with translation APIs.
- All translation history and bookmark data remains 100% on your local device.
