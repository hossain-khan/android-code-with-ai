package dev.hossain.codematex.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalModelAllowlistDataSourceTest {
    @Test
    fun `loadAllowlist returns bundled models`() {
        val dataSource = LocalModelAllowlistDataSource()

        val allowlist = dataSource.loadAllowlist()

        assertEquals(2, allowlist.size)
        assertEquals("litert-community/gemma-4-E2B-it-litert-lm", allowlist[0].modelId)
        assertEquals("gemma-4-E2B-it.litertlm", allowlist[0].modelFile)
        assertEquals(2_588_147_712L, allowlist[0].sizeInBytes)
        assertEquals(8, allowlist[0].minDeviceMemoryInGb)
        assertTrue(allowlist[0].description.isNotBlank())

        assertEquals("litert-community/gemma-4-E4B-it-litert-lm", allowlist[1].modelId)
        assertEquals(3_659_530_240L, allowlist[1].sizeInBytes)
        assertEquals(12, allowlist[1].minDeviceMemoryInGb)
    }
}
