package dev.hossain.codematex.data.repository

class FakeModelAllowlistDataSource(
    var allowlist: List<ModelEntry> = defaultAllowlist(),
) : ModelAllowlistDataSource {
    override fun loadAllowlist(): List<ModelEntry> = allowlist

    companion object {
        fun defaultAllowlist(): List<ModelEntry> =
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
                    description = "Lightweight on-device instruction-tuned model.",
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
                    description = "Higher capacity instruction-tuned model.",
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
                    sizeInBytes = 2_684_354_560L,
                    taskTypes = listOf("llm_chat"),
                    runtimeType = "LITERT_LM",
                    minDeviceMemoryInGb = 6,
                    publisher = "Google LiteRT Community",
                    license = "MIT",
                    licenseUrl = "https://opensource.org/licenses/MIT",
                    description = "Advanced multi-step reasoning for devices with high memory capacity.",
                    downloadUrl =
                        "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm",
                    contextWindow = 128000,
                    quantization = "INT4",
                    promptFormat = "PHI",
                    isGatedModel = false,
                ),
                ModelEntry(
                    modelId = "litert-community/Qwen2.5-Coder-1.5B-Instruct",
                    displayName = "Qwen 2.5 Coder 1.5B",
                    modelFile = "Qwen2.5-Coder-1.5B-Instruct_int4.litertlm",
                    commitHash = "main",
                    sizeInBytes = 1_120_000_000L,
                    taskTypes = listOf("llm_chat"),
                    runtimeType = "LITERT_LM",
                    minDeviceMemoryInGb = 3,
                    publisher = "Google LiteRT Community",
                    license = "Apache 2.0",
                    licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
                    description = "Best lightweight model for code explanations, syntax fixes, and debugging.",
                    downloadUrl =
                        "https://huggingface.co/litert-community/Qwen2.5-Coder-1.5B-Instruct/resolve/main/Qwen2.5-Coder-1.5B-Instruct_int4.litertlm",
                    contextWindow = 32768,
                    quantization = "INT4",
                    promptFormat = "CHATML",
                    isGatedModel = false,
                ),
            )
    }
}
