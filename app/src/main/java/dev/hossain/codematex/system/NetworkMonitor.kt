package dev.hossain.codematex.system

import kotlinx.coroutines.flow.Flow

/**
 * Interface for monitoring active internet connectivity across the application.
 */
interface NetworkMonitor {
    /**
     * Emits `true` when the device is connected to a validated internet network,
     * and `false` otherwise.
     */
    val isOnline: Flow<Boolean>
}
