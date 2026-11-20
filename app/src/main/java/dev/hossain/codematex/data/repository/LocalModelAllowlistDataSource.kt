package dev.hossain.codematex.data.repository

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn

/**
 * Bundled allowlist data source. In the future this can be swapped for a remote-backed
 * implementation that downloads the latest allowlist from a CDN.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class LocalModelAllowlistDataSource : ModelAllowlistDataSource {
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
                minDeviceMemoryInGb = 14,
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
                    "Best lightweight model for code explanations, syntax fixes, and Q&A.",
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
                minDeviceMemoryInGb = 4,
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
            ModelEntry(
                modelId = "litert-community/Qwen3-1.7B",
                displayName = "Qwen 3 1.7B",
                modelFile = "Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm",
                commitHash = "c421cc3775575372ac2ca4aa1bebc97300ae36b6",
                sizeInBytes = 977_184_032L,
                taskTypes = listOf("llm_chat"),
                runtimeType = "LITERT_LM",
                minDeviceMemoryInGb = 4,
                publisher = "Google LiteRT Community",
                license = "Apache 2.0",
                licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
                description =
                    "Compact 1.7B reasoning model under 1 GB with fused GPU ops for faster mobile coding explanations.",
                downloadUrl =
                    "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/c421cc3775575372ac2ca4aa1bebc97300ae36b6/Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm",
                fallbackDownloadUrls = emptyList(),
                sha256 = "2a72330b504a6596d53a2c00923c57173b10f1b83e6a541213ffec08d1fc5de8",
                contextWindow = 4096,
                quantization = "INT4",
                promptFormat = "CHATML",
                isGatedModel = false,
            ),
            ModelEntry(
                modelId = "litert-community/DeepSeek-R1-Distill-Qwen-1.5B",
                displayName = "DeepSeek-R1 Distill Qwen 1.5B",
                modelFile = "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
                commitHash = "1e4af2ff557cbb7b7ff12ff4b0a7a1fac1f126a5",
                sizeInBytes = 1_833_451_520L,
                taskTypes = listOf("llm_chat"),
                runtimeType = "LITERT_LM",
                minDeviceMemoryInGb = 6,
                publisher = "Google LiteRT Community",
                license = "MIT",
                licenseUrl = "https://opensource.org/licenses/MIT",
                description =
                    "Fast step-by-step reasoning distill with high GPU prefill speed for algorithmic hints and debugging.",
                downloadUrl =
                    "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/1e4af2ff557cbb7b7ff12ff4b0a7a1fac1f126a5/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
                fallbackDownloadUrls = emptyList(),
                sha256 = "69b35f01759eed765641ab4af589bbe98131fd2825662a086d9037409b8c1295",
                contextWindow = 4096,
                quantization = "INT8",
                promptFormat = "CHATML",
                isGatedModel = false,
            ),
            ModelEntry(
                modelId = "litert-community/SmolLM3-3B",
                displayName = "SmolLM3 3B",
                modelFile = "SmolLM3-3B_q4_block32_ekv4096.litertlm",
                commitHash = "31b930cf81cb1dbb99154d225989706d6468a26e",
                sizeInBytes = 2_004_664_320L,
                taskTypes = listOf("llm_chat"),
                runtimeType = "LITERT_LM",
                minDeviceMemoryInGb = 6,
                publisher = "Google LiteRT Community",
                license = "Apache 2.0",
                licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
                description =
                    "Efficient 3B reasoning model with low GPU memory footprint and fast time-to-first-token.",
                downloadUrl =
                    "https://huggingface.co/litert-community/SmolLM3-3B/resolve/31b930cf81cb1dbb99154d225989706d6468a26e/SmolLM3-3B_q4_block32_ekv4096.litertlm",
                fallbackDownloadUrls = emptyList(),
                sha256 = "7ff3bf595a29cc8784d0cb6f68325922e70c2e5f3ac2e0050eb6a0bd5290468c",
                contextWindow = 4096,
                quantization = "INT4",
                promptFormat = "CHATML",
                isGatedModel = false,
            ),
            ModelEntry(
                modelId = "litert-community/Qwen3-4B",
                displayName = "Qwen 3 4B",
                modelFile = "qwen3_4b_mixed_int4.litertlm",
                commitHash = "17b6220abb4d3e9782d37c8844b18112222b77f6",
                sizeInBytes = 2_272_935_936L,
                taskTypes = listOf("llm_chat"),
                runtimeType = "LITERT_LM",
                minDeviceMemoryInGb = 8,
                publisher = "Google LiteRT Community",
                license = "Apache 2.0",
                licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
                description =
                    "High-density 4B reasoning model for deeper coding explanations and multi-step problem solving.",
                downloadUrl =
                    "https://huggingface.co/litert-community/Qwen3-4B/resolve/17b6220abb4d3e9782d37c8844b18112222b77f6/qwen3_4b_mixed_int4.litertlm",
                fallbackDownloadUrls = emptyList(),
                sha256 = "4877b236f09c0b519958612afeb3c6fc6575c7ce56080d0d3a5406150e0ff9ce",
                contextWindow = 4096,
                quantization = "INT4",
                promptFormat = "CHATML",
                isGatedModel = false,
            ),
            ModelEntry(
                modelId = "litert-community/Qwen2.5-Coder-3B-Instruct",
                displayName = "Qwen 2.5 Coder 3B",
                modelFile = "Qwen2.5_Coder_3B_It.litertlm",
                commitHash = "5a5117fbb47498164eb1bc1b5d464a338ba1a0c6",
                sizeInBytes = 3_433_083_824L,
                taskTypes = listOf("llm_chat"),
                runtimeType = "LITERT_LM",
                minDeviceMemoryInGb = 10,
                publisher = "Google LiteRT Community",
                license = "Apache 2.0",
                licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
                description =
                    "Dedicated 3B coding specialist with INT8 precision for syntax-strict languages like Kotlin, Rust, and TypeScript.",
                downloadUrl =
                    "https://huggingface.co/litert-community/Qwen2.5-Coder-3B-Instruct/resolve/5a5117fbb47498164eb1bc1b5d464a338ba1a0c6/Qwen2.5_Coder_3B_It.litertlm",
                fallbackDownloadUrls = emptyList(),
                sha256 = "78d23da074383f52f852b945b8090870e6c9dded02a842f535ee3ccb9e2874f3",
                contextWindow = 4096,
                quantization = "INT8",
                promptFormat = "CHATML",
                isGatedModel = false,
            ),
            ModelEntry(
                modelId = "litert-community/DeepSeek-R1-Distill-Qwen-7B",
                displayName = "DeepSeek-R1 Distill Qwen 7B",
                modelFile = "DeepSeek-R1-Distill-Qwen-7B_q4_block32_ekv4096.litertlm",
                commitHash = "5729fc205e726768c1eb9c97308419bcbc85bd43",
                sizeInBytes = 4_533_698_560L,
                taskTypes = listOf("llm_chat"),
                runtimeType = "LITERT_LM",
                minDeviceMemoryInGb = 12,
                publisher = "Google LiteRT Community",
                license = "MIT",
                licenseUrl = "https://opensource.org/licenses/MIT",
                description =
                    "7B reasoning model for complex algorithms, data structures, and system design tutoring on high-RAM devices.",
                downloadUrl =
                    "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-7B/resolve/5729fc205e726768c1eb9c97308419bcbc85bd43/DeepSeek-R1-Distill-Qwen-7B_q4_block32_ekv4096.litertlm",
                fallbackDownloadUrls = emptyList(),
                sha256 = "095880100e5af6261beab2f5d0ac8b79b48417714c862b0295e62f5da7a914ec",
                contextWindow = 4096,
                quantization = "INT4",
                promptFormat = "CHATML",
                isGatedModel = false,
            ),
        )
}
