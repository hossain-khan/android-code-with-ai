package dev.hossain.codematex.worker

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Production implementation of [ModelDownloader] that downloads model files
 * over HTTP using [HttpURLConnection] with support for resuming partial
 * downloads.
 */
@ContributesBinding(AppScope::class)
class HttpModelDownloader
    @Inject
    constructor() : ModelDownloader {
        override suspend fun download(
            urls: List<String>,
            outputPath: String,
            onProgress: suspend (percent: Int) -> Unit,
            shouldCancel: () -> Boolean,
        ): Result<Unit> {
            if (urls.isEmpty()) {
                return Result.failure(IllegalArgumentException("No download URLs provided"))
            }

            var lastError: Throwable = IllegalStateException("Download failed for all candidate URLs")
            for ((index, url) in urls.withIndex()) {
                Timber.d("HttpModelDownloader: Trying candidate URL (${index + 1}/${urls.size}): $url")
                val result = download(url, outputPath, onProgress, shouldCancel)
                if (result.isSuccess) {
                    return result
                }
                val error = result.exceptionOrNull()
                if (error is CancellationException) {
                    throw error
                }
                if (error != null) {
                    lastError = error
                    Timber.w(error, "HttpModelDownloader: Candidate URL failed ($url), attempting fallback if available")
                }
            }

            return Result.failure(lastError)
        }

        override suspend fun download(
            url: String,
            outputPath: String,
            onProgress: suspend (percent: Int) -> Unit,
            shouldCancel: () -> Boolean,
        ): Result<Unit> =
            withContext(Dispatchers.IO) {
                val outputTmpFile = File("$outputPath.codematextmp")

                try {
                    Timber.d("HttpModelDownloader: Downloading $url to $outputPath")

                    val connection = URL(url).openConnection() as HttpURLConnection

                    if (outputTmpFile.exists() && outputTmpFile.length() > 0) {
                        connection.setRequestProperty("Range", "bytes=${outputTmpFile.length()}-")
                        connection.setRequestProperty("Accept-Encoding", "identity")
                    }

                    connection.connect()
                    val responseCode = connection.responseCode
                    Timber.d("HttpModelDownloader: Response code=$responseCode for $url")

                    if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                        Timber.e("HttpModelDownloader: Failed with response code $responseCode")
                        return@withContext Result.failure(
                            IllegalStateException("HTTP $responseCode"),
                        )
                    }

                    val isResuming = responseCode == HttpURLConnection.HTTP_PARTIAL
                    val initialBytes = if (isResuming) outputTmpFile.length() else 0L

                    val contentLength = connection.contentLengthLong
                    val totalBytes =
                        if (contentLength > 0) {
                            contentLength + initialBytes
                        } else {
                            Timber.w("HttpModelDownloader: Unknown content length")
                            0L
                        }

                    Timber.d(
                        "HttpModelDownloader: Content-Length=$contentLength, Total=$totalBytes, isResuming=$isResuming, Starting from $initialBytes",
                    )

                    outputTmpFile.parentFile?.mkdirs()
                    File(outputPath).parentFile?.mkdirs()

                    FileOutputStream(outputTmpFile, isResuming).use { fos ->
                        connection.inputStream.use { input ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var downloadedBytes = initialBytes
                            var lastReportedProgress = -1
                            var lastReportedBytes = initialBytes
                            val reportInterval = 100_000_000L // 100MB

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                ensureActive()
                                if (shouldCancel()) {
                                    throw CancellationException("Download cancelled")
                                }

                                fos.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead

                                val progress = if (totalBytes > 0) (downloadedBytes * 100 / totalBytes).toInt() else -1

                                if (progress != lastReportedProgress &&
                                    (progress % 5 == 0 || downloadedBytes - lastReportedBytes >= reportInterval)
                                ) {
                                    lastReportedProgress = progress
                                    lastReportedBytes = downloadedBytes
                                    onProgress(progress)
                                    Timber.i(
                                        "HttpModelDownloader: Progress=$progress% (${downloadedBytes / 1_000_000}MB / ${totalBytes / 1_000_000}MB)",
                                    )
                                }
                            }
                        }
                    }

                    outputTmpFile.renameTo(File(outputPath))
                    Timber.d("HttpModelDownloader: Download completed for $url")
                    Result.success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "HttpModelDownloader: Error downloading model")
                    Result.failure(e)
                }
            }
    }
