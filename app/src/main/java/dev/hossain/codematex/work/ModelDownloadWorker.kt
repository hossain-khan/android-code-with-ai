package dev.hossain.codematex.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dev.hossain.codematex.MainActivity
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
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_SHA256 = "sha256"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_MODEL_ID = "model_id"
        const val KEY_MODEL_NAME = "model_name"
        const val NOTIFICATION_ID_DOWNLOAD_COMPLETE = 2

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
            onProgress: suspend (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
        ): Result =
            try {
                Timber.d("ModelDownloadWorker: Starting download with candidate URLs=$urls to $outputPath")

                val result =
                    modelDownloader.download(
                        urls = urls,
                        outputPath = outputPath,
                        expectedSha256 = expectedSha256,
                        onProgress = { progress, bytesDownloaded, totalBytes ->
                            if (isStopped()) throw kotlinx.coroutines.CancellationException("Worker stopped")
                            onProgress(progress, bytesDownloaded, totalBytes)
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
            expectedSha256: String? = null,
            modelDownloader: ModelDownloader,
            isStopped: () -> Boolean,
            onProgress: suspend (Int) -> Unit,
        ): Result = executeDownload(urls, outputPath, expectedSha256, modelDownloader, isStopped) { p, _, _ -> onProgress(p) }

        suspend fun executeDownload(
            urls: List<String>,
            outputPath: String,
            modelDownloader: ModelDownloader,
            isStopped: () -> Boolean,
            onProgress: suspend (Int) -> Unit,
        ): Result = executeDownload(urls, outputPath, null, modelDownloader, isStopped, onProgress)

        suspend fun executeDownload(
            urls: List<String>,
            outputPath: String,
            modelDownloader: ModelDownloader,
            isStopped: () -> Boolean,
            onProgress: suspend (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
        ): Result = executeDownload(urls, outputPath, null, modelDownloader, isStopped, onProgress)

        suspend fun executeDownload(
            url: String,
            outputPath: String,
            modelDownloader: ModelDownloader,
            isStopped: () -> Boolean,
            onProgress: suspend (Int) -> Unit,
        ): Result = executeDownload(listOf(url), outputPath, null, modelDownloader, isStopped, onProgress)

        suspend fun executeDownload(
            url: String,
            outputPath: String,
            modelDownloader: ModelDownloader,
            isStopped: () -> Boolean,
            onProgress: suspend (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
        ): Result = executeDownload(listOf(url), outputPath, null, modelDownloader, isStopped, onProgress)

        suspend fun executeDownload(
            url: String,
            outputPath: String,
            expectedSha256: String? = null,
            modelDownloader: ModelDownloader,
            isStopped: () -> Boolean,
            onProgress: suspend (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
        ): Result = executeDownload(listOf(url), outputPath, expectedSha256, modelDownloader, isStopped, onProgress)
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

        try {
            setForeground(createForegroundInfo("Starting download..."))
        } catch (e: Exception) {
            Timber.w(e, "ModelDownloadWorker: Failed to start foreground service, continuing download in background")
        }

        val result =
            executeDownload(
                urls = urls,
                outputPath = outputPath,
                expectedSha256 = expectedSha256,
                modelDownloader = modelDownloader,
                isStopped = { isStopped },
                onProgress = { progress, bytesDownloaded, totalBytes ->
                    reportProgress(progress, bytesDownloaded, totalBytes)
                },
            )

        if (result == Result.success()) {
            showDownloadCompleteNotification()
        }

        return if (result != Result.success() && runAttemptCount < 5) {
            Result.retry()
        } else {
            result
        }
    }

    private fun showDownloadCompleteNotification() {
        val modelName =
            inputData.getString(KEY_MODEL_NAME)
                ?: inputData.getString(KEY_MODEL_ID)?.substringAfterLast("/")
                ?: "AI Model"

        val homeIntent =
            Intent(
                applicationContext,
                MainActivity::class.java,
            ).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val contentPendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                homeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            androidx.core.app.NotificationCompat
                .Builder(applicationContext, "model_download")
                .setContentTitle("$modelName is ready!")
                .setContentText("Download complete. Tap to explore coding topics with your on-device AI tutor!")
                .setStyle(
                    androidx.core.app.NotificationCompat
                        .BigTextStyle()
                        .setBigContentTitle("$modelName is ready!")
                        .bigText(
                            "$modelName has been downloaded and installed. Tap to start exploring Kotlin, Jetpack Compose, Android Architecture, and Algorithms with your personal on-device AI tutor!",
                        ),
                ).setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .build()

        val notificationManager =
            androidx.core.app.NotificationManagerCompat
                .from(applicationContext)

        try {
            notificationManager.notify(
                NOTIFICATION_ID_DOWNLOAD_COMPLETE,
                notification,
            )
        } catch (e: SecurityException) {
            Timber.w(e, "ModelDownloadWorker: Missing notification permission for completion notification")
        } catch (e: Exception) {
            Timber.w(e, "ModelDownloadWorker: Failed to post download completion notification")
        }
    }

    private suspend fun reportProgress(
        progress: Int,
        bytesDownloaded: Long = 0L,
        totalBytes: Long = 0L,
    ) {
        setProgress(
            Data
                .Builder()
                .putInt(KEY_PROGRESS, progress)
                .putLong(KEY_BYTES_DOWNLOADED, bytesDownloaded)
                .putLong(KEY_TOTAL_BYTES, totalBytes)
                .build(),
        )
        try {
            val content =
                if (totalBytes > 0) {
                    val downloadedMb = bytesDownloaded / 1_000_000L
                    val totalMb = totalBytes / 1_000_000L
                    String.format(java.util.Locale.US, "%d%% • %,d MB / %,d MB", progress, downloadedMb, totalMb)
                } else if (bytesDownloaded > 0) {
                    val downloadedMb = bytesDownloaded / 1_000_000L
                    String.format(java.util.Locale.US, "%d%% • %,d MB", progress, downloadedMb)
                } else {
                    "$progress%"
                }
            setForeground(createForegroundInfo(content, progress))
        } catch (e: Exception) {
            Timber.w(e, "ModelDownloadWorker: Failed to update foreground notification")
        }
    }

    private fun createForegroundInfo(
        content: String,
        progress: Int = 0,
        maxProgress: Int = 100,
    ): ForegroundInfo {
        val modelName =
            inputData.getString(KEY_MODEL_NAME)
                ?: inputData.getString(KEY_MODEL_ID)?.substringAfterLast("/")
                ?: "AI Model"

        val cancelIntent =
            androidx.work.WorkManager
                .getInstance(applicationContext)
                .createCancelPendingIntent(id)

        val deepLinkIntent =
            Intent(
                Intent.ACTION_VIEW,
                "codematex://models".toUri(),
                applicationContext,
                MainActivity::class.java,
            ).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_TARGET_SCREEN, MainActivity.SCREEN_MODELS)
            }

        val contentPendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                deepLinkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            androidx.core.app.NotificationCompat
                .Builder(applicationContext, "model_download")
                .setContentTitle("Downloading $modelName")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
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
