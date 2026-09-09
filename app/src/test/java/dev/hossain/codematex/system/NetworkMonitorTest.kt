package dev.hossain.codematex.system

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NetworkMonitorTest {
    @Test
    fun `fake network monitor emits initial online state`() =
        runTest {
            val monitor = FakeNetworkMonitor(initialIsOnline = true)
            assertThat(monitor.isOnline.first()).isTrue()
        }

    @Test
    fun `fake network monitor emits offline when updated`() =
        runTest {
            val monitor = FakeNetworkMonitor(initialIsOnline = true)
            monitor.setOnline(false)
            assertThat(monitor.isOnline.first()).isFalse()
        }
}
