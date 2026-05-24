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
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@AssistedInject
class ModelDownloadWorker(
    context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {
    companion object {
        const val KEY_URL = "url"
        const val KEY_PATH = "path"
        const val KEY_PROGRESS = "progress"
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val outputPath = inputData.getString(KEY_PATH) ?: return Result.failure()
        val outputTmpFile = File("$outputPath.codematextmp")

        setForeground(createForegroundInfo("Starting download..."))

        Timber.d("ModelDownloadWorker: Downloading $url to $outputPath")

        val connection = URL(url).openConnection() as HttpURLConnection

        if (outputTmpFile.exists() && outputTmpFile.length() > 0) {
            connection.setRequestProperty("Range", "bytes=${outputTmpFile.length()}-")
            connection.setRequestProperty("Accept-Encoding", "identity")
        }

        connection.connect()
        val responseCode = connection.responseCode
        Timber.d("ModelDownloadWorker: Response code=$responseCode for $url")

        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
            Timber.e("ModelDownloadWorker: Failed with response code $responseCode")
            return Result.failure()
        }

        val contentLength = connection.contentLengthLong
        val totalBytes =
            if (contentLength > 0) {
                contentLength + (outputTmpFile.length().takeIf { it > 0 } ?: 0)
            } else {
                Timber.w("ModelDownloadWorker: Unknown content length")
                0L
            }

        Timber.d("ModelDownloadWorker: Content-Length=$contentLength, Total=$totalBytes, Resuming from ${outputTmpFile.length()}")

        outputTmpFile.parentFile?.mkdirs()
        File(outputPath).parentFile?.mkdirs()

        FileOutputStream(outputTmpFile, true).use { fos ->
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = outputTmpFile.length()
                var lastReportedProgress = -1
                var lastReportedBytes = 0L
                val reportInterval = 100_000_000L // 100MB

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) return Result.failure()

                    fos.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val progress = if (totalBytes > 0) (downloadedBytes * 100 / totalBytes).toInt() else -1

                    if (progress != lastReportedProgress && (progress % 5 == 0 || downloadedBytes - lastReportedBytes >= reportInterval)) {
                        lastReportedProgress = progress
                        lastReportedBytes = downloadedBytes
                        setProgress(
                            Data
                                .Builder()
                                .putInt(KEY_PROGRESS, progress)
                                .build(),
                        )
                        Timber.i(
                            "ModelDownloadWorker: Progress=$progress% (${downloadedBytes / 1_000_000}MB / ${totalBytes / 1_000_000}MB)",
                        )
                        setForeground(
                            createForegroundInfo(
                                "$progress% - ${downloadedBytes / 1_000_000}MB / ${totalBytes / 1_000_000}MB",
                                progress,
                            ),
                        )
                    }
                }
            }
        }

        outputTmpFile.renameTo(File(outputPath))
        return Result.success()
    }

    private fun createForegroundInfo(
        content: String,
        progress: Int = 0,
        maxProgress: Int = 100,
    ): ForegroundInfo =
        ForegroundInfo(
            1,
            androidx.core.app.NotificationCompat
                .Builder(applicationContext, "model_download")
                .setContentTitle("Model Download")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setProgress(maxProgress, progress, progress <= 0)
                .build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )

    @WorkerKey(ModelDownloadWorker::class)
    @ContributesIntoMap(
        AppScope::class,
        binding = binding<WorkerInstanceFactory<*>>(),
    )
    @AssistedFactory
    abstract class Factory : WorkerInstanceFactory<ModelDownloadWorker>
}
