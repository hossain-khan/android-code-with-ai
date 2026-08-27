package dev.hossain.codematex.system

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContextUsageStatsTest {
    @Test
    fun `calculates fraction and percentage correctly`() {
        val stats = ContextUsageStats(usedTokens = 2048, maxTokens = 8192)
        assertThat(stats.fraction).isWithin(0.001f).of(0.25f)
        assertThat(stats.percent).isEqualTo(25)
    }

    @Test
    fun `handles zero max tokens safely without division by zero`() {
        val stats = ContextUsageStats(usedTokens = 100, maxTokens = 0)
        assertThat(stats.fraction).isWithin(0.001f).of(0f)
        assertThat(stats.percent).isEqualTo(0)
    }

    @Test
    fun `clamps fraction to 1 when used tokens exceeds max tokens`() {
        val stats = ContextUsageStats(usedTokens = 10000, maxTokens = 8192)
        assertThat(stats.fraction).isWithin(0.001f).of(1.0f)
        assertThat(stats.percent).isEqualTo(100)
    }

    @Test
    fun `formats token counts with thousands separators`() {
        val stats = ContextUsageStats(usedTokens = 1420, maxTokens = 32768)
        assertThat(stats.formattedUsedTokens).isEqualTo("1,420")
        assertThat(stats.formattedMaxTokens).isEqualTo("32,768")
    }
}
