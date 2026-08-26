package dev.hossain.codematex.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeFormatterTest {
    @Test
    fun `formatStorageSize formats zero and negative bytes as 0 MB`() {
        assertEquals("0 MB", formatStorageSize(0L))
        assertEquals("0 MB", formatStorageSize(-500L))
    }

    @Test
    fun `formatStorageSize formats megabytes with thousands separators`() {
        assertEquals("2,588 MB", formatStorageSize(2_588_147_712L))
        assertEquals("3,659 MB", formatStorageSize(3_659_530_240L))
        assertEquals("1,850 MB", formatStorageSize(1_850_000_000L))
        assertEquals("500 MB", formatStorageSize(500_000_000L))
    }

    @Test
    fun `formattedStorageSize extension property matches formatStorageSize function`() {
        val bytes = 2_588_147_712L
        assertEquals(formatStorageSize(bytes), bytes.formattedStorageSize)
    }
}
