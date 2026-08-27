package dev.hossain.codematex.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimeFormatterTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `formatRelativeTime returns empty string for zero or negative timestamps`() {
        assertThat(formatRelativeTime(0L, now)).isEmpty()
        assertThat(formatRelativeTime(-100L, now)).isEmpty()
    }

    @Test
    fun `formatRelativeTime returns Just now for future or very recent timestamps`() {
        assertThat(formatRelativeTime(now + 10.seconds.inWholeMilliseconds, now)).isEqualTo("Just now")
        assertThat(formatRelativeTime(now - 30.seconds.inWholeMilliseconds, now)).isEqualTo("Just now")
    }

    @Test
    fun `formatRelativeTime returns minutes ago for elapsed under one hour`() {
        assertThat(formatRelativeTime(now - 1.minutes.inWholeMilliseconds, now)).isEqualTo("1m ago")
        assertThat(formatRelativeTime(now - 5.minutes.inWholeMilliseconds, now)).isEqualTo("5m ago")
        assertThat(formatRelativeTime(now - 59.minutes.inWholeMilliseconds, now)).isEqualTo("59m ago")
    }

    @Test
    fun `formatRelativeTime returns hours ago for elapsed under one day`() {
        assertThat(formatRelativeTime(now - 1.hours.inWholeMilliseconds, now)).isEqualTo("1h ago")
        assertThat(formatRelativeTime(now - 14.hours.inWholeMilliseconds, now)).isEqualTo("14h ago")
        assertThat(formatRelativeTime(now - 23.hours.inWholeMilliseconds, now)).isEqualTo("23h ago")
    }

    @Test
    fun `formatRelativeTime returns days ago for elapsed under seven days`() {
        assertThat(formatRelativeTime(now - 1.days.inWholeMilliseconds, now)).isEqualTo("1d ago")
        assertThat(formatRelativeTime(now - 2.days.inWholeMilliseconds, now)).isEqualTo("2d ago")
        assertThat(formatRelativeTime(now - 6.days.inWholeMilliseconds, now)).isEqualTo("6d ago")
    }

    @Test
    fun `formatRelativeTime returns weeks ago for elapsed under thirty days`() {
        assertThat(formatRelativeTime(now - 7.days.inWholeMilliseconds, now)).isEqualTo("1w ago")
        assertThat(formatRelativeTime(now - 21.days.inWholeMilliseconds, now)).isEqualTo("3w ago")
    }

    @Test
    fun `formatRelativeTime returns months and years for longer elapsed durations`() {
        assertThat(formatRelativeTime(now - 30.days.inWholeMilliseconds, now)).isEqualTo("1mo ago")
        assertThat(formatRelativeTime(now - 180.days.inWholeMilliseconds, now)).isEqualTo("6mo ago")
        assertThat(formatRelativeTime(now - 365.days.inWholeMilliseconds, now)).isEqualTo("1y ago")
        assertThat(formatRelativeTime(now - 730.days.inWholeMilliseconds, now)).isEqualTo("2y ago")
    }

    @Test
    fun `formatRelativeTime handles compound durations correctly`() {
        val compoundPast = now - (1.days + 3.hours + 30.minutes).inWholeMilliseconds
        assertThat(formatRelativeTime(compoundPast, now)).isEqualTo("1d ago")
    }
}
