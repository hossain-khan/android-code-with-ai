package dev.hossain.codematex.worker

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * In-memory fake of [ModelDownloader] for unit tests.
 */
class FakeModelDownloader : ModelDownloader {
    val downloads = mutableListOf<DownloadCall>()
    val multiUrlDownloads = mutableListOf<MultiUrlDownloadCall>()
    val progressReports = mutableListOf<Int>()
    val detailedProgressReports = mutableListOf<Triple<Int, Long, Long>>()
    var nextResult: Result<Unit> = Result.success(Unit)
    var progressToReport: List<Int> = emptyList()
    var detailedProgressToReport: List<Triple<Int, Long, Long>> = emptyList()
    var shouldFailWithCancellation: Boolean = false

    data class DownloadCall(
        val url: String,
        val outputPath: String,
        val expectedSha256: String? = null,
    )

    data class MultiUrlDownloadCall(
        val urls: List<String>,
        val outputPath: String,
        val expectedSha256: String? = null,
    )

    override suspend fun download(
        urls: List<String>,
        outputPath: String,
        expectedSha256: String?,
        onProgress: suspend (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
        shouldCancel: () -> Boolean,
    ): Result<Unit> {
        multiUrlDownloads += MultiUrlDownloadCall(urls, outputPath, expectedSha256)
        return super.download(urls, outputPath, expectedSha256, onProgress, shouldCancel)
    }

    override suspend fun download(
        url: String,
        outputPath: String,
        expectedSha256: String?,
        onProgress: suspend (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
        shouldCancel: () -> Boolean,
    ): Result<Unit> {
        coroutineContext.ensureActive()
        downloads += DownloadCall(url, outputPath, expectedSha256)

        if (detailedProgressToReport.isNotEmpty()) {
            detailedProgressToReport.forEach { (progress, bytes, total) ->
                coroutineContext.ensureActive()
                if (shouldCancel()) {
                    throw kotlinx.coroutines.CancellationException("Download cancelled")
                }
                progressReports += progress
                detailedProgressReports += Triple(progress, bytes, total)
                onProgress(progress, bytes, total)
            }
        } else {
            progressToReport.forEach { progress ->
                coroutineContext.ensureActive()
                if (shouldCancel()) {
                    throw kotlinx.coroutines.CancellationException("Download cancelled")
                }
                progressReports += progress
                onProgress(progress, progress * 10_000_000L, 100_000_000L)
            }
        }

        if (shouldFailWithCancellation) {
            throw kotlinx.coroutines.CancellationException("Simulated cancellation")
        }

        return nextResult
    }
}
