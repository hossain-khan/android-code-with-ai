package dev.hossain.codematex.data.repository

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import javax.inject.Inject

/**
 * Bundled allowlist data source. In the future this can be swapped for a remote-backed
 * implementation that downloads the latest allowlist from a CDN.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class LocalModelAllowlistDataSource
    @Inject
    constructor() : ModelAllowlistDataSource {
        override fun loadAllowlist(): List<ModelEntry> =
            listOf(
                ModelEntry(
                    modelId = "litert-community/gemma-4-E2B-it-litert-lm",
                    displayName = "Gemma 4-E2B IT",
                    modelFile = "gemma-4-E2B-it.litertlm",
                    commitHash = "6e5c4f1e395deb959c494953478fa5cec4b8008f",
                    sizeInBytes = 2_588_147_712,
                    taskTypes = listOf("llm_chat"),
                    runtimeType = "LITERT_LM",
                    minDeviceMemoryInGb = 8,
                    publisher = "Google LiteRT Community",
                    license = "Apache 2.0",
                    licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
                    description =
                        "Lightweight on-device instruction-tuned model optimized for fast mobile code assistance and reasoning.",
                    downloadUrl =
                        "https://light-llm-storage.gohk.xyz/models/litert-community/gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm",
                    fallbackDownloadUrls =
                        listOf(
                            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6e5c4f1e395deb959c494953478fa5cec4b8008f/gemma-4-E2B-it.litertlm?download=true",
                        ),
                    sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
                    contextWindow = 8192,
                    quantization = "INT4",
                    promptFormat = "GEMMA",
                    isGatedModel = false,
                ),
                ModelEntry(
                    modelId = "litert-community/gemma-4-E4B-it-litert-lm",
                    displayName = "Gemma 4-E4B IT",
                    modelFile = "gemma-4-E4B-it.litertlm",
                    commitHash = "28299f30ee4d43294517a4ac93abd6163412f07f",
                    sizeInBytes = 3_659_530_240,
                    taskTypes = listOf("llm_chat"),
                    runtimeType = "LITERT_LM",
                    minDeviceMemoryInGb = 12,
                    publisher = "Google LiteRT Community",
                    license = "Apache 2.0",
                    licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
                    description =
                        "Higher capacity instruction-tuned model offering deeper coding comprehension and complex multi-turn logic.",
                    downloadUrl =
                        "https://light-llm-storage.gohk.xyz/models/litert-community/gemma-4-E4B-it-litert-lm/gemma-4-E4B-it.litertlm",
                    fallbackDownloadUrls =
                        listOf(
                            "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/28299f30ee4d43294517a4ac93abd6163412f07f/gemma-4-E4B-it.litertlm?download=true",
                        ),
                    sha256 = "0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0",
                    contextWindow = 8192,
                    quantization = "INT4",
                    promptFormat = "GEMMA",
                    isGatedModel = false,
                ),
                ModelEntry(
                    modelId = "litert-community/Phi-4-mini-instruct",
                    displayName = "Phi-4 Mini Instruct (3.8B)",
                    modelFile = "Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm",
                    commitHash = "main",
                    sizeInBytes = 3_910_090_752L,
                    taskTypes = listOf("llm_chat"),
                    runtimeType = "LITERT_LM",
                    minDeviceMemoryInGb = 12,
                    publisher = "Google LiteRT Community",
                    license = "MIT",
                    licenseUrl = "https://opensource.org/licenses/MIT",
                    description =
                        "Advanced multi-step reasoning for devices with high memory capacity.",
                    downloadUrl =
                        "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm",
                    fallbackDownloadUrls =
                        listOf(
                            "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm?download=true",
                        ),
                    contextWindow = 128000,
                    quantization = "Q8",
                    promptFormat = "PHI",
                    isGatedModel = false,
                ),
                ModelEntry(
                    modelId = "litert-community/Qwen2.5-Coder-1.5B-Instruct",
                    displayName = "Qwen 2.5 Coder 1.5B",
                    modelFile = "Qwen2.5-Coder-1.5B-Instruct_int4.litertlm",
                    commitHash = "main",
                    sizeInBytes = 1_117_385_648L,
                    taskTypes = listOf("llm_chat"),
                    runtimeType = "LITERT_LM",
                    minDeviceMemoryInGb = 3,
                    publisher = "Google LiteRT Community",
                    license = "Apache 2.0",
                    licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
                    description =
                        "Best lightweight model for code explanations, syntax fixes, and debugging.",
                    downloadUrl =
                        "https://light-llm-storage.gohk.xyz/models/litert-community/Qwen2.5-Coder-1.5B-Instruct/Qwen2.5-Coder-1.5B-Instruct_int4.litertlm",
                    fallbackDownloadUrls =
                        listOf(
                            "https://huggingface.co/litert-community/Qwen2.5-Coder-1.5B-Instruct/resolve/main/Qwen2.5-Coder-1.5B-Instruct_int4.litertlm?download=true",
                        ),
                    sha256 = "273ecc7771ba2dd5fe1bb6d4d4726ad0353102f04ad094082ccf59bca9f21213",
                    contextWindow = 32768,
                    quantization = "INT4",
                    promptFormat = "CHATML",
                    isGatedModel = false,
                ),
                ModelEntry(
                    modelId = "litert-community/Qwen3-0.6B",
                    displayName = "Qwen 3 0.6B",
                    modelFile = "Qwen3-0.6B.litertlm",
                    commitHash = "main",
                    sizeInBytes = 614_236_160L,
                    taskTypes = listOf("llm_chat"),
                    runtimeType = "LITERT_LM",
                    minDeviceMemoryInGb = 2,
                    publisher = "Google LiteRT Community",
                    license = "Apache 2.0",
                    licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
                    description =
                        "Ultra-compact efficient model designed for high-speed local inference on resource-constrained devices.",
                    downloadUrl =
                        "https://light-llm-storage.gohk.xyz/models/litert-community/Qwen3-0.6B/Qwen3-0.6B.litertlm",
                    fallbackDownloadUrls =
                        listOf(
                            "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm?download=true",
                        ),
                    sha256 = "555579ff2f4fd13379abe69c1c3ab5200f7338bc92471557f1d6614a6e5ab0b4",
                    contextWindow = 4096,
                    quantization = "INT8",
                    promptFormat = "CHATML",
                    isGatedModel = false,
                ),
            )
    }
