package dev.hossain.codematex.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalModelAllowlistDataSourceTest {
    @Test
    fun `loadAllowlist returns bundled models`() {
        val dataSource = LocalModelAllowlistDataSource()

        val allowlist = dataSource.loadAllowlist()

        assertEquals(4, allowlist.size)
        assertEquals("litert-community/gemma-4-E2B-it-litert-lm", allowlist[0].modelId)
        assertEquals("gemma-4-E2B-it.litertlm", allowlist[0].modelFile)
        assertEquals(2_588_147_712L, allowlist[0].sizeInBytes)
        assertEquals(8, allowlist[0].minDeviceMemoryInGb)
        assertEquals(8192, allowlist[0].contextWindow)
        assertEquals("INT4", allowlist[0].quantization)
        assertEquals("GEMMA", allowlist[0].promptFormat)
        assertTrue(allowlist[0].description.isNotBlank())

        assertEquals("litert-community/gemma-4-E4B-it-litert-lm", allowlist[1].modelId)
        assertEquals(3_659_530_240L, allowlist[1].sizeInBytes)
        assertEquals(12, allowlist[1].minDeviceMemoryInGb)
        assertEquals(8192, allowlist[1].contextWindow)

        assertEquals("litert-community/Phi-4-mini-instruct", allowlist[2].modelId)
        assertEquals("Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm", allowlist[2].modelFile)
        assertEquals(3_910_090_752L, allowlist[2].sizeInBytes)
        assertEquals(12, allowlist[2].minDeviceMemoryInGb)
        assertEquals(128000, allowlist[2].contextWindow)
        assertEquals("Q8", allowlist[2].quantization)
        assertEquals("PHI", allowlist[2].promptFormat)
        assertEquals("MIT", allowlist[2].license)

        assertEquals("litert-community/Qwen2.5-Coder-1.5B-Instruct", allowlist[3].modelId)
        assertEquals("Qwen2.5-Coder-1.5B-Instruct_int4.litertlm", allowlist[3].modelFile)
        assertEquals(1_120_000_000L, allowlist[3].sizeInBytes)
        assertEquals(3, allowlist[3].minDeviceMemoryInGb)
        assertEquals(32768, allowlist[3].contextWindow)
        assertEquals("INT4", allowlist[3].quantization)
        assertEquals("CHATML", allowlist[3].promptFormat)
        assertEquals("Apache 2.0", allowlist[3].license)
    }
}
