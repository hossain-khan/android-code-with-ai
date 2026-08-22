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
                    description = "Higher capacity instruction-tuned model.",
                ),
            )
    }
}
