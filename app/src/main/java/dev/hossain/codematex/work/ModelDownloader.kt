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
     * Downloads the resource from candidate [urls] (trying in order) to [outputPath].
     */
    suspend fun download(
        urls: List<String>,
        outputPath: String,
        onProgress: suspend (percent: Int) -> Unit,
        shouldCancel: () -> Boolean = { false },
    ): Result<Unit> =
        if (urls.isEmpty()) {
            Result.failure(IllegalArgumentException("No download URLs provided"))
        } else {
            download(urls.first(), outputPath, onProgress, shouldCancel)
        }

    /**
     * Downloads the resource at [url] to [outputPath].
     *
     * The implementation is responsible for:
     * - resuming partial downloads when a temporary file already exists,
     * - streaming bytes to disk,
     * - invoking [onProgress] with values in the range 0..100 when the total
     *   size is known, or -1 when it is unknown,
     * - checking [shouldCancel] frequently and aborting cleanly if it returns
     *   true.
     *
     * @return [Result.success] when the download completes and the output file
     *         is ready, or [Result.failure] otherwise.
     */
    suspend fun download(
        url: String,
        outputPath: String,
        onProgress: suspend (percent: Int) -> Unit,
        shouldCancel: () -> Boolean = { false },
    ): Result<Unit>
}
