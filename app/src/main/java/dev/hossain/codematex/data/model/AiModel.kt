package dev.hossain.codematex.data.model

import androidx.compose.runtime.Immutable
import dev.hossain.codematex.runtime.LlmEngine

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
    get() =
        dev.hossain.codematex.util
            .formatShortModelName(name)

/**
 * Returns a formatted storage size string (e.g. `"2,588 MB"`).
 */
val AiModel.formattedSize: String
    get() =
        dev.hossain.codematex.util
            .formatStorageSize(sizeBytes)
