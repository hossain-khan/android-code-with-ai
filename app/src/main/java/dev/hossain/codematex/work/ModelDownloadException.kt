package dev.hossain.codematex.work

/**
 * Typed, classified failures for the model download pipeline.
 *
 * Each subtype exposes whether the failure is retryable so that [ModelDownloadWorker] can
 * return [androidx.work.ListenableWorker.Result.retry] only for transient network/server
 * problems and [androidx.work.ListenableWorker.Result.failure] for permanent conditions.
 */
sealed class ModelDownloadException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * True when the failure is transient and a later retry may succeed.
     */
    abstract val isRetryable: Boolean

    class ChecksumMismatch(
        expected: String,
        actual: String,
    ) : ModelDownloadException("SHA-256 checksum mismatch: expected $expected, calculated $actual") {
        override val isRetryable: Boolean = false
    }

    class InsufficientStorage(
        availableBytes: Long,
        requiredBytes: Long,
    ) : ModelDownloadException("Insufficient storage space: available $availableBytes bytes, required $requiredBytes bytes") {
        override val isRetryable: Boolean = false
    }

    class HttpError(
        val responseCode: Int,
    ) : ModelDownloadException("HTTP $responseCode") {
        override val isRetryable: Boolean =
            responseCode in 500..599 || responseCode == 408 || responseCode == 429
    }

    class NetworkFailure(
        cause: Throwable,
    ) : ModelDownloadException("Network failure: ${cause.message}", cause) {
        override val isRetryable: Boolean = true
    }

    class InstallationFailure(
        cause: Throwable,
    ) : ModelDownloadException("Failed to install downloaded model: ${cause.message}", cause) {
        override val isRetryable: Boolean = false
    }

    class MalformedInput(
        detail: String,
    ) : ModelDownloadException("Malformed download input: $detail") {
        override val isRetryable: Boolean = false
    }
}
