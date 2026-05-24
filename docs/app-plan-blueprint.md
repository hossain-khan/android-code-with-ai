# On-Device AI Android App — Architecture Blueprint

> **Target App: "Code with AI"** — An on-device AI learning companion where users download
> LLM models and chat about coding languages/concepts. Sessions are saved and summarized
> for revisiting past learning.

---

> **IMPORTANT: This document has two parts.**
>
> - **Part 1 (Sections 1-24):** Reference analysis of Google AI Edge Gallery — documents how
>   on-device LLM inference, model download, streaming, and chat persistence work. This part
>   uses Gallery's original patterns (Hilt DI, ViewModel, Jetpack Navigation) for reference only.
>   **Do NOT use these patterns in the actual "Code with AI" app.**
>
> - **Part 2 (Sections 25-37):** The actual implementation blueprint using **Metro DI** +
>   **Circuit UDF** architecture from the `android-compose-app-template`. **Follow Part 2 for
>   all app architecture, DI, navigation, state management, and testing patterns.**
>
> When building the app, use Part 1 only for understanding the inference engine internals
> (LiteRT-LM API, download worker mechanics, streaming callbacks). All architecture decisions
> (DI, navigation, state, persistence) come from Part 2.

---

# Part 1: Reference — Google AI Edge Gallery Internals

*Use this section to understand how on-device LLM inference works. Do not copy the
architectural patterns (Hilt, ViewModel, Jetpack Navigation) — those are replaced by
Metro DI + Circuit UDF in Part 2.*

---

## 1. Project Structure

Single-module Android app:

```
app/src/main/java/com/example/myaiapp/
├── MyApplication.kt            # @HiltAndroidApp entry
├── MainActivity.kt             # Single Activity
├── AppComposable.kt            # Root composable
├── data/                       # Domain models, repositories, configs
│   ├── Model.kt               # Model domain object
│   ├── ModelAllowlist.kt       # Model registry / discovery
│   ├── Config.kt              # Configurable model parameters
│   ├── DownloadRepository.kt  # WorkManager-based download
│   └── DataStoreRepository.kt # Persistent storage (Proto DataStore)
├── di/                         # Hilt DI modules
├── runtime/                    # Inference engine abstraction
│   ├── LlmModelHelper.kt     # Inference interface
│   ├── ModelHelperExt.kt      # Runtime dispatch
│   ├── LlmChatModelHelper.kt # LiteRT-LM backend
│   └── AICoreModelHelper.kt  # ML Kit AICore backend (optional)
├── ui/                         # All screens (Jetpack Compose)
│   ├── navigation/            # NavGraph + routes
│   ├── home/                  # Home screen
│   ├── modelmanager/          # Model list + download UI
│   ├── llmchat/               # Chat screens + ViewModel
│   └── common/chat/           # Shared chat components
├── customtasks/                # Plugin system for features
│   ├── common/CustomTask.kt   # Plugin interface
│   ├── chat/                  # Multi-turn chat task
│   ├── image/                 # Vision QA task
│   ├── audio/                 # Audio transcription task
│   └── agent/                 # Agent + tool calling task
└── notifications/              # Download notifications
```

---

## 2. Tech Stack & Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // On-device LLM inference
    implementation("com.google.ai.edge:litertlm:x.y.z")

    // Optional: ML Kit AICore (Pixel-only Gemini)
    implementation("com.google.mlkit:genai-prompt:x.y.z")

    // Compose UI
    implementation(platform("androidx.compose:compose-bom:2024.x.x"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:x.y.z")

    // DI
    implementation("com.google.dagger:hilt-android:x.y.z")
    ksp("com.google.dagger:hilt-compiler:x.y.z")

    // Background downloads
    implementation("androidx.work:work-runtime-ktx:x.y.z")

    // Persistence
    implementation("androidx.datastore:datastore:x.y.z")
    implementation("com.google.protobuf:protobuf-javalite:x.y.z")

    // JSON parsing
    implementation("com.squareup.moshi:moshi-kotlin:x.y.z")

    // Agent tools (optional)
    implementation("io.modelcontextprotocol:kotlin-sdk:x.y.z")
    implementation("io.ktor:ktor-client-okhttp:x.y.z")
}
```

**Minimum SDK:** 31 (Android 12)  
**Compile SDK:** 35  
**Language:** Kotlin 2.x with Compose compiler plugin  

---

## 3. Navigation (Single Activity Pattern)

```kotlin
// NavGraph.kt
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("models/{taskId}") { ModelListScreen(...) }
        composable("chat/{taskId}/{modelName}") { ChatScreen(...) }
        composable("benchmark/{modelName}") { BenchmarkScreen(...) }
        composable("model_manager") { ModelManagerScreen(...) }
    }
}
```

Deep links: `myapp://chat/<taskId>/<modelName>`

---

## 4. Model Management

### 4.1 Model Discovery

Fetch a JSON allowlist (from remote URL or bundled asset):

```json
{
  "models": [
    {
      "modelId": "google/gemma-2-2b-it",
      "modelFile": "gemma-2-2b-it.task",
      "commitHash": "abc123",
      "sizeInBytes": 2500000000,
      "taskTypes": ["llm_chat", "llm_prompt_lab"],
      "runtimeType": "LITERT_LM"
    }
  ]
}
```

### 4.2 Model Domain Object

```kotlin
data class Model(
    val name: String,
    val downloadUrl: String,           // HuggingFace resolve URL
    val sizeInBytes: Long,
    val taskTypes: List<String>,
    val runtimeType: RuntimeType,
    val configValues: MutableMap<String, Any> = mutableMapOf(),
    var downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    var instance: Any? = null           // Live runtime handle
) {
    fun getPath(context: Context): String =
        "${context.getExternalFilesDir(null)}/${normalizedName}/${version}/${fileName}"
}

enum class DownloadStatus {
    NOT_DOWNLOADED, PARTIALLY_DOWNLOADED, IN_PROGRESS, UNZIPPING, SUCCEEDED, FAILED
}

enum class RuntimeType { LITERT_LM, AICORE }
```

### 4.3 Download System

```kotlin
interface DownloadRepository {
    fun downloadModel(model: Model): LiveData<WorkInfo>
    fun cancelDownload(model: Model)
    fun deleteModel(model: Model)
}

class DefaultDownloadRepository(
    private val workManager: WorkManager
) : DownloadRepository {
    override fun downloadModel(model: Model): LiveData<WorkInfo> {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf("url" to model.downloadUrl, "path" to model.path))
            .build()
        workManager.enqueueUniqueWork(model.name, ExistingWorkPolicy.KEEP, request)
        return workManager.getWorkInfoByIdLiveData(request.id)
    }
}
```

**Download URL format (HuggingFace):**
```
https://huggingface.co/{repoId}/resolve/{commitHash}/{fileName}?download=true
```

**Storage location:**
```
/storage/emulated/0/Android/data/com.example.myapp/files/{modelName}/{version}/{file}
```

---

## 5. Inference Engine (Core)

### 5.1 Interface

```kotlin
interface LlmModelHelper {
    fun initialize(
        context: Context,
        model: Model,
        supportImage: Boolean = false,
        supportAudio: Boolean = false,
        systemInstruction: String? = null,
        tools: List<ToolSet>? = null,
        onDone: () -> Unit
    )

    fun runInference(
        model: Model,
        input: String,
        resultListener: (partialResult: String, done: Boolean) -> Unit,
        onError: (String) -> Unit,
        images: List<Bitmap>? = null,
        audioClips: List<ByteArray>? = null
    )

    fun resetConversation(
        model: Model,
        systemInstruction: String? = null,
        tools: List<ToolSet>? = null
    )

    fun stopResponse(model: Model)
    fun cleanUp(model: Model, onDone: () -> Unit)
}
```

### 5.2 Runtime Dispatch

```kotlin
val Model.runtimeHelper: LlmModelHelper
    get() = when (runtimeType) {
        RuntimeType.LITERT_LM -> LiteRtLmModelHelper
        RuntimeType.AICORE -> AICoreModelHelper
    }
```

### 5.3 LiteRT-LM Backend (Primary)

```kotlin
object LiteRtLmModelHelper : LlmModelHelper {

    override fun initialize(context, model, supportImage, supportAudio, systemInstruction, tools, onDone) {
        val engineConfig = EngineConfig.builder()
            .setModelPath(model.getPath(context))
            .setBackend(model.getBackend())       // CPU, GPU, or NPU
            .setMaxNumTokens(model.maxTokens)
            .build()

        val engine = Engine(engineConfig)
        engine.initialize()

        val samplerConfig = SamplerConfig.builder()
            .setTopK(model.configValues["topK"] as Int)
            .setTopP(model.configValues["topP"] as Float)
            .setTemperature(model.configValues["temperature"] as Float)
            .build()

        val conversation = engine.createConversation(
            ConversationConfig.builder()
                .setSamplerConfig(samplerConfig)
                .setSystemInstruction(systemInstruction)
                .setTools(tools)
                .build()
        )

        model.instance = LlmModelInstance(engine, conversation)
        onDone()
    }

    override fun runInference(model, input, resultListener, onError, images, audioClips) {
        val instance = model.instance as LlmModelInstance
        val contents = buildList {
            images?.forEach { add(Content.ImageBytes(it.toPngBytes())) }
            audioClips?.forEach { add(Content.AudioBytes(it)) }
            add(Content.Text(input))
        }

        instance.conversation.sendMessageAsync(
            Contents.of(contents),
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    resultListener(message.text, false)
                }
                override fun onDone() {
                    resultListener("", true)
                }
                override fun onError(error: String) {
                    onError(error)
                }
            }
        )
    }

    override fun stopResponse(model) {
        (model.instance as LlmModelInstance).conversation.cancelProcess()
    }
}

data class LlmModelInstance(val engine: Engine, val conversation: Conversation)
```

**Backend selection:**

| Accelerator | Backend |
|---|---|
| CPU | `Backend.CPU()` |
| GPU | `Backend.GPU()` |
| NPU (Qualcomm) | `Backend.NPU(context.applicationInfo.nativeLibraryDir)` |

---

## 6. Chat / Conversation Architecture

### 6.1 State Model

```kotlin
data class ChatUiState(
    val messagesByModel: Map<String, MutableList<ChatMessage>> = emptyMap(),
    val inProgress: Boolean = false,
    val preparing: Boolean = false,
    val isResettingSession: Boolean = false
)

sealed class ChatMessage(val side: Side) {
    enum class Side { USER, AGENT, SYSTEM }
}
class ChatMessageText(side: Side, var content: String) : ChatMessage(side)
class ChatMessageImage(side: Side, val bitmap: Bitmap) : ChatMessage(side)
class ChatMessageThinking(val content: String) : ChatMessage(Side.AGENT)
class ChatMessageLoading : ChatMessage(Side.AGENT)
class ChatMessageError(val error: String) : ChatMessage(Side.SYSTEM)
```

### 6.2 Chat ViewModel (Streaming Token Flow)

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun generateResponse(model: Model, input: String, images: List<Bitmap>? = null) {
        viewModelScope.launch(Dispatchers.Default) {
            // 1. Add user message
            addMessage(model, ChatMessageText(Side.USER, input))
            setInProgress(true)
            setPreparing(true)

            // 2. Add loading placeholder
            addMessage(model, ChatMessageLoading())

            // 3. Wait for model to be ready
            while (model.instance == null) delay(100)

            // 4. Run inference with streaming callback
            var firstToken = true
            model.runtimeHelper.runInference(
                model = model,
                input = input,
                images = images,
                resultListener = { partialResult, done ->
                    if (done) {
                        setInProgress(false)
                        return@runInference
                    }
                    if (firstToken) {
                        removeLastMessage(model)  // remove loading
                        addMessage(model, ChatMessageText(Side.AGENT, ""))
                        setPreparing(false)
                        firstToken = false
                    }
                    // Append token to last message
                    appendToLastMessage(model, partialResult)
                },
                onError = { error ->
                    removeLastMessage(model)
                    addMessage(model, ChatMessageError(error))
                    setInProgress(false)
                }
            )
        }
    }

    fun stopResponse(model: Model) {
        model.runtimeHelper.stopResponse(model)
        setInProgress(false)
    }

    fun resetSession(model: Model, systemInstruction: String?) {
        model.runtimeHelper.resetConversation(model, systemInstruction)
        clearMessages(model)
    }
}
```

### 6.3 Streaming UI

```kotlin
@Composable
fun ChatScreen(model: Model, viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = uiState.messagesByModel[model.name] ?: emptyList()

    Column(Modifier.fillMaxSize()) {
        // Message list
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                when (message) {
                    is ChatMessageText -> ChatBubble(message)
                    is ChatMessageThinking -> ThinkingBlock(message)
                    is ChatMessageLoading -> LoadingIndicator()
                    is ChatMessageError -> ErrorBubble(message)
                }
            }
        }

        // Input panel
        ChatInputPanel(
            onSend = { text -> viewModel.generateResponse(model, text) },
            onStop = { viewModel.stopResponse(model) },
            isGenerating = uiState.inProgress
        )
    }
}
```

---

## 7. Plugin System (Adding Features)

### 7.1 Plugin Interface

```kotlin
interface CustomTask {
    val task: Task
    fun initializeModelFn(context: Context, model: Model, onDone: () -> Unit)
    fun cleanUpModelFn(context: Context, model: Model, onDone: () -> Unit)
    @Composable fun MainScreen(data: Any)
}

data class Task(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector
)
```

### 7.2 Registration (Hilt Multibinding)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ChatTaskModule {
    @Provides @IntoSet
    fun provideTask(): CustomTask = LlmChatTask()
}

@Module
@InstallIn(SingletonComponent::class)
object ImageTaskModule {
    @Provides @IntoSet
    fun provideTask(): CustomTask = LlmAskImageTask()
}
```

### 7.3 Auto-Discovery in ViewModel

```kotlin
@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    private val customTasks: Set<@JvmSuppressWildcards CustomTask>,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    fun getActiveTasks(): Set<CustomTask> = customTasks
}
```

---

## 8. Config System

```kotlin
sealed class Config(val key: String, val label: String)

class NumberSliderConfig(
    key: String, label: String,
    val min: Float, val max: Float, val default: Float
) : Config(key, label)

class SegmentedButtonConfig(
    key: String, label: String,
    val options: List<String>, val default: String
) : Config(key, label)

class SwitchConfig(
    key: String, label: String,
    val default: Boolean
) : Config(key, label)

fun createLlmChatConfigs(): List<Config> = listOf(
    NumberSliderConfig("topK", "Top-K", min = 1f, max = 100f, default = 40f),
    NumberSliderConfig("topP", "Top-P", min = 0f, max = 1f, default = 1.0f),
    NumberSliderConfig("temperature", "Temperature", min = 0f, max = 2f, default = 0.8f),
    NumberSliderConfig("maxTokens", "Max Tokens", min = 128f, max = 8192f, default = 1024f),
    SegmentedButtonConfig("accelerator", "Accelerator", listOf("CPU", "GPU", "NPU"), "CPU"),
    SwitchConfig("enableThinking", "Thinking Mode", default = false)
)
```

---

## 9. Agent / Tool Calling (Optional)

```kotlin
class AgentTools : ToolSet {
    @Tool(description = "Search Wikipedia for information")
    fun searchWikipedia(query: String): String {
        // HTTP call to Wikipedia API
        return result
    }

    @Tool(description = "Run a Model Context Protocol tool")
    fun runMcpTool(serverUrl: String, toolName: String, args: String): String {
        // MCP client SDK call
        return result
    }

    @Tool(description = "Execute an Android intent")
    fun runIntent(action: String, data: String): String {
        // Fire Android intent
        return result
    }
}

// Pass tools during initialization
model.runtimeHelper.initialize(
    context = context,
    model = model,
    tools = listOf(AgentTools()),
    systemInstruction = "You are a helpful assistant with access to tools...",
    onDone = { /* ready */ }
)
```

---

## 10. Threading Model

| Operation | Dispatcher | Why |
|---|---|---|
| Model allowlist fetch | `Dispatchers.IO` | Network I/O |
| Model initialization | `Dispatchers.Default` | CPU-heavy native engine load |
| LLM inference | `Dispatchers.Default` | CPU-bound token generation |
| Model download | WorkManager threads | Long-running background work |
| DataStore read/write | `Dispatchers.IO` | Disk I/O |
| UI state updates | Main (auto via StateFlow) | Compose recomposition |

---

## 11. Minimum Viable Implementation

To build the simplest working on-device LLM chat app:

### Step 1: Add dependency
```kotlin
implementation("com.google.ai.edge:litertlm:x.y.z")
```

### Step 2: Download a model file
Store a `.task` model file (e.g., from HuggingFace) in app storage.

### Step 3: Initialize the engine
```kotlin
val config = EngineConfig.builder()
    .setModelPath(modelPath)
    .setBackend(Backend.CPU())
    .setMaxNumTokens(1024)
    .build()
val engine = Engine(config)
engine.initialize()
val conversation = engine.createConversation(
    ConversationConfig.builder()
        .setSamplerConfig(SamplerConfig.builder().setTemperature(0.8f).build())
        .build()
)
```

### Step 4: Stream responses
```kotlin
conversation.sendMessageAsync(
    Contents.of(listOf(Content.Text(userInput))),
    object : MessageCallback {
        override fun onMessage(msg: Message) { /* append token to UI */ }
        override fun onDone() { /* mark complete */ }
        override fun onError(err: String) { /* show error */ }
    }
)
```

### Step 5: Build UI
A `LazyColumn` for messages + a `TextField` with send button.

---

## 12. Full Feature Roadmap

| Priority | Feature | Complexity |
|---|---|---|
| P0 | Basic text chat | Low |
| P0 | Model download + progress | Medium |
| P1 | Multiple model support | Medium |
| P1 | Configurable parameters (temp, topK) | Low |
| P1 | Session history / persistence | Medium |
| P2 | Image input (vision) | Medium |
| P2 | Audio input | Medium |
| P2 | Thinking mode | Low |
| P2 | GPU/NPU acceleration | Low |
| P3 | Tool calling / agents | High |
| P3 | MCP server integration | High |
| P3 | Benchmark suite | Medium |
| P3 | Plugin system for custom tasks | High |

---

## 13. Key Architectural Decisions

*These are Gallery's decisions for reference. For "Code with AI", see Section 35 comparison table.*

1. **Single Activity + Circuit Navigation** — type-safe Screen objects, no string routes.
2. **Model.instance holds live runtime** — avoids a separate registry; the model IS the session.
3. **Per-model message lists** — enables switching models without losing context.
4. **Plugin via `@CircuitInject`** — zero-touch feature registration; new screens auto-register.
5. **Interface-based runtime dispatch** — swap LiteRT-LM for any other engine without UI changes.
6. **WorkManager for downloads** — survives process death, shows progress notifications.
7. **Circuit UDF + `rememberRetained`** — unidirectional data flow, survives config changes and back-stack.
8. **Room DB** — relational persistence for sessions + messages with Flow-based observation.

---

## 14. Android Manifest & Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**Critical: WorkManager foreground service declaration** (required for Android 14+):

```xml
<service
    android:name="androidx.work.impl.foreground.SystemForegroundService"
    android:foregroundServiceType="dataSync"
    android:exported="false"
    tools:node="merge" />
```

Without `tools:node="merge"` and `foregroundServiceType="dataSync"`, download workers crash on Android 14+.

**Activity config:**
```xml
<activity
    android:name=".MainActivity"
    android:configChanges="uiMode"
    android:screenOrientation="portrait"
    android:exported="true">
    <intent-filter>
        <data android:scheme="com.example.myapp" />
    </intent-filter>
</activity>
```

---

## 15. DI Setup (Gallery uses Hilt — we use Metro instead, see Part 2 Section 26)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Settings> =
        DataStoreFactory.create(
            serializer = SettingsSerializer,
            produceFile = { context.dataStoreFile("settings.pb") }
        )

    @Provides @Singleton
    fun provideUserDataStore(@ApplicationContext context: Context): DataStore<UserData> =
        DataStoreFactory.create(
            serializer = UserDataSerializer,
            produceFile = { context.dataStoreFile("user_data.pb") }
        )
}
```

WorkManager is NOT provided via Hilt — obtain it directly:
```kotlin
val workManager = WorkManager.getInstance(context)
```

---

## 16. Download Worker (Full Implementation)

```kotlin
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val outputPath = inputData.getString(KEY_PATH) ?: return Result.failure()
        val outputTmpFile = File("$outputPath.gallerytmp")

        setForeground(createForegroundInfo("Downloading..."))

        val connection = URL(url).openConnection() as HttpURLConnection

        // Resume partial downloads
        if (outputTmpFile.exists() && outputTmpFile.length() > 0) {
            connection.setRequestProperty("Range", "bytes=${outputTmpFile.length()}-")
            connection.setRequestProperty("Accept-Encoding", "identity")  // CRITICAL for range requests
        }

        connection.connect()
        val totalBytes = connection.contentLengthLong + (outputTmpFile.length().takeIf { it > 0 } ?: 0)

        FileOutputStream(outputTmpFile, true).use { fos ->
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = outputTmpFile.length()

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    fos.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    // Report progress every 200ms (use a time check)
                    setProgress(workDataOf(
                        KEY_PROGRESS to (downloadedBytes * 100 / totalBytes).toInt()
                    ))
                }
            }
        }

        // Rename tmp file to final
        outputTmpFile.renameTo(File(outputPath))
        return Result.success()
    }
}
```

**Key details:**
- Uses `HttpURLConnection` (not OkHttp)
- `Accept-Encoding: identity` is critical — gzip responses don't support byte-range resume
- Temp file extension: `.gallerytmp`
- Progress reported via `setProgress()` with `workDataOf`
- Foreground service type must be `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC`
- Enqueue policy: `ExistingWorkPolicy.REPLACE` (cancels existing download of same model)

---

## 17. Model Initialization Lifecycle

```kotlin
// In ModelManagerViewModel
fun initializeModel(context: Context, task: Task, model: Model) {
    if (model.initializing) return  // Prevent re-entrant init

    model.initializing = true
    viewModelScope.launch(Dispatchers.Default) {
        task.customTask.initializeModelFn(
            context = context,
            model = model,
            onDone = { error ->
                model.initializing = false
                if (error != null) {
                    // Set ERROR status with message
                } else {
                    model.instance != null  // Ready for inference
                }
                if (model.cleanUpAfterInit) {
                    model.cleanUpAfterInit = false
                    cleanupModel(model)
                }
            }
        )
    }
}
```

**Timing:** Models are initialized when the user navigates to a chat screen, NOT at download time.

**Waiting pattern in ViewModel before inference:**
```kotlin
// Busy-wait until engine is ready
while (model.instance == null) delay(100)
```

**Auto-recovery on error:**
```kotlin
private fun handleError(model: Model, error: String) {
    addMessage(model, ChatMessageError(error))
    cleanupModel(model)
    initializeModel(context, task, model)  // Re-init automatically
    addMessage(model, ChatMessageWarning("Session re-initialized"))
}
```

---

## 18. Session Persistence (Proto Schema)

```protobuf
message ChatSessionProto {
    string session_id = 1;
    string title = 2;              // First 30 chars of first user message
    int64 timestamp_ms = 3;
    string original_model = 4;
    string task_id = 5;
    repeated ChatMessageProto messages = 6;
}

message ChatMessageProto {
    string message_type = 1;       // TEXT, THINKING, IMAGE, AUDIO_CLIP, etc.
    string content = 2;
    ChatSideProto side = 3;        // USER, MODEL, SYSTEM
    float latency_ms = 4;
    bool is_markdown = 5;
    string accelerator = 6;
    repeated string image_file_paths = 7;
    repeated AudioMessageProto audio_clips = 8;
}

message AudioMessageProto {
    string file_path = 1;
    int32 sample_rate = 2;
}
```

**Images are saved as PNG to `cacheDir`:**
```kotlin
val file = File(context.cacheDir, "img_${sessionId}_${timestamp}_${index}.png")
bitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
```

---

## 19. Multimodal Input Handling

### Image preprocessing (before sending to model):

```kotlin
// 1. Decode with size limit (max 1024x1024)
fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)

    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)!!
}

// 2. Apply EXIF rotation
val rotatedBitmap = rotateBitmap(bitmap, exifOrientation)

// 3. Convert to PNG bytes for inference
fun Bitmap.toPngByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}
```

### Audio preprocessing:
- Format: WAV → mono PCM at 16kHz sample rate
- Max duration: 30 seconds
- Wrapper: `Content.AudioBytes(pcmByteArray)`

### Limits:
- Max images per session: 10 (1 for AICore models)
- Max audio clips per session: 1

---

## 20. Error Handling & Recovery

| Scenario | Behavior |
|---|---|
| Inference error | Show `ChatMessageError`, cleanup model, auto-reinitialize |
| User stops generation | `CancellationException` treated as normal completion, not error |
| Model file corrupt | `Engine()` throws → caught → `ModelInitializationStatus.ERROR` shown |
| Download fails | `Result.failure()` with error message → file deleted → status `FAILED` |
| Download interrupted | `.gallerytmp` left on disk → detected on next launch → auto-resumes via Range header |
| Network lost mid-download | WorkManager auto-retries with backoff |

---

## 21. Memory Management

**Only one model instance lives at a time.** `initializeModel()` calls `cleanupModel()` first, which calls:
```kotlin
instance.conversation.close()
instance.engine.close()
model.instance = null
```

There is no LRU eviction or max-models-loaded limit. The single-instance design keeps memory usage predictable.

**Race condition guard:** If cleanup is requested while init is in-flight, `model.cleanUpAfterInit = true` defers cleanup to the init's `onDone` callback.

---

## 22. System Prompt Management

```kotlin
// Store custom system prompt per task
class SystemPromptRepository @Inject constructor(
    private val userDataStore: DataStore<UserData>
) {
    fun getCustomSystemPrompt(taskId: String): Flow<String?> =
        userDataStore.data.map { it.secretsMap["system_prompt_$taskId"] }

    suspend fun updateSystemPrompt(taskId: String, prompt: String) {
        userDataStore.updateData { it.toBuilder().putSecrets("system_prompt_$taskId", prompt).build() }
    }
}

// Get effective prompt (custom or default)
fun getEffectiveSystemPrompt(taskId: String, defaultPrompt: String): String {
    return systemPromptRepository.getCustomSystemPrompt(taskId).firstOrNull() ?: defaultPrompt
}
```

Applied at two points:
1. **Model initialization** — passed to `ConversationConfig`
2. **Session reset** — when user edits the system prompt, conversation is reset with new instruction

---

## 23. Streaming Text Rendering (BufferedFadingMarkdownText)

The most interesting UI component — smooth token-by-token text appearance:

```kotlin
@Composable
fun BufferedFadingMarkdownText(text: String, inProgress: Boolean) {
    var text1 by remember { mutableStateOf(text) }     // Base layer
    var text2 by remember { mutableStateOf(text) }     // Overlay layer
    val alpha2 = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        snapshotFlow { text }
            .conflate()  // KEY: drops intermediate states if animation can't keep up
            .collect { newText ->
                text2 = newText
                alpha2.snapTo(0f)
                alpha2.animateTo(1f, tween(120))  // 120ms fade-in
                text1 = newText
                awaitFrame()
                alpha2.snapTo(0f)
            }
    }

    // Base layer (fades out as overlay fades in)
    MarkdownText(text = text1, alpha = 1f - alpha2.value)
    // Overlay layer (fades in with new text)
    MarkdownText(text = text2, alpha = alpha2.value, blendMode = BlendMode.Plus)
}
```

**Why this works:**
- `.conflate()` buffers rapid tokens — if tokens arrive faster than 120ms, intermediate states are skipped
- Crossfade hides Markdown re-layout jumps (partial `**bold` → complete `**bold**`)
- `BlendMode.Plus` ensures colors add correctly during transition

---

## 24. Download Complete Notifications with Deep Links

```kotlin
fun sendDownloadCompleteNotification(context: Context, model: Model, taskId: String) {
    val deepLinkUri = Uri.parse("myapp://model/$taskId/${model.name}")
    val pendingIntent = NavDeepLinkBuilder(context)
        .setGraph(R.navigation.nav_graph)
        .setDestination("chat/{taskId}/{modelName}")
        .setArguments(bundleOf("taskId" to taskId, "modelName" to model.name))
        .createPendingIntent()

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("Download Complete")
        .setContentText("${model.name} is ready to use")
        .setSmallIcon(R.drawable.ic_notification)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(model.name.hashCode(), notification)
}
```

Only sent when app is in **background** (checked via `AppLifecycleProvider.isAppInForeground`).

---
---

# Part 2: "Code with AI" — Implementation with Metro DI + Circuit UDF

Adapted from the `android-compose-app-template` architecture.

---

## 25. Project Structure (Code with AI)

```
code-with-ai/
├── app/
│   ├── build.gradle.kts
│   └── src/main/java/dev/example/codewith/
│       ├── CodeWithAiApp.kt                    # Application class, Metro graph creation
│       ├── MainActivity.kt                     # Circuit host + NavigableCircuitContent
│       ├── circuit/
│       │   ├── HomeScreen.kt                   # Topic picker / session list
│       │   ├── ModelPickerScreen.kt            # Download & select models
│       │   ├── ChatScreen.kt                   # Main chat with streaming
│       │   ├── SessionHistoryScreen.kt         # Past sessions with summaries
│       │   ├── SessionDetailScreen.kt          # Resume a saved session
│       │   └── overlay/
│       │       ├── ModelConfigOverlay.kt       # Temperature, topK settings
│       │       └── TopicPickerOverlay.kt       # Choose coding topic
│       ├── data/
│       │   ├── model/
│       │   │   ├── AiModel.kt                 # Model domain object
│       │   │   ├── ChatMessage.kt             # Message types
│       │   │   ├── ChatSession.kt             # Session with summary
│       │   │   └── CodingTopic.kt             # Language/concept enum
│       │   ├── repository/
│       │   │   ├── ModelRepository.kt          # Interface: discover, download, status
│       │   │   ├── ModelRepositoryImpl.kt      # @ContributesBinding
│       │   │   ├── ChatSessionRepository.kt    # Interface: save, load, summarize
│       │   │   └── ChatSessionRepositoryImpl.kt
│       │   └── local/
│       │       ├── SessionDatabase.kt          # Room DB for sessions
│       │       ├── SessionDao.kt
│       │       └── DatabaseGraph.kt            # @ContributesTo with @Provides
│       ├── runtime/
│       │   ├── LlmEngine.kt                   # Interface: init, infer, stop, cleanup
│       │   ├── LlmEngineImpl.kt               # @ContributesBinding — LiteRT-LM wrapper
│       │   └── RuntimeGraph.kt                 # @ContributesTo
│       ├── worker/
│       │   └── ModelDownloadWorker.kt          # @AssistedInject CoroutineWorker
│       ├── di/
│       │   ├── AppGraph.kt                     # Root @DependencyGraph
│       │   ├── CircuitProviders.kt             # Circuit multibindings
│       │   ├── ComposeAppComponentFactory.kt
│       │   ├── ActivityKey.kt
│       │   ├── WorkerKey.kt
│       │   ├── ApplicationContext.kt
│       │   └── AppWorkerFactory.kt
│       └── ui/theme/
│           ├── Color.kt
│           ├── Type.kt
│           └── Theme.kt
├── gradle/
│   └── libs.versions.toml
└── gradle.properties                           # ksp.useKSP2=true
```

---

## 26. Metro DI Graph

```kotlin
@DependencyGraph(scope = AppScope::class)
@SingleIn(AppScope::class)
interface AppGraph {
    @Multibinds val activityProviders: Map<KClass<out Activity>, () -> Activity>
    val circuit: Circuit
    val workManager: WorkManager
    val workerFactory: AppWorkerFactory

    @Provides
    fun providesWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @DependencyGraph.Factory
    interface Factory {
        fun create(@ApplicationContext @Provides context: Context): AppGraph
    }
}
```

**Contributing modules (auto-aggregated via `@ContributesTo`):**

```kotlin
// RuntimeGraph.kt
@ContributesTo(AppScope::class)
interface RuntimeGraph {
    @Provides @SingleIn(AppScope::class)
    fun provideLlmEngine(@ApplicationContext context: Context): LlmEngine = LlmEngineImpl(context)
}

// DatabaseGraph.kt
@ContributesTo(AppScope::class)
interface DatabaseGraph {
    @Provides @SingleIn(AppScope::class)
    fun provideDatabase(@ApplicationContext context: Context): SessionDatabase =
        Room.databaseBuilder(context, SessionDatabase::class.java, "sessions.db").build()

    @Provides
    fun provideSessionDao(db: SessionDatabase): SessionDao = db.sessionDao()
}
```

---

## 27. Screen Definitions (Circuit)

### HomeScreen (Root)

```kotlin
@Parcelize
data object HomeScreen : Screen {
    @Stable
    sealed interface State : CircuitUiState {
        data object Loading : State
        data class Success(
            val recentSessions: List<ChatSession>,
            val topics: List<CodingTopic>,
            val hasDownloadedModel: Boolean,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Immutable
    sealed interface Event : CircuitUiEvent {
        data class TopicSelected(val topic: CodingTopic) : Event
        data class SessionClicked(val sessionId: String) : Event
        data object ManageModels : Event
        data object ViewAllSessions : Event
    }
}
```

### ChatScreen (Core experience)

```kotlin
@Parcelize
data class ChatScreen(
    val topic: CodingTopic,
    val sessionId: String? = null,  // null = new session, non-null = resume
) : Screen {
    @Stable
    sealed interface State : CircuitUiState {
        data object Loading : State
        data class Active(
            val messages: List<ChatMessage>,
            val isGenerating: Boolean,
            val isPreparing: Boolean,
            val modelName: String,
            val topic: CodingTopic,
            val eventSink: (Event) -> Unit,
        ) : State
        data class Error(
            val message: String,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Immutable
    sealed interface Event : CircuitUiEvent {
        data class SendMessage(val text: String) : Event
        data object StopGeneration : Event
        data object ResetSession : Event
        data object Retry : Event
        data class CopyMessage(val content: String) : Event
    }
}
```

### ModelPickerScreen

```kotlin
@Parcelize
data object ModelPickerScreen : Screen {
    @Stable
    sealed interface State : CircuitUiState {
        data object Loading : State
        data class Success(
            val models: List<AiModel>,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Immutable
    sealed interface Event : CircuitUiEvent {
        data class Download(val model: AiModel) : Event
        data class CancelDownload(val model: AiModel) : Event
        data class Delete(val model: AiModel) : Event
        data class Select(val model: AiModel) : Event
    }
}
```

### SessionHistoryScreen

```kotlin
@Parcelize
data object SessionHistoryScreen : Screen {
    @Stable
    sealed interface State : CircuitUiState {
        data object Loading : State
        data class Success(
            val sessions: List<ChatSession>,  // Each has .summary field
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Immutable
    sealed interface Event : CircuitUiEvent {
        data class OpenSession(val sessionId: String) : Event
        data class DeleteSession(val sessionId: String) : Event
    }
}
```

---

## 28. Chat Presenter (Streaming with Circuit UDF)

```kotlin
@AssistedInject
class ChatPresenter constructor(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: ChatScreen,
    private val llmEngine: LlmEngine,
    private val modelRepository: ModelRepository,
    private val sessionRepository: ChatSessionRepository,
) : Presenter<ChatScreen.State> {

    @Composable
    override fun present(): ChatScreen.State {
        var messages by rememberRetained { mutableStateOf<List<ChatMessage>>(emptyList()) }
        var isGenerating by rememberRetained { mutableStateOf(false) }
        var isPreparing by rememberRetained { mutableStateOf(false) }
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        var pendingInput by rememberRetained { mutableStateOf<String?>(null) }
        var initTrigger by rememberRetained { mutableStateOf(0) }

        val activeModel = modelRepository.getSelectedModel()

        // Initialize engine on first composition or model change
        LaunchedEffect(activeModel, initTrigger) {
            if (activeModel == null) return@LaunchedEffect
            isPreparing = true
            try {
                llmEngine.initialize(
                    modelPath = activeModel.localPath,
                    backend = activeModel.preferredBackend,
                    systemInstruction = buildSystemPrompt(screen.topic),
                )
                // Restore session if resuming
                if (screen.sessionId != null && messages.isEmpty()) {
                    messages = sessionRepository.loadMessages(screen.sessionId)
                    llmEngine.restoreHistory(messages)
                }
            } catch (e: Exception) {
                errorMessage = e.message
            }
            isPreparing = false
        }

        // Handle inference
        LaunchedEffect(pendingInput) {
            val input = pendingInput ?: return@LaunchedEffect
            pendingInput = null
            isGenerating = true

            messages = messages + ChatMessage.User(input)
            messages = messages + ChatMessage.Agent(content = "", isStreaming = true)

            try {
                llmEngine.runInference(input) { partialToken, done ->
                    val lastAgent = messages.last() as ChatMessage.Agent
                    messages = messages.dropLast(1) + lastAgent.copy(
                        content = lastAgent.content + partialToken,
                        isStreaming = !done,
                    )
                    if (done) {
                        isGenerating = false
                        // Auto-save session
                        sessionRepository.saveSession(screen.topic, messages)
                    }
                }
            } catch (e: Exception) {
                isGenerating = false
                messages = messages.dropLast(1) + ChatMessage.Error(e.message ?: "Inference failed")
                // Auto-recovery: reinitialize
                initTrigger++
            }
        }

        val eventSink: (ChatScreen.Event) -> Unit = { event ->
            when (event) {
                is ChatScreen.Event.SendMessage -> pendingInput = event.text
                ChatScreen.Event.StopGeneration -> {
                    llmEngine.stop()
                    isGenerating = false
                }
                ChatScreen.Event.ResetSession -> {
                    messages = emptyList()
                    llmEngine.resetConversation(buildSystemPrompt(screen.topic))
                }
                ChatScreen.Event.Retry -> initTrigger++
                is ChatScreen.Event.CopyMessage -> { /* clipboard via context */ }
            }
        }

        return when {
            errorMessage != null -> ChatScreen.State.Error(errorMessage!!, eventSink)
            activeModel == null -> ChatScreen.State.Loading
            else -> ChatScreen.State.Active(
                messages = messages,
                isGenerating = isGenerating,
                isPreparing = isPreparing,
                modelName = activeModel.name,
                topic = screen.topic,
                eventSink = eventSink,
            )
        }
    }

    private fun buildSystemPrompt(topic: CodingTopic): String =
        """You are a coding tutor specializing in ${topic.displayName}.
           |Explain concepts clearly with examples. Use markdown for code blocks.
           |Keep explanations concise but thorough.""".trimMargin()

    @CircuitInject(ChatScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(navigator: Navigator, screen: ChatScreen): ChatPresenter
    }
}
```

---

## 29. LLM Engine Interface

```kotlin
interface LlmEngine {
    suspend fun initialize(
        modelPath: String,
        backend: Backend = Backend.CPU,
        systemInstruction: String? = null,
    )

    suspend fun runInference(
        input: String,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    )

    fun stop()
    fun resetConversation(systemInstruction: String? = null)
    suspend fun restoreHistory(messages: List<ChatMessage>)
    fun cleanup()

    enum class Backend { CPU, GPU, NPU }
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class LlmEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LlmEngine {
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    override suspend fun initialize(modelPath: String, backend: Backend, systemInstruction: String?) {
        cleanup()
        withContext(Dispatchers.Default) {
            val config = EngineConfig.builder()
                .setModelPath(modelPath)
                .setBackend(backend.toLiteRtBackend())
                .setMaxNumTokens(2048)
                .build()
            engine = Engine(config).also { it.initialize() }
            conversation = engine!!.createConversation(
                ConversationConfig.builder()
                    .setSamplerConfig(SamplerConfig.builder().setTemperature(0.7f).build())
                    .setSystemInstruction(systemInstruction)
                    .build()
            )
        }
    }

    override suspend fun runInference(input: String, onToken: (String, Boolean) -> Unit) {
        withContext(Dispatchers.Default) {
            suspendCancellableCoroutine { cont ->
                conversation!!.sendMessageAsync(
                    Contents.of(listOf(Content.Text(input))),
                    object : MessageCallback {
                        override fun onMessage(message: Message) = onToken(message.toString(), false)
                        override fun onDone() { onToken("", true); cont.resume(Unit) }
                        override fun onError(t: Throwable) { cont.resumeWithException(t) }
                    }
                )
                cont.invokeOnCancellation { conversation?.cancelProcess() }
            }
        }
    }

    override fun stop() { conversation?.cancelProcess() }

    override fun resetConversation(systemInstruction: String?) {
        // LiteRT-LM: close old conversation, create new one with same engine
        conversation?.close()
        conversation = engine?.createConversation(
            ConversationConfig.builder()
                .setSystemInstruction(systemInstruction)
                .build()
        )
    }

    override fun cleanup() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
    }
}
```

---

## 30. Domain Models

```kotlin
@Immutable
data class AiModel(
    val id: String,
    val name: String,
    val displayName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val localPath: String?,          // null if not downloaded
    val downloadStatus: DownloadStatus,
    val preferredBackend: LlmEngine.Backend,
)

enum class DownloadStatus { NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, FAILED }

@Immutable
sealed class ChatMessage {
    data class User(val content: String) : ChatMessage()
    data class Agent(val content: String, val isStreaming: Boolean = false) : ChatMessage()
    data class Error(val message: String) : ChatMessage()
    data class System(val info: String) : ChatMessage()
}

@Immutable
data class ChatSession(
    val id: String,
    val topic: CodingTopic,
    val title: String,           // First user message (truncated)
    val summary: String,         // AI-generated summary
    val messageCount: Int,
    val lastActiveAt: Long,
    val modelUsed: String,
)

enum class CodingTopic(val displayName: String) {
    KOTLIN("Kotlin"),
    PYTHON("Python"),
    JAVASCRIPT("JavaScript"),
    RUST("Rust"),
    GO("Go"),
    SWIFT("Swift"),
    ALGORITHMS("Algorithms & Data Structures"),
    SYSTEM_DESIGN("System Design"),
    ANDROID("Android Development"),
    WEB("Web Development"),
}
```

---

## 31. Session Persistence (Room)

```kotlin
@Database(entities = [SessionEntity::class, MessageEntity::class], version = 1)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val topic: String,
    val title: String,
    val summary: String,
    val messageCount: Int,
    val lastActiveAt: Long,
    val modelUsed: String,
)

@Entity(tableName = "messages", foreignKeys = [
    ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"])
])
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val type: String,           // "user", "agent", "error", "system"
    val content: String,
    val timestamp: Long,
    @ColumnInfo(index = true) val orderIndex: Int,
)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY lastActiveAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY orderIndex")
    suspend fun getMessages(sessionId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionEntity)

    @Insert
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessages(sessionId: String)
}
```

---

## 32. Session Summary Generation

```kotlin
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ChatSessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val llmEngine: LlmEngine,
) : ChatSessionRepository {

    override suspend fun saveSession(topic: CodingTopic, messages: List<ChatMessage>) {
        val sessionId = messages.hashCode().toString()  // or UUID
        val title = messages.filterIsInstance<ChatMessage.User>()
            .firstOrNull()?.content?.take(50) ?: "Untitled"

        // Generate summary using the same model
        val summary = generateSummary(messages)

        sessionDao.upsertSession(SessionEntity(
            id = sessionId,
            topic = topic.name,
            title = title,
            summary = summary,
            messageCount = messages.size,
            lastActiveAt = System.currentTimeMillis(),
            modelUsed = "current",
        ))
        sessionDao.deleteMessages(sessionId)
        sessionDao.insertMessages(messages.mapIndexed { index, msg ->
            MessageEntity(
                sessionId = sessionId,
                type = when (msg) {
                    is ChatMessage.User -> "user"
                    is ChatMessage.Agent -> "agent"
                    is ChatMessage.Error -> "error"
                    is ChatMessage.System -> "system"
                },
                content = when (msg) {
                    is ChatMessage.User -> msg.content
                    is ChatMessage.Agent -> msg.content
                    is ChatMessage.Error -> msg.message
                    is ChatMessage.System -> msg.info
                },
                timestamp = System.currentTimeMillis(),
                orderIndex = index,
            )
        })
    }

    private suspend fun generateSummary(messages: List<ChatMessage>): String {
        val conversationText = messages.joinToString("\n") { msg ->
            when (msg) {
                is ChatMessage.User -> "User: ${msg.content}"
                is ChatMessage.Agent -> "AI: ${msg.content.take(200)}"
                else -> ""
            }
        }.take(1000)

        var summary = ""
        llmEngine.runInference(
            "Summarize this coding learning session in 1-2 sentences: $conversationText"
        ) { token, done ->
            summary += token
        }
        return summary.ifBlank { "Coding session about ${messages.size} messages" }
    }
}
```

---

## 33. MainActivity (Circuit Host)

```kotlin
@ActivityKey(MainActivity::class)
@ContributesIntoMap(AppScope::class, binding = binding<Activity>())
class MainActivity constructor(
    private val circuit: Circuit,
) : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CodeWithAiTheme {
                val navStack = rememberSaveableNavStack(root = HomeScreen)
                val navigator = rememberCircuitNavigator(navStack)

                CircuitCompositionLocals(circuit) {
                    ContentWithOverlays {
                        NavigableCircuitContent(
                            navigator = navigator,
                            navStack = navStack,
                            decoratorFactory = remember(navigator) {
                                GestureNavigationDecorationFactory(
                                    onBackInvoked = navigator::pop
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
```

---

## 34. Testing Pattern (Presenter Tests)

```kotlin
class ChatPresenterTest {
    private val fakeNavigator = FakeNavigator(ChatScreen(topic = CodingTopic.KOTLIN))
    private val fakeLlmEngine = FakeLlmEngine()
    private val fakeModelRepo = FakeModelRepository(selectedModel = testModel())
    private val fakeSessionRepo = FakeSessionRepository()

    @Test
    fun `present - sends message and streams response`() = runTest {
        fakeLlmEngine.responseTokens = listOf("Hello", " world", "!")
        val presenter = ChatPresenter(
            navigator = fakeNavigator,
            screen = ChatScreen(topic = CodingTopic.KOTLIN),
            llmEngine = fakeLlmEngine,
            modelRepository = fakeModelRepo,
            sessionRepository = fakeSessionRepo,
        )
        presenter.test {
            // Skip loading/preparing states
            val active = awaitItem() as ChatScreen.State.Active
            assertEquals(emptyList(), active.messages)

            // Send a message
            active.eventSink(ChatScreen.Event.SendMessage("What is a coroutine?"))
            val generating = awaitItem() as ChatScreen.State.Active
            assertTrue(generating.isGenerating)

            // Await streaming completion
            val done = awaitItem() as ChatScreen.State.Active
            assertFalse(done.isGenerating)
            assertEquals("Hello world!", (done.messages.last() as ChatMessage.Agent).content)
        }
    }
}

class FakeLlmEngine : LlmEngine {
    var responseTokens: List<String> = listOf("test response")

    override suspend fun runInference(input: String, onToken: (String, Boolean) -> Unit) {
        responseTokens.forEachIndexed { index, token ->
            onToken(token, index == responseTokens.lastIndex)
        }
    }
    // ... other methods stub out
}
```

---

## 35. Key Differences from Gallery App Architecture

| Aspect | Gallery App | Code with AI (Template) |
|---|---|---|
| DI | Hilt (kapt) | Metro (KSP) — faster builds |
| Navigation | Jetpack Compose Navigation (string routes) | Circuit (type-safe Screen objects) |
| State management | ViewModel + MutableStateFlow | Circuit Presenter + `rememberRetained` |
| UI pattern | Composables observe StateFlow | `@CircuitInject` composable receives `State` |
| Event delivery | Call ViewModel methods directly | `eventSink(Event)` lambda in state |
| Persistence | Proto DataStore | Room (better for relational session data) |
| Plugin system | `@IntoSet` CustomTask | `@CircuitInject` auto-registration |
| Testing | ViewModel + StateFlow collection | `Presenter.test {}` + FakeNavigator |
| Overlays/Dialogs | Compose dialogs inline | `circuitx-overlays` (BottomSheet, etc.) |
| Activity injection | Standard Hilt `@AndroidEntryPoint` | Metro `AppComponentFactory` constructor injection |

---

## 36. Build Dependencies (libs.versions.toml additions)

```toml
[versions]
litertlm = "0.11.0"
room = "2.7.1"

[libraries]
# On-device inference
litertlm = { group = "com.google.ai.edge.litertlm", name = "litertlm-android", version.ref = "litertlm" }

# Persistence
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Markdown rendering
richtext-commonmark = { group = "com.halilibo.compose-richtext", name = "richtext-commonmark", version = "1.0.0-alpha02" }
richtext-ui-material3 = { group = "com.halilibo.compose-richtext", name = "richtext-ui-material3", version = "1.0.0-alpha02" }
```

---

## 37. Navigation Flow

```
HomeScreen (root)
  ├── TopicSelected → ChatScreen(topic, sessionId=null)
  ├── SessionClicked → ChatScreen(topic, sessionId="abc")
  ├── ManageModels → ModelPickerScreen
  └── ViewAllSessions → SessionHistoryScreen
                              └── OpenSession → ChatScreen(topic, sessionId)

ChatScreen
  └── (back) → auto-saves session → navigator.pop()

ModelPickerScreen
  └── Select → pop back to HomeScreen (model now active)
```
