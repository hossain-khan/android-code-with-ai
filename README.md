[![Android CI](https://github.com/hossain-khan/android-code-with-ai/actions/workflows/android.yml/badge.svg)](https://github.com/hossain-khan/android-code-with-ai/actions/workflows/android.yml) [![Android Release Build](https://github.com/hossain-khan/android-code-with-ai/actions/workflows/android-release.yml/badge.svg)](https://github.com/hossain-khan/android-code-with-ai/actions/workflows/android-release.yml) [![codecov](https://codecov.io/gh/hossain-khan/android-code-with-ai/graph/badge.svg?token=F4QSYSTLTX)](https://codecov.io/gh/hossain-khan/android-code-with-ai) [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

# CodeMateX - Code with AI

An **on-device AI coding tutor** for Android. Chat privately with locally-executed Large Language Models (LLMs) about programming languages, algorithms, architecture, and system design — **100% offline, private, and with zero subscription fees**.

---

## ✨ Features

- 🧠 **On-Device Inference** - Powered by Google's **LiteRT-LM** runtime with hardware acceleration across GPU, NPU, and CPU via XNNPack delegates.
- 🤖 **Gemma 4 Models** - Support for **Gemma 4 E2B** (2.6 GB, 8 GB RAM) and **Gemma 4 E4B** (3.7 GB, 12 GB RAM) instruction-tuned weights.
- 🛡️ **Hardware Fallback Engine** - Automated recovery loop (NPU ➔ GPU ➔ CPU) preventing crashes on devices with missing OpenCL drivers.
- 🎨 **Material 3 Expressive & Adaptive** - Dynamic atmospheric gradient lighting, topic theme accents, and multi-pane adaptive layouts for phones, foldables, and tablets.
- 💬 **Live Telemetry & Streaming** - Real-time Time-to-First-Token (TTFT) and decode throughput tracking (tokens/sec) directly in the chat interface.
- ⌨️ **Fluid Chat Experience** - Markdown code syntax rendering, in-context notification permissions, and smart keyboard auto-dismissal.
- 💾 **Session History & Auto-Summaries** - Persistent multi-turn conversation storage in Room database with AI-generated session summaries.
- 📥 **Resilient Model Downloader** - Background WorkManager service featuring HTTP Range byte chunking, automatic resume, and cancellation support.
- 🛠️ **Dev Mode** - Instant stub simulation for testing and UI development without downloading multi-gigabyte weights.

---

## 🛠️ Tech Stack

| Layer | Technology | Description |
|---|---|---|
| **UI & Layout** | **Jetpack Compose + Material 3 Expressive** | Modern declarative UI with adaptive multi-pane window sizing |
| **Architecture** | **Slack Circuit** | MVI-based Presenter/UI pattern with unidirectional data flow |
| **Dependency Injection** | **Metro** | KSP-based compile-time dependency injection |
| **Inference Runtime** | **Google LiteRT-LM (0.11.0)** | High-performance edge AI model runtime |
| **Persistence** | **Room Database** | Type-safe SQLite persistence for sessions and chat messages |
| **Background Processing** | **AndroidX WorkManager** | Foreground downloads with progress notifications |
| **Permissions** | **Accompanist Permissions** | Contextual runtime permissions for notification alerts |
| **Code Coverage** | **Kover + Codecov** | Automated test verification and coverage reporting |
| **Logging** | **Timber** | Lightweight, build-type aware structured logging |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- JDK 17+
- Android SDK 37 (Target), min SDK 28

### Build & Run

```bash
# Clone the repository
git clone https://github.com/hossain-khan/android-code-with-ai.git
cd android-code-with-ai

# Format code and run all checks
./gradlew formatKotlin && ./gradlew check

# Build debug APK
./gradlew assembleDebug

# Install on connected device or emulator
./gradlew installDebug
```

### Dev Mode
By default, `DEV_MODE=true` in `local.properties` provides instant mock AI responses without downloading weights. Set to `false` when running actual on-device models:

```properties
# local.properties
DEV_MODE=false
```

---

## 🏛️ Architecture & Project Structure

```
app/src/main/java/dev/hossain/codematex/
├── circuit/                    # Circuit Screens, Presenters, and UI components
│   ├── ChatPresenter.kt        # State management for chat sessions
│   ├── ChatInferenceOrchestrator.kt # Inference lifecycle & token streaming
│   ├── SystemStatsMonitor.kt   # CPU/RAM hardware monitor
│   ├── ThroughputTracker.kt    # TTFT and tokens/sec calculator
│   └── overlay/                # Bottom sheet dialogs and overlays
├── data/
│   ├── local/                  # Room Database, DAOs, and Entities
│   ├── model/                  # Domain models (AiModel, ChatMessage, CodingTopic)
│   ├── network/                # OkHttpClient and Retrofit network configuration
│   └── repository/             # Model and Session repository implementations
├── domain/
│   └── summary/                # Session summary generators and LLM prompt formatters
├── runtime/                    # LiteRT-LM engine wrapper, factory, and fallback strategy
├── worker/                     # ModelDownloadWorker & HttpModelDownloader
├── di/                         # Metro DI scopes and contributors
└── ui/
    ├── component/              # Reusable UI widgets & gradient scrims
    └── theme/                  # M3 Expressive theme, typography, and topic visual info
```

---

## 📚 Documentation & Guides

- 🎨 **[Design Guidelines](docs/DESIGN_GUIDELINES.md)** - Material 3 Expressive specifications, adaptive layouts, and topic color systems.
- 🚢 **[Release Process Guide](RELEASE.md)** - Step-by-step release lifecycle, versioning rules, and CI/CD cryptographic validation.
- 🤖 **[AI Agent Guide](AGENTS.md)** - Project overview, JNI memory safety constraints, and core workflows for autonomous developers.

---

## 🔒 Security & CI/CD Verification

GitHub Actions automates continuous verification on every PR and tag:
- **Continuous Integration ([`android.yml`](.github/workflows/android.yml))**: Automated Kotlin formatting checks (`kotlinter`), Android Lint, JVM unit tests, and Kover coverage thresholds.
- **Production Releases ([`android-release.yml`](.github/workflows/android-release.yml))**: Automated production keystore signing with cryptographic `apksigner` SHA-256 certificate fingerprint verification before attaching binaries to GitHub Releases.

---

## 📄 License

CodeMateX is open source software licensed under the [MIT License](LICENSE).
