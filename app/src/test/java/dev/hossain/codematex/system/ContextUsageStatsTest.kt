package dev.hossain.codematex.system

import org.junit.Assert.assertEquals
import org.junit.Test

class ContextUsageStatsTest {
    @Test
    fun `calculates fraction and percentage correctly`() {
        val stats = ContextUsageStats(usedTokens = 2048, maxTokens = 8192)
        assertEquals(0.25f, stats.fraction, 0.001f)
        assertEquals(25, stats.percent)
    }

    @Test
    fun `handles zero max tokens safely without division by zero`() {
        val stats = ContextUsageStats(usedTokens = 100, maxTokens = 0)
        assertEquals(0f, stats.fraction, 0.001f)
        assertEquals(0, stats.percent)
    }

    @Test
    fun `clamps fraction to 1 when used tokens exceeds max tokens`() {
        val stats = ContextUsageStats(usedTokens = 10000, maxTokens = 8192)
        assertEquals(1.0f, stats.fraction, 0.001f)
        assertEquals(100, stats.percent)
    }

    @Test
    fun `formats token counts with thousands separators`() {
        val stats = ContextUsageStats(usedTokens = 1420, maxTokens = 32768)
        assertEquals("1,420", stats.formattedUsedTokens)
        assertEquals("32,768", stats.formattedMaxTokens)
    }
}
