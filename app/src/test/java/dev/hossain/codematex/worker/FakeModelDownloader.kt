package dev.hossain.codematex.worker

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * In-memory fake of [ModelDownloader] for unit tests.
 */
class FakeModelDownloader : ModelDownloader {
    val downloads = mutableListOf<DownloadCall>()
    val progressReports = mutableListOf<Int>()
    var nextResult: Result<Unit> = Result.success(Unit)
    var progressToReport: List<Int> = emptyList()
    var shouldFailWithCancellation: Boolean = false

    data class DownloadCall(
        val url: String,
        val outputPath: String,
    )

    override suspend fun download(
        url: String,
        outputPath: String,
        onProgress: suspend (percent: Int) -> Unit,
        shouldCancel: () -> Boolean,
    ): Result<Unit> {
        coroutineContext.ensureActive()
        downloads += DownloadCall(url, outputPath)

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
