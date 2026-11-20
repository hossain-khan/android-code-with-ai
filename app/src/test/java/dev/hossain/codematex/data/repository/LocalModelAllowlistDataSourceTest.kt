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

        assertThat(allowlist[5].modelId).isEqualTo("litert-community/Qwen3-1.7B")
        assertThat(allowlist[5].modelFile).isEqualTo("Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm")
        assertThat(allowlist[5].sizeInBytes).isEqualTo(977_184_032L)
        assertThat(allowlist[5].minDeviceMemoryInGb).isEqualTo(4)
        assertThat(allowlist[5].contextWindow).isEqualTo(4096)
        assertThat(allowlist[5].quantization).isEqualTo("INT4")
        assertThat(allowlist[5].sha256).isEqualTo("2a72330b504a6596d53a2c00923c57173b10f1b83e6a541213ffec08d1fc5de8")

        assertThat(allowlist[6].modelId).isEqualTo("litert-community/DeepSeek-R1-Distill-Qwen-1.5B")
        assertThat(allowlist[6].sizeInBytes).isEqualTo(1_833_451_520L)
        assertThat(allowlist[6].minDeviceMemoryInGb).isEqualTo(6)
        assertThat(allowlist[6].license).isEqualTo("MIT")
        assertThat(allowlist[6].sha256).isEqualTo("69b35f01759eed765641ab4af589bbe98131fd2825662a086d9037409b8c1295")

        assertThat(allowlist[7].modelId).isEqualTo("litert-community/SmolLM3-3B")
        assertThat(allowlist[7].sizeInBytes).isEqualTo(2_004_664_320L)
        assertThat(allowlist[7].minDeviceMemoryInGb).isEqualTo(6)
        assertThat(allowlist[7].sha256).isEqualTo("7ff3bf595a29cc8784d0cb6f68325922e70c2e5f3ac2e0050eb6a0bd5290468c")

        assertThat(allowlist[8].modelId).isEqualTo("litert-community/Qwen3-4B")
        assertThat(allowlist[8].sizeInBytes).isEqualTo(2_272_935_936L)
        assertThat(allowlist[8].minDeviceMemoryInGb).isEqualTo(8)
        assertThat(allowlist[8].sha256).isEqualTo("4877b236f09c0b519958612afeb3c6fc6575c7ce56080d0d3a5406150e0ff9ce")

        assertThat(allowlist[9].modelId).isEqualTo("litert-community/Qwen2.5-Coder-3B-Instruct")
        assertThat(allowlist[9].sizeInBytes).isEqualTo(3_433_083_824L)
        assertThat(allowlist[9].minDeviceMemoryInGb).isEqualTo(10)
        assertThat(allowlist[9].quantization).isEqualTo("INT8")
        assertThat(allowlist[9].sha256).isEqualTo("78d23da074383f52f852b945b8090870e6c9dded02a842f535ee3ccb9e2874f3")

        assertThat(allowlist[10].modelId).isEqualTo("litert-community/DeepSeek-R1-Distill-Qwen-7B")
        assertThat(allowlist[10].sizeInBytes).isEqualTo(4_533_698_560L)
        assertThat(allowlist[10].minDeviceMemoryInGb).isEqualTo(12)
        assertThat(allowlist[10].license).isEqualTo("MIT")
        assertThat(allowlist[10].sha256).isEqualTo("095880100e5af6261beab2f5d0ac8b79b48417714c862b0295e62f5da7a914ec")
    }
}
