package dev.hossain.codematex.worker

/**
 * Abstraction over the actual HTTP download and file I/O logic used by
 * [ModelDownloadWorker].
 *
 * Splitting this out makes the download pipeline unit-testable on the JVM
 * without initializing the WorkManager framework or performing real network
 * requests.
 */
interface ModelDownloader {
    /**
     * Downloads the resource from candidate [urls] (trying in order) to [outputPath],
     * optionally verifying file integrity against [expectedSha256].
     */
    suspend fun download(
        urls: List<String>,
        outputPath: String,
        expectedSha256: String? = null,
        onProgress: suspend (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
        shouldCancel: () -> Boolean = { false },
    ): Result<Unit> =
        if (urls.isEmpty()) {
            Result.failure(IllegalArgumentException("No download URLs provided"))
        } else {
            download(urls.first(), outputPath, expectedSha256, onProgress, shouldCancel)
        }

    /**
     * Downloads the resource at [url] to [outputPath], optionally verifying file integrity against [expectedSha256].
     */
    suspend fun download(
        url: String,
        outputPath: String,
        expectedSha256: String? = null,
        onProgress: suspend (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
        shouldCancel: () -> Boolean = { false },
    ): Result<Unit>

    suspend fun download(
        urls: List<String>,
        outputPath: String,
        onProgress: suspend (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
        shouldCancel: () -> Boolean = { false },
    ): Result<Unit> = download(urls, outputPath, null, onProgress, shouldCancel)

    suspend fun download(
        url: String,
        outputPath: String,
        onProgress: suspend (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
        shouldCancel: () -> Boolean = { false },
    ): Result<Unit> = download(url, outputPath, null, onProgress, shouldCancel)

    suspend fun download(
        urls: List<String>,
        outputPath: String,
        expectedSha256: String? = null,
        onProgress: suspend (percent: Int) -> Unit,
        shouldCancel: () -> Boolean = { false },
    ): Result<Unit> =
        download(
            urls = urls,
            outputPath = outputPath,
            expectedSha256 = expectedSha256,
            onProgress = { percent, _, _ -> onProgress(percent) },
            shouldCancel = shouldCancel,
        )

    suspend fun download(
        url: String,
        outputPath: String,
        expectedSha256: String? = null,
        onProgress: suspend (percent: Int) -> Unit,
        shouldCancel: () -> Boolean = { false },
    ): Result<Unit> =
        download(
            url = url,
            outputPath = outputPath,
            expectedSha256 = expectedSha256,
            onProgress = { percent, _, _ -> onProgress(percent) },
            shouldCancel = shouldCancel,
        )
}
