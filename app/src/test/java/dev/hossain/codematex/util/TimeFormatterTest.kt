package dev.hossain.codematex.util

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimeFormatterTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `formatRelativeTime returns empty string for zero or negative timestamps`() {
        assertEquals("", formatRelativeTime(0L, now))
        assertEquals("", formatRelativeTime(-100L, now))
    }

    @Test
    fun `formatRelativeTime returns Just now for future or very recent timestamps`() {
        assertEquals("Just now", formatRelativeTime(now + 10.seconds.inWholeMilliseconds, now))
        assertEquals("Just now", formatRelativeTime(now - 30.seconds.inWholeMilliseconds, now))
    }

    @Test
    fun `formatRelativeTime returns minutes ago for elapsed under one hour`() {
        assertEquals("1m ago", formatRelativeTime(now - 1.minutes.inWholeMilliseconds, now))
        assertEquals("5m ago", formatRelativeTime(now - 5.minutes.inWholeMilliseconds, now))
        assertEquals("59m ago", formatRelativeTime(now - 59.minutes.inWholeMilliseconds, now))
    }

    @Test
    fun `formatRelativeTime returns hours ago for elapsed under one day`() {
        assertEquals("1h ago", formatRelativeTime(now - 1.hours.inWholeMilliseconds, now))
        assertEquals("14h ago", formatRelativeTime(now - 14.hours.inWholeMilliseconds, now))
        assertEquals("23h ago", formatRelativeTime(now - 23.hours.inWholeMilliseconds, now))
    }

    @Test
    fun `formatRelativeTime returns days ago for elapsed under seven days`() {
        assertEquals("1d ago", formatRelativeTime(now - 1.days.inWholeMilliseconds, now))
        assertEquals("2d ago", formatRelativeTime(now - 2.days.inWholeMilliseconds, now))
        assertEquals("6d ago", formatRelativeTime(now - 6.days.inWholeMilliseconds, now))
    }

    @Test
    fun `formatRelativeTime returns weeks ago for elapsed under thirty days`() {
        assertEquals("1w ago", formatRelativeTime(now - 7.days.inWholeMilliseconds, now))
        assertEquals("3w ago", formatRelativeTime(now - 21.days.inWholeMilliseconds, now))
    }

    @Test
    fun `formatRelativeTime returns months and years for longer elapsed durations`() {
        assertEquals("1mo ago", formatRelativeTime(now - 30.days.inWholeMilliseconds, now))
        assertEquals("6mo ago", formatRelativeTime(now - 180.days.inWholeMilliseconds, now))
        assertEquals("1y ago", formatRelativeTime(now - 365.days.inWholeMilliseconds, now))
        assertEquals("2y ago", formatRelativeTime(now - 730.days.inWholeMilliseconds, now))
    }

    @Test
    fun `formatRelativeTime handles compound durations correctly`() {
        val compoundPast = now - (1.days + 3.hours + 30.minutes).inWholeMilliseconds
        assertEquals("1d ago", formatRelativeTime(compoundPast, now))
    }
}
