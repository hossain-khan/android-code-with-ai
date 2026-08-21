package dev.hossain.codematex.data.model

import androidx.compose.runtime.Immutable
import dev.hossain.codematex.runtime.LlmEngine

@Immutable
data class AiModel(
    val id: String,
    val name: String,
    val displayName: String,
    val downloadUrl: String,
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
)

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
}
