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
        const val KEY_SHA256 = "sha256"
        const val KEY_ERROR_MESSAGE = "error_message"

        /**
         * Pure business logic for executing a model download, separated from the WorkManager
         * framework.
         */
        suspend fun executeDownload(
            urls: List<String>,
            outputPath: String,
            expectedSha256: String? = null,
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
                        expectedSha256 = expectedSha256,
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
                val errorData =
                    Data
                        .Builder()
                        .putString(KEY_ERROR_MESSAGE, e.localizedMessage ?: e.message ?: "Download failed")
                        .build()
                Result.failure(errorData)
            }

        suspend fun executeDownload(
            urls: List<String>,
            outputPath: String,
            modelDownloader: ModelDownloader,
            isStopped: () -> Boolean,
            onProgress: suspend (Int) -> Unit,
        ): Result = executeDownload(urls, outputPath, null, modelDownloader, isStopped, onProgress)

        suspend fun executeDownload(
            url: String,
            outputPath: String,
            modelDownloader: ModelDownloader,
            isStopped: () -> Boolean,
            onProgress: suspend (Int) -> Unit,
        ): Result = executeDownload(listOf(url), outputPath, null, modelDownloader, isStopped, onProgress)
    }

    override suspend fun doWork(): Result {
        val urls =
            inputData.getStringArray(KEY_URLS)?.toList()
                ?: listOfNotNull(inputData.getString(KEY_URL))
        if (urls.isEmpty()) {
            return Result.failure(
                Data.Builder().putString(KEY_ERROR_MESSAGE, "No download URLs provided").build(),
            )
        }
        val outputPath =
            inputData.getString(KEY_PATH)
                ?: return Result.failure(
                    Data.Builder().putString(KEY_ERROR_MESSAGE, "Missing output path").build(),
                )
        val expectedSha256 = inputData.getString(KEY_SHA256)

        setForeground(createForegroundInfo("Starting download..."))

        val result =
            executeDownload(
                urls = urls,
                outputPath = outputPath,
                expectedSha256 = expectedSha256,
                modelDownloader = modelDownloader,
                isStopped = { isStopped },
                onProgress = { progress -> reportProgress(progress) },
            )

        return if (result != Result.success() && runAttemptCount < 5) {
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
