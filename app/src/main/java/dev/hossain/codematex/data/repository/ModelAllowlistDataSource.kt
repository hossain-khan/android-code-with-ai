package dev.hossain.codematex.data.repository

import kotlinx.serialization.Serializable

@Serializable
data class ModelAllowlist(
    val models: List<ModelEntry>,
)

@Serializable
data class ModelEntry(
    val modelId: String,
    val modelFile: String,
    val commitHash: String,
    val sizeInBytes: Long,
    val taskTypes: List<String>,
    val runtimeType: String,
    val minDeviceMemoryInGb: Int = 0,
    val publisher: String = "Google LiteRT Community",
    val license: String = "Apache 2.0",
    val licenseUrl: String = "https://www.apache.org/licenses/LICENSE-2.0",
    val description: String = "",
    val downloadUrl: String? = null,
    val fallbackDownloadUrls: List<String> = emptyList(),
    val sha256: String? = null,
)

/**
 * Provides the current model allowlist.
 */
fun interface ModelAllowlistDataSource {
    /**
     * Returns the list of allowed models available for download.
     */
    fun loadAllowlist(): List<ModelEntry>
}
