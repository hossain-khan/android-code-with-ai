package dev.hossain.codematex.data.repository

import kotlinx.serialization.Serializable

@Serializable
data class ModelAllowlist(
    val models: List<ModelEntry>,
)

@Serializable
data class ModelEntry(
    /** Unique repository identifier or namespace (e.g. `"litert-community/Phi-4-mini-instruct"`). */
    val modelId: String,
    /** Physical binary filename stored on disk or repository (e.g. `"Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm"`). */
    val modelFile: String,
    /** Git commit SHA or revision on the source repository (e.g. `"6e5c4f1e395deb959c494953478fa5cec4b8008f"` or `"main"`). */
    val commitHash: String,
    /** Total file size in bytes (e.g. `2_588_147_712L` for ~2.58 GB). */
    val sizeInBytes: Long,
    /** Supported inference task types (e.g. `["llm_chat"]`). */
    val taskTypes: List<String>,
    /** Execution runtime framework name (e.g. `"LITERT_LM"`). */
    val runtimeType: String,
    /** Minimum recommended device RAM capacity in binary gigabytes (e.g. `6` for 6 GB). */
    val minDeviceMemoryInGb: Int = 0,
    /** Organization, author, or community publishing the weights (e.g. `"Google LiteRT Community"`). */
    val publisher: String = "Google LiteRT Community",
    /** Distribution license name (e.g. `"Apache 2.0"`, `"MIT"`). */
    val license: String = "Apache 2.0",
    /** Web URL pointing to the full license text (e.g. `"https://www.apache.org/licenses/LICENSE-2.0"`). */
    val licenseUrl: String = "https://www.apache.org/licenses/LICENSE-2.0",
    /** User-facing description explaining the model's capabilities and target use cases. */
    val description: String = "",
    /** Direct HTTP/HTTPS download link for the model weights bundle. */
    val downloadUrl: String? = null,
    /** Secondary mirror or Hugging Face fallback URLs if the primary CDN endpoint fails. */
    val fallbackDownloadUrls: List<String> = emptyList(),
    /** Cryptographic SHA-256 checksum used to verify file integrity post-download. */
    val sha256: String? = null,
    /** Human-friendly model display name (e.g. `"Phi-4 Mini Instruct (3.8B)"`, `"Qwen 2.5 Coder 1.5B"`). */
    val displayName: String? = null,
    /** Maximum token context length supported by the model (e.g. `128000` for 128k tokens, `32768` for 32k tokens). */
    val contextWindow: Int = 0,
    /** Quantization format and bit-width precision (e.g. `"INT4"`, `"Q8"`). */
    val quantization: String = "",
    /** Dialogue formatting template / control token syntax (e.g. `"GEMMA"`, `"PHI"`, `"CHATML"`). */
    val promptFormat: String = "",
    /** Indicates whether the model requires Hugging Face authentication or license gating. */
    val isGatedModel: Boolean = false,
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
