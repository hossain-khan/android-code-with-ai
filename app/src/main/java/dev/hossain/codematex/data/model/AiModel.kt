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
)

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
}
