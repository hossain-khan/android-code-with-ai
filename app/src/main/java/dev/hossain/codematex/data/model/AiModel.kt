package dev.hossain.codematex.data.model

import androidx.compose.runtime.Immutable
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.util.formatShortModelName
import dev.hossain.codematex.util.formatStorageSize

@Immutable
data class AiModel(
    /** Unique identifier for the model (e.g. `"litert-community/Phi-4-mini-instruct"`). */
    val id: String,
    /** Technical model identifier or repository short name (e.g. `"Phi-4-mini-instruct"`). */
    val name: String,
    /** Human-friendly formatted name for UI display (e.g. `"Phi-4 Mini Instruct (3.8B)"`). */
    val displayName: String,
    /** Primary download URL pointing to the model weights bundle. */
    val downloadUrl: String,
    /** Fallback mirror URLs if the primary download fails. */
    val fallbackDownloadUrls: List<String> = emptyList(),
    /** Total file size in bytes (e.g. `2_684_354_560L`). */
    val sizeBytes: Long,
    /** Absolute path on device storage once downloaded, or `null` if not on device. */
    val localPath: String?,
    /** Current lifecycle and download state of the model file on device. */
    val downloadStatus: DownloadStatus,
    /** Preferred hardware acceleration backend (e.g. [LlmEngine.Backend.GPU]). */
    val preferredBackend: LlmEngine.Backend,
    /** Minimum recommended device RAM in gigabytes (e.g. `6`). */
    val minDeviceMemoryInGb: Int = 0,
    /** Current download progress percentage (0 to 100). */
    val downloadProgress: Int = 0,
    /** Publisher or author organization (e.g. `"Google LiteRT Community"`). */
    val publisher: String = "Google LiteRT Community",
    /** Web URL pointing to the model repository (e.g. `"https://huggingface.co/litert-community"`). */
    val modelRepoUrl: String = "https://huggingface.co/litert-community",
    /** License name governing usage of the model weights (e.g. `"MIT"`, `"Apache 2.0"`). */
    val license: String = "Apache 2.0",
    /** URL pointing to the full license text. */
    val licenseUrl: String = "https://www.apache.org/licenses/LICENSE-2.0",
    /** Description of the model's strengths, specializations, or target use cases. */
    val description: String = "",
    /** Expected SHA-256 hash for verifying file integrity post-download. */
    val sha256: String? = null,
    /** Error message if the last download attempt failed, or `null` otherwise. */
    val downloadErrorMessage: String? = null,
    /** Whether this model is currently active/selected for chat inference. */
    val isSelected: Boolean = false,
    /** Maximum token context length supported by the model (e.g. `128000`, `32768`). */
    val contextWindow: Int = 0,
    /** Quantization bit-width and compression format (e.g. `"INT4"`, `"Q8"`). */
    val quantization: String = "",
    /** Chat dialogue formatting template / control token syntax (e.g. `"GEMMA"`, `"PHI"`, `"CHATML"`). */
    val promptFormat: String = "",
    /** Whether the model is gated behind Hugging Face agreements/authentication. */
    val isGatedModel: Boolean = false,
)

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
}

/**
 * Returns a concise, user-friendly display name derived from the model name/ID.
 */
val AiModel.shortDisplayName: String
    get() = formatShortModelName(name)

/**
 * Returns a formatted storage size string (e.g. `"2,588 MB"`).
 */
val AiModel.formattedSize: String
    get() = formatStorageSize(sizeBytes)

/**
 * Returns a formatted context window string (e.g. `"128k Context"`, `"32k Context"`), or `null` if unspecified.
 */
val AiModel.formattedContextWindow: String?
    get() {
        if (contextWindow <= 0) return null
        val kCount =
            when {
                // Powers-of-2 multiples of 1024 (e.g. 4096 -> 4k, 8192 -> 8k, 32768 -> 32k, 131072 -> 128k)
                contextWindow % 1024 == 0 && ((contextWindow / 1024) and ((contextWindow / 1024) - 1)) == 0 -> {
                    contextWindow / 1024
                }

                // Multiples of 1000 (e.g. 128000 -> 128k, 32000 -> 32k)
                contextWindow % 1000 == 0 -> {
                    contextWindow / 1000
                }

                // Other binary multiples of 1024
                contextWindow % 1024 == 0 -> {
                    contextWindow / 1024
                }

                else -> {
                    kotlin.math.round(contextWindow / 1000.0).toInt()
                }
            }
        return "${kCount}k Context"
    }
