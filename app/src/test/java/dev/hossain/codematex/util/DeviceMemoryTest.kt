package dev.hossain.codematex.util

import android.app.ActivityManager
import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.system.MemoryCompatibilityPolicy
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

        assertThat(bytes).isEqualTo(8L * MemoryCompatibilityPolicy.BYTES_PER_GB)
    }

    @Test
    fun `given any platform - get process cpu ticks never returns negative value`() {
        // On platforms without /proc (e.g. macOS dev machines) this falls back to 0.
        assertThat(DeviceMemory.getProcessCpuTicks()).isAtLeast(0L)
    }

    @Test
    fun `getMemoryStats returns used and total memory in gigabytes`() {
        val stats =
            DeviceMemory.getMemoryStats { memoryInfo ->
                memoryInfo.totalMem = 16L * MemoryCompatibilityPolicy.BYTES_PER_GB
                memoryInfo.availMem = 12L * MemoryCompatibilityPolicy.BYTES_PER_GB
            }

        assertThat(stats.totalGb).isWithin(0.001f).of(16.0f)
        assertThat(stats.usedGb).isWithin(0.001f).of(4.0f)
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

        assertThat(ticks).isEqualTo(300L)
    }

    @Test
    fun `getProcessCpuTicks returns zero when stat file is missing`() {
        val ticks = DeviceMemory.getProcessCpuTicks("/nonexistent/path/to/stat")

        assertThat(ticks).isEqualTo(0L)
    }

    @Test
    fun `getProcessCpuTicks returns zero when stat file has invalid format`() {
        val statFile = temporaryFolder.newFile("bad-stat")
        statFile.writeText("not enough fields\n")

        val ticks = DeviceMemory.getProcessCpuTicks(statFile.absolutePath)

        assertThat(ticks).isEqualTo(0L)
    }
}
