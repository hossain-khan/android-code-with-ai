package dev.hossain.codematex.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocalModelAllowlistDataSourceTest {
    @Test
    fun `loadAllowlist returns bundled models`() {
        val dataSource = LocalModelAllowlistDataSource()

        val allowlist = dataSource.loadAllowlist()

        assertThat(allowlist).hasSize(5)
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
    }
}
