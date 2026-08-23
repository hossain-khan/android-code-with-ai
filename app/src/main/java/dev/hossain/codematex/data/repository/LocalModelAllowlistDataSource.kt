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
                ),
                ModelEntry(
                    modelId = "litert-community/gemma-4-E4B-it-litert-lm",
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
                ),
            )
    }
