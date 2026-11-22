package dev.hossain.codematex.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocalModelAllowlistDataSourceTest {
    @Test
    fun `loadAllowlist returns bundled models`() {
        val dataSource = LocalModelAllowlistDataSource()

        val allowlist = dataSource.loadAllowlist()

        assertThat(allowlist).hasSize(7)
        assertThat(allowlist[0].modelId).isEqualTo("litert-community/gemma-4-E2B-it-litert-lm")
        assertThat(allowlist[0].modelFile).isEqualTo("gemma-4-E2B-it.litertlm")
        assertThat(allowlist[0].sizeInBytes).isEqualTo(2_588_147_712L)
        assertThat(allowlist[0].minDeviceMemoryInGb).isEqualTo(8)
        assertThat(allowlist[0].contextWindow).isEqualTo(8192)
        assertThat(allowlist[0].quantization).isEqualTo("INT4")
        assertThat(allowlist[0].promptFormat).isEqualTo("GEMMA")
        assertThat(allowlist[0].description).isNotEmpty()

        assertThat(allowlist[1].modelId).isEqualTo("litert-community/gemma-4-E4B-it-litert-lm")
        assertThat(allowlist[1].sizeInBytes).isEqualTo(3_659_530_240L)
        assertThat(allowlist[1].minDeviceMemoryInGb).isEqualTo(12)
        assertThat(allowlist[1].contextWindow).isEqualTo(8192)

        assertThat(allowlist[2].modelId).isEqualTo("litert-community/Phi-4-mini-instruct")
        assertThat(allowlist[2].modelFile).isEqualTo("Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm")
        assertThat(allowlist[2].sizeInBytes).isEqualTo(3_910_090_752L)
        assertThat(allowlist[2].minDeviceMemoryInGb).isEqualTo(14)
        assertThat(allowlist[2].contextWindow).isEqualTo(128000)
        assertThat(allowlist[2].quantization).isEqualTo("Q8")
        assertThat(allowlist[2].promptFormat).isEqualTo("PHI")
        assertThat(allowlist[2].license).isEqualTo("MIT")

        assertThat(allowlist[3].modelId).isEqualTo("litert-community/Qwen2.5-Coder-1.5B-Instruct")
        assertThat(allowlist[3].modelFile).isEqualTo("Qwen2.5-Coder-1.5B-Instruct_int4.litertlm")
        assertThat(allowlist[3].sizeInBytes).isEqualTo(1_117_385_648L)
        assertThat(allowlist[3].minDeviceMemoryInGb).isEqualTo(3)
        assertThat(allowlist[3].contextWindow).isEqualTo(32768)
        assertThat(allowlist[3].quantization).isEqualTo("INT4")
        assertThat(allowlist[3].promptFormat).isEqualTo("CHATML")
        assertThat(allowlist[3].license).isEqualTo("Apache 2.0")
        assertThat(allowlist[3].sha256).isEqualTo("273ecc7771ba2dd5fe1bb6d4d4726ad0353102f04ad094082ccf59bca9f21213")

        assertThat(allowlist[4].modelId).isEqualTo("litert-community/Qwen3-0.6B")
        assertThat(allowlist[4].modelFile).isEqualTo("Qwen3-0.6B.litertlm")
        assertThat(allowlist[4].sizeInBytes).isEqualTo(614_236_160L)
        assertThat(allowlist[4].minDeviceMemoryInGb).isEqualTo(4)
        assertThat(allowlist[4].contextWindow).isEqualTo(4096)
        assertThat(allowlist[4].quantization).isEqualTo("INT8")
        assertThat(allowlist[4].promptFormat).isEqualTo("CHATML")
        assertThat(allowlist[4].license).isEqualTo("Apache 2.0")
        assertThat(allowlist[4].sha256).isEqualTo("555579ff2f4fd13379abe69c1c3ab5200f7338bc92471557f1d6614a6e5ab0b4")
        assertThat(allowlist[4].isGatedModel).isFalse()

        val deepSeekSmall = allowlist.first { it.modelId == "litert-community/DeepSeek-R1-Distill-Qwen-1.5B" }
        assertThat(deepSeekSmall.commitHash).isEqualTo("1e4af2ff557cbb7b7ff12ff4b0a7a1fac1f126a5")
        assertThat(deepSeekSmall.sizeInBytes).isEqualTo(1_833_451_520L)
        assertThat(deepSeekSmall.minDeviceMemoryInGb).isEqualTo(6)
        assertThat(deepSeekSmall.contextWindow).isEqualTo(4096)
        assertThat(deepSeekSmall.quantization).isEqualTo("INT8")
        assertThat(deepSeekSmall.promptFormat).isEqualTo("CHATML")
        assertThat(deepSeekSmall.runtimeType).isEqualTo("LITERT_LM")
        assertThat(deepSeekSmall.license).isEqualTo("MIT")
        assertThat(deepSeekSmall.downloadUrl).contains(deepSeekSmall.commitHash)
        assertThat(deepSeekSmall.fallbackDownloadUrls).isEmpty()
        assertThat(deepSeekSmall.sha256).isEqualTo("69b35f01759eed765641ab4af589bbe98131fd2825662a086d9037409b8c1295")
        assertThat(deepSeekSmall.isGatedModel).isFalse()

        val smolLm3 = allowlist.first { it.modelId == "litert-community/SmolLM3-3B" }
        assertThat(smolLm3.commitHash).isEqualTo("31b930cf81cb1dbb99154d225989706d6468a26e")
        assertThat(smolLm3.sizeInBytes).isEqualTo(2_004_664_320L)
        assertThat(smolLm3.minDeviceMemoryInGb).isEqualTo(6)
        assertThat(smolLm3.contextWindow).isEqualTo(4096)
        assertThat(smolLm3.quantization).isEqualTo("INT4")
        assertThat(smolLm3.promptFormat).isEqualTo("CHATML")
        assertThat(smolLm3.runtimeType).isEqualTo("LITERT_LM")
        assertThat(smolLm3.downloadUrl).contains(smolLm3.commitHash)
        assertThat(smolLm3.fallbackDownloadUrls).isEmpty()
        assertThat(smolLm3.sha256).isEqualTo("7ff3bf595a29cc8784d0cb6f68325922e70c2e5f3ac2e0050eb6a0bd5290468c")
        assertThat(smolLm3.isGatedModel).isFalse()
    }

    @Test
    fun `allowlist entries satisfy structural invariants`() {
        val allowlist = LocalModelAllowlistDataSource().loadAllowlist()

        assertThat(allowlist.map { it.modelId }).containsNoDuplicates()
        allowlist.forEach { entry ->
            assertThat(entry.modelId).isNotEmpty()
            assertThat(entry.modelFile).endsWith(".litertlm")
            assertThat(entry.displayName).isNotEmpty()
            assertThat(entry.sizeInBytes).isGreaterThan(0L)
            assertThat(entry.minDeviceMemoryInGb).isGreaterThan(0)
            assertThat(entry.contextWindow).isGreaterThan(0)
            assertThat(entry.runtimeType).isEqualTo("LITERT_LM")
            assertThat(entry.downloadUrl).startsWith("https://")
            if (entry.commitHash != "main") {
                assertThat(entry.sha256).isNotEmpty()
            }
        }
    }
}
