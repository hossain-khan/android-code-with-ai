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
    var nextResult: Result<Unit> = Result.success(Unit)
    var progressToReport: List<Int> = emptyList()
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
        onProgress: suspend (percent: Int) -> Unit,
        shouldCancel: () -> Boolean,
    ): Result<Unit> {
        multiUrlDownloads += MultiUrlDownloadCall(urls, outputPath, expectedSha256)
        return super.download(urls, outputPath, expectedSha256, onProgress, shouldCancel)
    }

    override suspend fun download(
        url: String,
        outputPath: String,
        expectedSha256: String?,
        onProgress: suspend (percent: Int) -> Unit,
        shouldCancel: () -> Boolean,
    ): Result<Unit> {
        coroutineContext.ensureActive()
        downloads += DownloadCall(url, outputPath, expectedSha256)

        progressToReport.forEach { progress ->
            coroutineContext.ensureActive()
            if (shouldCancel()) {
                throw kotlinx.coroutines.CancellationException("Download cancelled")
            }
            progressReports += progress
            onProgress(progress)
        }

        if (shouldFailWithCancellation) {
            throw kotlinx.coroutines.CancellationException("Simulated cancellation")
        }

        return nextResult
    }
}
