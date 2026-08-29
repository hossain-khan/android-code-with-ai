package dev.hossain.codematex

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import dev.hossain.codematex.di.AppGraph
import dev.hossain.codematex.work.ModelDownloadWorker
import dev.zacsweers.metro.createGraphFactory
import timber.log.Timber

/**
 * Application class for the app with key initializations.
 *
 * This class demonstrates the following Metro features:
 * - Graph creation using [createGraphFactory]
 * - Lazy initialization of the dependency graph
 *
 * See https://zacsweers.github.io/metro/latest/dependency-graphs/ for more on creating graphs.
 */
class CodeWithAIApp :
    Application(),
    Configuration.Provider {
    /**
     * Lazily creates the Metro app graph using the factory pattern.
     *
     * [createGraphFactory] is a Metro intrinsic function that generates a factory
     * for creating the dependency graph. The graph is created with the Application
     * context as a runtime dependency.
     *
     * See https://zacsweers.github.io/metro/latest/dependency-graphs/#creating-factories
     */
    val appGraph by lazy { createGraphFactory<AppGraph.Factory>().create(this) }

    fun appGraph(): AppGraph = appGraph

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(appGraph.workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("CodeWithAIApp created")
        appGraph.systemMemoryManager.register(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel =
                NotificationChannel(
                    ModelDownloadWorker.CHANNEL_ID_DOWNLOAD_PROGRESS,
                    "Model Download Progress",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Silent ongoing progress notifications for on-device AI model downloads"
                    setShowBadge(false)
                }

            val completeChannel =
                NotificationChannel(
                    ModelDownloadWorker.CHANNEL_ID_DOWNLOAD_COMPLETE,
                    "Model Download Completed",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Notifications when an on-device AI model download has finished"
                    setShowBadge(true)
                }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(listOf(progressChannel, completeChannel))
            Timber.d("Created notification channels for model downloads")
        }
    }
}
