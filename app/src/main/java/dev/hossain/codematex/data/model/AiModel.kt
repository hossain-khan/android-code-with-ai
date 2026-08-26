package dev.hossain.codematex.data.model

import androidx.compose.runtime.Immutable
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.util.formatShortModelName
import dev.hossain.codematex.util.formatStorageSize

@Immutable
data class AiModel(
    val id: String,
    val name: String,
    val displayName: String,
    val downloadUrl: String,
    val fallbackDownloadUrls: List<String> = emptyList(),
    val sizeBytes: Long,
    val localPath: String?,
    val downloadStatus: DownloadStatus,
    val preferredBackend: LlmEngine.Backend,
    val minDeviceMemoryInGb: Int = 0,
    val downloadProgress: Int = 0,
    val publisher: String = "Google LiteRT Community",
    val modelRepoUrl: String = "https://huggingface.co/litert-community",
    val license: String = "Apache 2.0",
    val licenseUrl: String = "https://www.apache.org/licenses/LICENSE-2.0",
    val description: String = "",
    val sha256: String? = null,
    val downloadErrorMessage: String? = null,
    val isSelected: Boolean = false,
    val contextWindow: Int = 0,
    val quantization: String = "",
    val promptFormat: String = "",
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
