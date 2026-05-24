package dev.hossain.codematex

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.hossain.codematex.di.AppGraph
import dev.hossain.codematex.work.SampleWorker
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
        createNotificationChannels()
        scheduleBackgroundWork()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val downloadChannel =
                NotificationChannel(
                    "model_download",
                    "Model Downloads",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Notifications for on-device AI model downloads"
                }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(downloadChannel)
        }
    }

    /**
     * Schedules a background work request using the [WorkManager].
     * This is just an example to demonstrate how to use WorkManager with Metro DI.
     */
    private fun scheduleBackgroundWork() {
        val workRequest =
            OneTimeWorkRequestBuilder<SampleWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(workDataOf(SampleWorker.KEY_WORK_NAME to "Circuit App ${System.currentTimeMillis()}"))
                .setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).build()

        appGraph.workManager.enqueue(workRequest)
    }
}
