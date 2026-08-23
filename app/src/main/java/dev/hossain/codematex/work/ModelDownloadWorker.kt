package dev.hossain.codematex.worker

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dev.hossain.codematex.di.AppWorkerFactory
import dev.hossain.codematex.di.AppWorkerFactory.WorkerInstanceFactory
import dev.hossain.codematex.di.WorkerKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.binding
import timber.log.Timber

@AssistedInject
class ModelDownloadWorker(
    context: Context,
    @Assisted params: WorkerParameters,
    private val modelDownloader: ModelDownloader,
) : CoroutineWorker(context, params) {
    companion object {
        const val KEY_URL = "url"
        const val KEY_URLS = "urls"
        const val KEY_PATH = "path"
        const val KEY_PROGRESS = "progress"

        /**
         * Downloads a model file from [urls] using [modelDownloader] and reports progress
         * via [onProgress].
         *
         * This function is extracted from [doWork] so the coordination logic
         * can be unit-tested on the JVM without initializing the WorkManager
         * framework.
         */
        suspend fun executeDownload(
            urls: List<String>,
            outputPath: String,
            modelDownloader: ModelDownloader,
            isStopped: () -> Boolean,
            onProgress: suspend (Int) -> Unit,
        ): Result =
            try {
                Timber.d("ModelDownloadWorker: Starting download with candidate URLs=$urls to $outputPath")

                val result =
                    modelDownloader.download(
                        urls = urls,
                        outputPath = outputPath,
                        onProgress = { progress ->
                            if (isStopped()) throw kotlinx.coroutines.CancellationException("Worker stopped")
                            onProgress(progress)
                        },
                        shouldCancel = isStopped,
                    )

                result
                    .onSuccess {
                        Timber.d("ModelDownloadWorker: Download completed successfully")
                    }.onFailure { error ->
                        Timber.e(error, "ModelDownloadWorker: Download failed for all URLs")
                    }.getOrThrow()

                Result.success()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "ModelDownloadWorker: Error during download work")
                Result.failure()
            }

        suspend fun executeDownload(
            url: String,
            outputPath: String,
            modelDownloader: ModelDownloader,
            isStopped: () -> Boolean,
            onProgress: suspend (Int) -> Unit,
        ): Result = executeDownload(listOf(url), outputPath, modelDownloader, isStopped, onProgress)
    }

    override suspend fun doWork(): Result {
        val urls =
            inputData.getStringArray(KEY_URLS)?.toList()
                ?: listOfNotNull(inputData.getString(KEY_URL))
        if (urls.isEmpty()) return Result.failure()
        val outputPath = inputData.getString(KEY_PATH) ?: return Result.failure()

        setForeground(createForegroundInfo("Starting download..."))

        val result =
            executeDownload(
                urls = urls,
                outputPath = outputPath,
                modelDownloader = modelDownloader,
                isStopped = { isStopped },
                onProgress = { progress -> reportProgress(progress) },
            )

        return if (result == Result.failure() && runAttemptCount < 5) {
            Result.retry()
        } else {
            result
        }
    }

    private suspend fun reportProgress(progress: Int) {
        setProgress(
            Data
                .Builder()
                .putInt(KEY_PROGRESS, progress)
                .build(),
        )
        setForeground(createForegroundInfo("$progress%", progress))
    }

    private fun createForegroundInfo(
        content: String,
        progress: Int = 0,
        maxProgress: Int = 100,
    ): ForegroundInfo {
        val cancelIntent =
            androidx.work.WorkManager
                .getInstance(applicationContext)
                .createCancelPendingIntent(id)

        val notification =
            androidx.core.app.NotificationCompat
                .Builder(applicationContext, "model_download")
                .setContentTitle("Model Download")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setProgress(maxProgress, progress, progress <= 0)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Cancel",
                    cancelIntent,
                ).build()

        return ForegroundInfo(
            1,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    @WorkerKey(ModelDownloadWorker::class)
    @ContributesIntoMap(
        AppScope::class,
        binding = binding<WorkerInstanceFactory<*>>(),
    )
    @AssistedFactory
    abstract class Factory : WorkerInstanceFactory<ModelDownloadWorker>
}
