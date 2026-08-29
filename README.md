[![Android CI](https://github.com/hossain-khan/android-code-with-ai/actions/workflows/android.yml/badge.svg)](https://github.com/hossain-khan/android-code-with-ai/actions/workflows/android.yml) [![Android Release Build](https://github.com/hossain-khan/android-code-with-ai/actions/workflows/android-release.yml/badge.svg)](https://github.com/hossain-khan/android-code-with-ai/actions/workflows/android-release.yml) [![codecov](https://codecov.io/gh/hossain-khan/android-code-with-ai/graph/badge.svg?token=F4QSYSTLTX)](https://codecov.io/gh/hossain-khan/android-code-with-ai) [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

# CodeMateX - Code with AI

An **on-device AI coding tutor** for Android. Chat privately with locally-executed Large Language Models (LLMs) about programming languages, algorithms, architecture, and system design, or learn via guided interactive courses — **100% offline, private, and with zero subscription fees**.

---

## ✨ Features

- 🧠 **On-Device Inference** - Powered by Google's **LiteRT-LM** runtime with hardware acceleration across GPU, NPU, and CPU via XNNPack delegates.
- 🤖 **Gemma 4 Models** - Support for **Gemma 4 E2B** (2.6 GB, 8 GB RAM) and **Gemma 4 E4B** (3.7 GB, 12 GB RAM) instruction-tuned weights.
- 🎓 **Guided Learning Courses** - 5 bundled, comprehensive programming courses (**Kotlin**, **Python**, **TypeScript**, **Go**, **Rust**) featuring chapters, bite-sized lessons, syntax-highlighted code, interactive quizzes, and persistent progress tracking.
- 🧭 **Topic & Course Discovery** - Topic cards with course availability badges on the Home screen and empty chat state course callout cards to eliminate prompt paralysis.
- 🎭 **Tutor Personas** - Switchable tutoring personalities (**Senior Software Engineer**, **Helpful Mentor**, **Interviewer**, **Code Reviewer**).
- 🛡️ **Hardware Fallback Engine** - Automated recovery loop (NPU ➔ GPU ➔ CPU) preventing crashes on devices with missing OpenCL drivers.
- 🎨 **Material 3 Expressive & Adaptive** - Dynamic atmospheric gradient lighting, topic theme accents, and multi-pane adaptive layouts for phones, foldables, and tablets.
- 💬 **Live Telemetry & Streaming** - Real-time Time-to-First-Token (TTFT) and decode throughput tracking (tokens/sec) directly in the chat interface.
- ⌨️ **Fluid Markdown & Syntax Highlighting** - High-performance code highlighting powered by [compose-highlight](https://github.com/hossain-khan/android-compose-highlight) (Highlight.js) and [multiplatform-markdown-renderer](https://github.com/mikepenz/multiplatform-markdown-renderer).
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
| **Syntax Highlighting** | **compose-highlight (0.34.0)** | Native Compose Highlight.js engine for streaming & static code blocks |
| **Markdown Rendering** | **multiplatform-markdown-renderer (0.44.0)** | Material 3 Markdown parsing with custom component interception |
| **Persistence** | **Room Database** | Type-safe SQLite persistence for sessions, messages, and course progress |
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

CodeMateX uses Slack's **Circuit** framework for predictable MVI state management, backed by **Metro** dependency injection and **Google LiteRT-LM** for on-device inference.

```
app/src/main/java/dev/hossain/codematex/
├── data/
│   ├── local/                      # Room Database, DAOs, and Entities (Sessions, Messages, Lesson Progress)
│   ├── model/                      # Domain models (AiModel, ChatMessage, CodingTopic, LearningCourse)
│   ├── network/                    # OkHttpClient and Retrofit network configuration
│   ├── repository/                 # Model and Session repository implementations
│   │   └── course/                 # Bundled course content (Kotlin, Python, TS, Go, Rust) & LearningRepository
│   ├── ChatInferenceOrchestrator.kt # Inference lifecycle & token streaming orchestration
│   └── TopicPromptProvider.kt      # System prompts & tutor persona definitions
├── domain/
│   └── summary/                    # Session summary generators and LLM prompt formatters
├── runtime/                        # LiteRT-LM engine wrapper, factory, and fallback strategy
├── system/                         # Device memory analysis, hardware eligibility, & compatibility policies
├── ui/
│   ├── component/                  # Reusable UI widgets, MarkdownMessage, telemetry bars, gradient scrims
│   ├── overlay/                    # Bottom sheet dialogs and overlays (ModelConfig, ModelInfo, PersonaPicker)
│   ├── screens/                    # Circuit MVI Screens, Presenters, and UI Composables
│   └── theme/                      # Material 3 Expressive theme, typography, and topic visual info
├── util/                           # Formatting utilities, device memory helpers, and token estimator
├── work/                           # ModelDownloadWorker & HttpModelDownloader (WorkManager)
└── di/                             # Metro DI scopes and contributors
```

---

## 📚 Documentation & Guides

- 🎨 **[Design Guidelines](docs/DESIGN_GUIDELINES.md)** - Material 3 Expressive specifications, adaptive layouts, and topic color systems.
- 📖 **[Adding Courses Guide](docs/ADDING_COURSES.md)** - Step-by-step guide for authoring, structuring, and verifying new language courses.
- 🚢 **[Release Process Guide](RELEASE.md)** - Step-by-step release lifecycle, versioning rules, and CI/CD cryptographic validation.
- 🤖 **[AI Agent Guide](AGENTS.md)** - Project overview, JNI memory safety constraints, and core workflows for autonomous developers.

---

## 📄 License

CodeMateX is open source software licensed under the [MIT License](LICENSE).
