# Code with AI

An on-device AI coding tutor for Android. Chat with local LLMs about programming languages, algorithms, and system design — no internet required after model download.

## Features

- **On-Device Inference** — Powered by Google's LiteRT-LM with CPU/GPU/NPU acceleration
- **Gemma 4 Models** — E2B (2.4GB, 8GB RAM) and E4B (3.4GB, 12GB RAM) from HuggingFace
- **Coding Topics** — Kotlin, Python, JavaScript, Rust, Go, Swift, Android, Web, Algorithms, System Design
- **Session Persistence** — Auto-saved chats with AI-generated summaries, full message history review
- **Markdown Rendering** — AI responses render with proper code block formatting
- **Download Management** — Foreground service with progress tracking, cancel/resume support
- **RAM Compatibility** — Incompatible models automatically disabled with clear messaging
- **Dev Mode** — Stub responses for testing without downloading models

## Tech Stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose + Material 3 |
| Architecture | Circuit UDF (Unidirectional Data Flow) |
| DI | Metro (KSP-based, faster than Hilt) |
| Navigation | Circuit type-safe Screen objects |
| Persistence | Room Database (sessions + messages) |
| Background | WorkManager (model downloads) |
| Inference | LiteRT-LM 0.11.0 |
| Logging | Timber |

## Getting Started

### Prerequisites
- Android Studio Meerkat or later
- JDK 17+
- Android SDK 37 (target), min SDK 28

### Build & Run

```bash
# Clone the repo
git clone https://github.com/hossain-khan/android-code-with-ai.git
cd android-code-with-ai

# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

### Dev Mode

By default, `DEV_MODE=true` in `local.properties` returns stub responses without a model file. Set to `false` for real inference:

```properties
# local.properties
DEV_MODE=false
```

### Model Download

1. Open the app → tap **Manage Models** (⚙️ icon)
2. Tap **Download** on Gemma 4 E2B (recommended for 8GB RAM devices)
3. Wait for download (~2.4GB) — progress shown in notification and UI
4. Tap **Select** to activate the model
5. Pick a coding topic and start chatting

## Architecture

```
MainActivity (Circuit host)
├── HomeScreen → Topic picker + recent sessions
├── ChatScreen → Streaming chat with LLM
├── ModelPickerScreen → Download/select models
├── SessionHistoryScreen → Past sessions list
└── SessionDetailScreen → Session messages + resume

DI: Metro @ContributesTo aggregation (no manual module wiring)
State: Circuit Presenter + rememberRetained (survives config changes)
```

## Project Structure

```
app/src/main/java/dev/hossain/codematex/
├── circuit/                    # Screens (Presenter + UI)
│   ├── overlay/                # Bottom sheet overlays
│   └── ...
├── data/
│   ├── model/                  # Domain models (AiModel, ChatMessage, etc.)
│   ├── repository/             # Repository implementations
│   └── local/                  # Room database
├── runtime/                    # LiteRT-LM engine wrapper
├── worker/                     # ModelDownloadWorker
├── di/                         # Metro DI graph
└── ui/theme/                   # Material 3 theme
```

## CI/CD

GitHub Actions workflows:
- **CI builds** on PRs and `main` branch
- **Kotlin lint** (kotlinter) + **Android lint** checks
- **Unit tests** (Presenter tests with Circuit test utilities)
- **Release builds** (debug-signed, configure production keystore for releases)

## License

MIT License — see [LICENSE](LICENSE) for details.
