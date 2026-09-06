package dev.hossain.codematex.system

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MemorySnapshotTest {
    @Test
    fun `Given older snapshot with less native memory, When diffFrom is called, Then deltaNativeMb is positive`() {
        val prev =
            MemorySnapshot(
                nativeAllocatedBytes = 10L * 1024 * 1024, // 10 MB
                jvmUsedBytes = 0L,
                systemAvailBytes = 0L,
                timestampMs = 1000L,
            )
        val current =
            MemorySnapshot(
                nativeAllocatedBytes = 25L * 1024 * 1024, // 25 MB
                jvmUsedBytes = 0L,
                systemAvailBytes = 0L,
                timestampMs = 2000L,
            )

        val delta = current.diffFrom(prev)

        assertThat(delta.deltaNativeMb).isWithin(0.001f).of(15f)
    }

    @Test
    fun `Given older snapshot with more native memory, When diffFrom is called, Then deltaNativeMb is negative`() {
        val prev =
            MemorySnapshot(
                nativeAllocatedBytes = 50L * 1024 * 1024, // 50 MB
                jvmUsedBytes = 0L,
                systemAvailBytes = 0L,
                timestampMs = 1000L,
            )
        val current =
            MemorySnapshot(
                nativeAllocatedBytes = 30L * 1024 * 1024, // 30 MB
                jvmUsedBytes = 0L,
                systemAvailBytes = 0L,
                timestampMs = 2000L,
            )

        val delta = current.diffFrom(prev)

        assertThat(delta.deltaNativeMb).isWithin(0.001f).of(-20f)
    }

    @Test
    fun `Given older snapshot with less JVM memory, When diffFrom is called, Then deltaJvmMb is positive`() {
        val prev =
            MemorySnapshot(
                nativeAllocatedBytes = 0L,
                jvmUsedBytes = 100L * 1024 * 1024, // 100 MB
                systemAvailBytes = 0L,
                timestampMs = 1000L,
            )
        val current =
            MemorySnapshot(
                nativeAllocatedBytes = 0L,
                jvmUsedBytes = 150L * 1024 * 1024, // 150 MB
                systemAvailBytes = 0L,
                timestampMs = 2000L,
            )

        val delta = current.diffFrom(prev)

        assertThat(delta.deltaJvmMb).isWithin(0.001f).of(50f)
    }

    @Test
    fun `Given system avail decreasing, When diffFrom is called, Then deltaSystemMb is positive`() {
        val prev =
            MemorySnapshot(
                nativeAllocatedBytes = 0L,
                jvmUsedBytes = 0L,
                systemAvailBytes = 2000L * 1024 * 1024, // 2000 MB available
                timestampMs = 1000L,
            )
        val current =
            MemorySnapshot(
                nativeAllocatedBytes = 0L,
                jvmUsedBytes = 0L,
                systemAvailBytes = 1500L * 1024 * 1024, // 1500 MB available
                timestampMs = 2000L,
            )

        val delta = current.diffFrom(prev)

        // System consumption means less available, delta should be positive
        assertThat(delta.deltaSystemMb).isWithin(0.001f).of(500f)
    }

    @Test
    fun `Given snapshots with different timestamps, When diffFrom is called, Then durationMs is correct difference`() {
        val prev =
            MemorySnapshot(
                nativeAllocatedBytes = 0L,
                jvmUsedBytes = 0L,
                systemAvailBytes = 0L,
                timestampMs = 5000L,
            )
        val current =
            MemorySnapshot(
                nativeAllocatedBytes = 0L,
                jvmUsedBytes = 0L,
                systemAvailBytes = 0L,
                timestampMs = 8500L,
            )

        val delta = current.diffFrom(prev)

        assertThat(delta.durationMs).isEqualTo(3500L)
    }

    @Test
    fun `Given current timestamp before previous, When diffFrom is called, Then durationMs is coerced to 0`() {
        val prev =
            MemorySnapshot(
                nativeAllocatedBytes = 0L,
                jvmUsedBytes = 0L,
                systemAvailBytes = 0L,
                timestampMs = 10000L,
            )
        val current =
            MemorySnapshot(
                nativeAllocatedBytes = 0L,
                jvmUsedBytes = 0L,
                systemAvailBytes = 0L,
                timestampMs = 8000L, // Went back in time
            )

        val delta = current.diffFrom(prev)

        assertThat(delta.durationMs).isEqualTo(0L)
    }

    @Test
    fun `Given identical snapshots, When diffFrom is called, Then all deltas are zero`() {
        val prev =
            MemorySnapshot(
                nativeAllocatedBytes = 123456789L,
                jvmUsedBytes = 987654321L,
                systemAvailBytes = 555555555L,
                timestampMs = 1000L,
            )
        val current =
            MemorySnapshot(
                nativeAllocatedBytes = 123456789L,
                jvmUsedBytes = 987654321L,
                systemAvailBytes = 555555555L,
                timestampMs = 1000L,
            )

        val delta = current.diffFrom(prev)

        assertThat(delta.deltaNativeMb).isWithin(0.001f).of(0f)
        assertThat(delta.deltaJvmMb).isWithin(0.001f).of(0f)
        assertThat(delta.deltaSystemMb).isWithin(0.001f).of(0f)
        assertThat(delta.durationMs).isEqualTo(0L)
    }

    @Test
    fun `Given large values, When diffFrom is called, Then maintains precision`() {
        val prev =
            MemorySnapshot(
                nativeAllocatedBytes = 1L * 1024 * 1024 * 1024, // 1 GB
                jvmUsedBytes = 2L * 1024 * 1024 * 1024, // 2 GB
                systemAvailBytes = 8L * 1024 * 1024 * 1024, // 8 GB
                timestampMs = 1000L,
            )
        val current =
            MemorySnapshot(
                nativeAllocatedBytes = 4L * 1024 * 1024 * 1024, // 4 GB
                jvmUsedBytes = 3L * 1024 * 1024 * 1024, // 3 GB
                systemAvailBytes = 6L * 1024 * 1024 * 1024, // 6 GB
                timestampMs = 2000L,
            )

        val delta = current.diffFrom(prev)

        assertThat(delta.deltaNativeMb).isWithin(0.01f).of(3072f) // 3 GB = 3072 MB
        assertThat(delta.deltaJvmMb).isWithin(0.01f).of(1024f) // 1 GB = 1024 MB
        assertThat(delta.deltaSystemMb).isWithin(0.01f).of(2048f) // 8GB - 6GB = 2GB = 2048 MB
    }

    @Test
    fun `Given DebugMemoryStats without args, When created, Then default values are zero or false`() {
        val stats = DebugMemoryStats()

        assertThat(stats.nativeAllocatedMb).isWithin(0.001f).of(0f)
        assertThat(stats.nativeTotalMb).isWithin(0.001f).of(0f)
        assertThat(stats.nativeFreeMb).isWithin(0.001f).of(0f)
        assertThat(stats.jvmUsedMb).isWithin(0.001f).of(0f)
        assertThat(stats.jvmTotalMb).isWithin(0.001f).of(0f)
        assertThat(stats.jvmMaxMb).isWithin(0.001f).of(0f)
        assertThat(stats.ramUsedGb).isWithin(0.001f).of(0f)
        assertThat(stats.ramTotalGb).isWithin(0.001f).of(0f)
        assertThat(stats.ramAvailGb).isWithin(0.001f).of(0f)
        assertThat(stats.isLowMemory).isFalse()
        assertThat(stats.cpuPercent).isWithin(0.001f).of(0f)
    }

    @Test
    fun `Given MemoryDelta without args, When created, Then default values are zero`() {
        val delta = MemoryDelta()

        assertThat(delta.deltaNativeMb).isWithin(0.001f).of(0f)
        assertThat(delta.deltaJvmMb).isWithin(0.001f).of(0f)
        assertThat(delta.deltaSystemMb).isWithin(0.001f).of(0f)
        assertThat(delta.durationMs).isEqualTo(0L)
    }

    @Test
    fun `Given MemorySnapshot, When copied with explicit timestamp, Then timestamp is preserved`() {
        val snapshot =
            MemorySnapshot(
                nativeAllocatedBytes = 100L,
                jvmUsedBytes = 200L,
                systemAvailBytes = 300L,
                timestampMs = 12345L,
            )

        val copied = snapshot.copy(timestampMs = 99999L)

        assertThat(copied.timestampMs).isEqualTo(99999L)
        assertThat(copied.nativeAllocatedBytes).isEqualTo(100L)
        assertThat(copied.jvmUsedBytes).isEqualTo(200L)
        assertThat(copied.systemAvailBytes).isEqualTo(300L)
    }
}
