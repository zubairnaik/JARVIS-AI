# JARVIS Android Application (Kotlin + Jetpack Compose)

A complete native Android AI personal assistant built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Android SpeechRecognizer**, **Android TextToSpeech**, **DataStore**, and **Google Gemini Streaming API**.

## Setup in Android Studio

1. Open the `android/` directory in **Android Studio Ladybug (or newer)**.
2. Copy `local.properties.example` to `local.properties` in the `android/` folder.
3. Add your Android SDK path (`sdk.dir=...`) and your Gemini API key:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
   `app/build.gradle.kts` reads `GEMINI_API_KEY` at compile time into `BuildConfig.GEMINI_API_KEY` so no secrets are hardcoded in Kotlin files or committed to version control.
4. Sync Gradle and run on an Android 8.0+ (API 26+) device or emulator with Google Speech Services enabled.

## Included Capabilities

- **Google Gemini Streaming Chat** (`GeminiRepository.kt`): Multi-turn conversation context with SSE streaming and English / Hindi / Roman Hindi fluency.
- **Native Voice Input** (`VoiceManager.kt`): Android `SpeechRecognizer` with live partial transcription and RMS audio amplitude for the animated circular core.
- **Text-to-Speech** (`VoiceManager.kt`): Automatic language switching (`en-IN` / `hi-IN`) with configurable speed and pitch.
- **Holographic Dashboard & Core** (`JarvisCore.kt`, `DashboardScreen.kt`): Custom Compose Canvas multi-ring rotating core, live clock & date, typing animation, persistent chat history, and clear conversation control.
- **Settings Screen** (`SettingsScreen.kt`): Voice toggles, speech rate & pitch sliders, recognition language selector, runtime microphone permission status, and history reset.
