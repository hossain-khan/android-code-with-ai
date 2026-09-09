package dev.hossain.codematex.system

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [NetworkMonitor] for unit testing.
 */
class FakeNetworkMonitor(
    initialIsOnline: Boolean = true,
) : NetworkMonitor {
    val isOnlineFlow = MutableStateFlow(initialIsOnline)

    override val isOnline: Flow<Boolean> get() = isOnlineFlow

    fun setOnline(online: Boolean) {
        isOnlineFlow.value = online
    }
}
