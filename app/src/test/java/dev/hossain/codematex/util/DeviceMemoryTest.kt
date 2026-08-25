package dev.hossain.codematex.util

import android.app.ActivityManager
import dev.hossain.codematex.system.MemoryCompatibilityPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Unit tests for [DeviceMemory].
 */
class DeviceMemoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `given total memory - getTotalMemoryBytes returns raw bytes`() {
        val bytes =
            DeviceMemory.getTotalMemoryBytes { memoryInfo ->
                memoryInfo.totalMem = 8L * MemoryCompatibilityPolicy.BYTES_PER_GB
            }

        assertEquals(8L * MemoryCompatibilityPolicy.BYTES_PER_GB, bytes)
    }

    @Test
    fun `given any platform - get process cpu ticks never returns negative value`() {
        // On platforms without /proc (e.g. macOS dev machines) this falls back to 0.
        assertTrue(DeviceMemory.getProcessCpuTicks() >= 0L)
    }

    @Test
    fun `getMemoryStats returns used and total memory in gigabytes`() {
        val stats =
            DeviceMemory.getMemoryStats { memoryInfo ->
                memoryInfo.totalMem = 16L * 1_000_000_000
                memoryInfo.availMem = 12L * 1_000_000_000
            }

        assertEquals(16.0f, stats.totalGb, 0.001f)
        assertEquals(4.0f, stats.usedGb, 0.001f)
    }

    @Test
    fun `getProcessCpuTicks reads utime and stime from proc stat file`() {
        val statFile = temporaryFolder.newFile("stat")
        // /proc/self/stat fields are 0-indexed; utime is at index 13 and stime at index 14.
        val fields = MutableList(44) { "0" }
        fields[0] = "1234"
        fields[1] = "(app)"
        fields[2] = "R"
        fields[13] = "100"
        fields[14] = "200"
        statFile.writeText(fields.joinToString(" ") + "\n")

        val ticks = DeviceMemory.getProcessCpuTicks(statFile.absolutePath)

        assertEquals(300L, ticks)
    }

    @Test
    fun `getProcessCpuTicks returns zero when stat file is missing`() {
        val ticks = DeviceMemory.getProcessCpuTicks("/nonexistent/path/to/stat")

        assertEquals(0L, ticks)
    }

    @Test
    fun `getProcessCpuTicks returns zero when stat file has invalid format`() {
        val statFile = temporaryFolder.newFile("bad-stat")
        statFile.writeText("not enough fields\n")

        val ticks = DeviceMemory.getProcessCpuTicks(statFile.absolutePath)

        assertEquals(0L, ticks)
    }
}
