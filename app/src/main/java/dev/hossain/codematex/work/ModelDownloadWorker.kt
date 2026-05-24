package dev.hossain.codematex.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.Data
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

        setForeground(createForegroundInfo("Downloading model..."))

        val connection = URL(url).openConnection() as HttpURLConnection

        if (outputTmpFile.exists() && outputTmpFile.length() > 0) {
            connection.setRequestProperty("Range", "bytes=${outputTmpFile.length()}-")
            connection.setRequestProperty("Accept-Encoding", "identity")
        }

        connection.connect()
        val totalBytes = connection.contentLengthLong + (outputTmpFile.length().takeIf { it > 0 } ?: 0)

        FileOutputStream(outputTmpFile, true).use { fos ->
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = outputTmpFile.length()

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) return Result.failure()

                    fos.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    setProgress(
                        Data.Builder()
                            .putInt(KEY_PROGRESS, (downloadedBytes * 100 / totalBytes).toInt())
                            .build(),
                    )
                }
            }
        }

        outputTmpFile.renameTo(File(outputPath))
        return Result.success()
    }

    private fun createForegroundInfo(content: String): ForegroundInfo {
        return ForegroundInfo(
            1,
            androidx.core.app.NotificationCompat.Builder(applicationContext, "model_download")
                .setContentTitle("Model Download")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .build(),
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
