package dev.hossain.codematex.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocalModelAllowlistDataSourceTest {
    @Test
    fun `loadAllowlist returns bundled models`() {
        val dataSource = LocalModelAllowlistDataSource()

        val allowlist = dataSource.loadAllowlist()

        assertThat(allowlist).hasSize(11)
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

        val qwen3Small = allowlist.first { it.modelId == "litert-community/Qwen3-1.7B" }
        assertThat(qwen3Small.modelFile).isEqualTo("Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm")
        assertThat(qwen3Small.commitHash).isEqualTo("c421cc3775575372ac2ca4aa1bebc97300ae36b6")
        assertThat(qwen3Small.sizeInBytes).isEqualTo(977_184_032L)
        assertThat(qwen3Small.minDeviceMemoryInGb).isEqualTo(4)
        assertThat(qwen3Small.contextWindow).isEqualTo(4096)
        assertThat(qwen3Small.quantization).isEqualTo("INT4")
        assertThat(qwen3Small.promptFormat).isEqualTo("CHATML")
        assertThat(qwen3Small.runtimeType).isEqualTo("LITERT_LM")
        assertThat(qwen3Small.downloadUrl).contains(qwen3Small.commitHash)
        assertThat(qwen3Small.fallbackDownloadUrls).isEmpty()
        assertThat(qwen3Small.sha256).isEqualTo("2a72330b504a6596d53a2c00923c57173b10f1b83e6a541213ffec08d1fc5de8")
        assertThat(qwen3Small.isGatedModel).isFalse()

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

        val qwen3Medium = allowlist.first { it.modelId == "litert-community/Qwen3-4B" }
        assertThat(qwen3Medium.commitHash).isEqualTo("17b6220abb4d3e9782d37c8844b18112222b77f6")
        assertThat(qwen3Medium.sizeInBytes).isEqualTo(2_272_935_936L)
        assertThat(qwen3Medium.minDeviceMemoryInGb).isEqualTo(8)
        assertThat(qwen3Medium.contextWindow).isEqualTo(4096)
        assertThat(qwen3Medium.quantization).isEqualTo("INT4")
        assertThat(qwen3Medium.promptFormat).isEqualTo("CHATML")
        assertThat(qwen3Medium.runtimeType).isEqualTo("LITERT_LM")
        assertThat(qwen3Medium.downloadUrl).contains(qwen3Medium.commitHash)
        assertThat(qwen3Medium.fallbackDownloadUrls).isEmpty()
        assertThat(qwen3Medium.sha256).isEqualTo("4877b236f09c0b519958612afeb3c6fc6575c7ce56080d0d3a5406150e0ff9ce")
        assertThat(qwen3Medium.isGatedModel).isFalse()

        val qwen25Coder3b = allowlist.first { it.modelId == "litert-community/Qwen2.5-Coder-3B-Instruct" }
        assertThat(qwen25Coder3b.commitHash).isEqualTo("5a5117fbb47498164eb1bc1b5d464a338ba1a0c6")
        assertThat(qwen25Coder3b.sizeInBytes).isEqualTo(3_433_083_824L)
        assertThat(qwen25Coder3b.minDeviceMemoryInGb).isEqualTo(10)
        assertThat(qwen25Coder3b.contextWindow).isEqualTo(4096)
        assertThat(qwen25Coder3b.quantization).isEqualTo("INT8")
        assertThat(qwen25Coder3b.promptFormat).isEqualTo("CHATML")
        assertThat(qwen25Coder3b.runtimeType).isEqualTo("LITERT_LM")
        assertThat(qwen25Coder3b.downloadUrl).contains(qwen25Coder3b.commitHash)
        assertThat(qwen25Coder3b.fallbackDownloadUrls).isEmpty()
        assertThat(qwen25Coder3b.sha256).isEqualTo("78d23da074383f52f852b945b8090870e6c9dded02a842f535ee3ccb9e2874f3")
        assertThat(qwen25Coder3b.isGatedModel).isFalse()

        val deepSeekLarge = allowlist.first { it.modelId == "litert-community/DeepSeek-R1-Distill-Qwen-7B" }
        assertThat(deepSeekLarge.commitHash).isEqualTo("5729fc205e726768c1eb9c97308419bcbc85bd43")
        assertThat(deepSeekLarge.sizeInBytes).isEqualTo(4_533_698_560L)
        assertThat(deepSeekLarge.minDeviceMemoryInGb).isEqualTo(12)
        assertThat(deepSeekLarge.contextWindow).isEqualTo(4096)
        assertThat(deepSeekLarge.quantization).isEqualTo("INT4")
        assertThat(deepSeekLarge.promptFormat).isEqualTo("CHATML")
        assertThat(deepSeekLarge.runtimeType).isEqualTo("LITERT_LM")
        assertThat(deepSeekLarge.license).isEqualTo("MIT")
        assertThat(deepSeekLarge.downloadUrl).contains(deepSeekLarge.commitHash)
        assertThat(deepSeekLarge.fallbackDownloadUrls).isEmpty()
        assertThat(deepSeekLarge.sha256).isEqualTo("095880100e5af6261beab2f5d0ac8b79b48417714c862b0295e62f5da7a914ec")
        assertThat(deepSeekLarge.isGatedModel).isFalse()
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
