package dev.hossain.codematex.work

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Production implementation of [ModelDownloader] that downloads model files
 * over HTTP using [OkHttpClient] with support for resuming partial
 * downloads and verifying SHA-256 digests.
 *
 * The [okHttpClient] is provided by the dependency graph (see
 * [dev.hossain.codematex.data.network.NetworkingGraph]); there is no default
 * fallback so a missing binding fails at compile time rather than silently
 * using a differently configured client at runtime.
 */
@ContributesBinding(AppScope::class)
class HttpModelDownloader
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
    ) : ModelDownloader {
        internal var spaceChecker: (File) -> Long = { it.usableSpace }

        /**
         * Test seam for the final install step. The default performs an atomic move with a safe
         * checked fallback; tests can override it to simulate installation failures.
         */
        internal var fileMover: (File, File) -> Unit = { tmpFile, destination -> installFile(tmpFile, destination) }

        override suspend fun download(
            urls: List<String>,
            outputPath: String,
            expectedSha256: String?,
            onProgress: suspend (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
            shouldCancel: () -> Boolean,
        ): Result<Unit> {
            if (urls.isEmpty()) {
                return Result.failure(IllegalArgumentException("No download URLs provided"))
            }

            var lastError: Throwable = IllegalStateException("No URLs to download")

            for (url in urls) {
                Timber.d("HttpModelDownloader: Trying candidate URL: $url")
                val result = download(url, outputPath, expectedSha256, onProgress, shouldCancel)
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
            expectedSha256: String?,
            onProgress: suspend (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
            shouldCancel: () -> Boolean,
        ): Result<Unit> =
            withContext(Dispatchers.IO) {
                val outputTmpFile = File("$outputPath.codematextmp")

                try {
                    Timber.d("HttpModelDownloader: Downloading $url to $outputPath")

                    val requestBuilder = Request.Builder().url(url)

                    if (outputTmpFile.exists() && outputTmpFile.length() > 0) {
                        requestBuilder.header("Range", "bytes=${outputTmpFile.length()}-")
                        requestBuilder.header("Accept-Encoding", "identity")
                    }

                    val response = okHttpClient.newCall(requestBuilder.build()).execute()

                    response.use { resp ->
                        val responseCode = resp.code
                        Timber.d("HttpModelDownloader: Response code=$responseCode for $url")

                        if (responseCode != 200 && responseCode != 206) {
                            Timber.e("HttpModelDownloader: Failed with response code $responseCode")
                            return@withContext Result.failure(
                                ModelDownloadException.HttpError(responseCode),
                            )
                        }

                        val body = resp.body
                        val isResuming = responseCode == 206
                        val initialBytes = if (isResuming) outputTmpFile.length() else 0L

                        val contentLength = body.contentLength()
                        val totalBytes =
                            if (contentLength > 0) {
                                contentLength + initialBytes
                            } else {
                                Timber.w("HttpModelDownloader: Unknown content length")
                                0L
                            }

                        val targetDir = (File(outputPath).parentFile ?: outputTmpFile.parentFile ?: File(".")).apply { mkdirs() }
                        val availableSpace = spaceChecker(targetDir)
                        val requiredBytes = if (isResuming) contentLength else totalBytes

                        if (availableSpace > 0 && requiredBytes > 0 && availableSpace < requiredBytes) {
                            Timber.e(
                                "HttpModelDownloader: Insufficient storage space. Required=$requiredBytes B, Available=$availableSpace B",
                            )
                            return@withContext Result.failure(
                                ModelDownloadException.InsufficientStorage(
                                    availableBytes = availableSpace,
                                    requiredBytes = requiredBytes,
                                ),
                            )
                        }

                        Timber.d(
                            "HttpModelDownloader: Content-Length=$contentLength, Total=$totalBytes, isResuming=$isResuming, Starting from $initialBytes",
                        )

                        outputTmpFile.parentFile?.mkdirs()
                        File(outputPath).parentFile?.mkdirs()

                        FileOutputStream(outputTmpFile, isResuming).use { fos ->
                            body.byteStream().use { input ->
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

                                    if ((progress != -1 && progress != lastReportedProgress) ||
                                        (progress == -1 && downloadedBytes - lastReportedBytes >= reportInterval)
                                    ) {
                                        lastReportedProgress = progress
                                        lastReportedBytes = downloadedBytes
                                        onProgress(progress, downloadedBytes, totalBytes)
                                        Timber.i(
                                            "HttpModelDownloader: Progress=$progress% (${downloadedBytes / 1_000_000}MB / ${totalBytes / 1_000_000}MB)",
                                        )
                                    }
                                }
                            }
                        }

                        if (expectedSha256 != null) {
                            Timber.d("HttpModelDownloader: Verifying SHA-256 checksum against $expectedSha256")
                            val actualHash = computeSha256(outputTmpFile, shouldCancel)
                            if (!actualHash.equals(expectedSha256, ignoreCase = true)) {
                                Timber.e("HttpModelDownloader: Checksum mismatch! Expected=$expectedSha256, Actual=$actualHash")
                                outputTmpFile.delete()
                                return@withContext Result.failure(
                                    ModelDownloadException.ChecksumMismatch(
                                        expected = expectedSha256,
                                        actual = actualHash,
                                    ),
                                )
                            }
                            Timber.d("HttpModelDownloader: Checksum verified successfully")
                        }

                        val outputFile = File(outputPath)
                        val expectedSize = if (totalBytes > 0) totalBytes else outputTmpFile.length()

                        try {
                            fileMover(outputTmpFile, outputFile)
                        } catch (e: Exception) {
                            Timber.e(e, "HttpModelDownloader: Failed to install downloaded file")
                            outputTmpFile.delete()
                            outputFile.delete()
                            return@withContext Result.failure(ModelDownloadException.InstallationFailure(e))
                        }

                        if (!outputFile.exists() || outputFile.length() != expectedSize) {
                            Timber.e(
                                "HttpModelDownloader: Destination verification failed. " +
                                    "exists=${outputFile.exists()}, size=${outputFile.length()}, expected=$expectedSize",
                            )
                            outputFile.delete()
                            return@withContext Result.failure(
                                ModelDownloadException.InstallationFailure(
                                    IllegalStateException(
                                        "Destination verification failed: size=${outputFile.length()}, expected=$expectedSize",
                                    ),
                                ),
                            )
                        }

                        if (expectedSha256 != null) {
                            val destinationHash = computeSha256(outputFile, shouldCancel)
                            if (!destinationHash.equals(expectedSha256, ignoreCase = true)) {
                                Timber.e(
                                    "HttpModelDownloader: Destination checksum mismatch! Expected=$expectedSha256, Actual=$destinationHash",
                                )
                                outputFile.delete()
                                return@withContext Result.failure(
                                    ModelDownloadException.InstallationFailure(
                                        IllegalStateException(
                                            "Destination checksum mismatch: expected $expectedSha256, calculated $destinationHash",
                                        ),
                                    ),
                                )
                            }
                        }

                        outputTmpFile.delete()
                        Timber.d("HttpModelDownloader: Download completed for $url")
                        Result.success(Unit)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    Timber.e(e, "HttpModelDownloader: Network or file I/O error")
                    Result.failure(ModelDownloadException.NetworkFailure(e))
                } catch (e: Exception) {
                    Timber.e(e, "HttpModelDownloader: Error downloading model")
                    Result.failure(e)
                }
            }

        private fun installFile(
            tmpFile: File,
            destination: File,
        ) {
            destination.parentFile?.mkdirs()
            try {
                Files.move(
                    tmpFile.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (e: AtomicMoveNotSupportedException) {
                Timber.w(e, "HttpModelDownloader: Atomic move not supported, falling back to copy+delete")
                Files.copy(
                    tmpFile.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
                tmpFile.delete()
            }
        }

        private fun computeSha256(
            file: File,
            shouldCancel: () -> Boolean,
        ): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(65536)
            file.inputStream().use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (shouldCancel()) throw CancellationException("Cancelled during checksum calculation")
                    digest.update(buffer, 0, bytesRead)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
