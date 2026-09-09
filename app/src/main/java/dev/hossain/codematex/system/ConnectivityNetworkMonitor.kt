package dev.hossain.codematex.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation of [NetworkMonitor] that observes the device's default network connection
 * and verifies both [NetworkCapabilities.NET_CAPABILITY_INTERNET] and
 * [NetworkCapabilities.NET_CAPABILITY_VALIDATED] using Android's [ConnectivityManager].
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ConnectivityNetworkMonitor
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : NetworkMonitor {
        override val isOnline: Flow<Boolean> =
            callbackFlow {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                if (connectivityManager == null) {
                    Timber.w("ConnectivityNetworkMonitor: ConnectivityManager is null, defaulting to offline.")
                    trySend(false)
                    close()
                    return@callbackFlow
                }

                fun isNetworkValidated(capabilities: NetworkCapabilities?): Boolean =
                    capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                val networkCallback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            val capabilities = connectivityManager.getNetworkCapabilities(network)
                            trySend(isNetworkValidated(capabilities))
                        }

                        override fun onCapabilitiesChanged(
                            network: Network,
                            networkCapabilities: NetworkCapabilities,
                        ) {
                            trySend(isNetworkValidated(networkCapabilities))
                        }

                        override fun onLost(network: Network) {
                            trySend(false)
                        }

                        override fun onUnavailable() {
                            trySend(false)
                        }
                    }

                // Initial connectivity check
                val activeCapabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                trySend(isNetworkValidated(activeCapabilities))

                try {
                    connectivityManager.registerDefaultNetworkCallback(networkCallback)
                } catch (e: Exception) {
                    Timber.e(e, "ConnectivityNetworkMonitor: Failed to register network callback.")
                    trySend(false)
                }

                awaitClose {
                    try {
                        connectivityManager.unregisterNetworkCallback(networkCallback)
                    } catch (e: Exception) {
                        Timber.w(e, "ConnectivityNetworkMonitor: Failed to unregister network callback.")
                    }
                }
            }.conflate().distinctUntilChanged()
    }
