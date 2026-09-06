package dev.hossain.codematex.work

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [ModelDownloadException] subclasses and their properties.
 */
class ModelDownloadExceptionTest {
    @Test
    fun `ChecksumMismatch exception has correct message and is not retryable`() {
        val exception = ModelDownloadException.ChecksumMismatch(expected = "abc", actual = "def")

        assertThat(exception.message).isEqualTo("SHA-256 checksum mismatch: expected abc, calculated def")
        assertThat(exception.isRetryable).isFalse()
        assertThat(exception).isInstanceOf(ModelDownloadException::class.java)
        assertThat(exception).isInstanceOf(Exception::class.java)
    }

    @Test
    fun `InsufficientStorage exception has correct message and is not retryable`() {
        val exception = ModelDownloadException.InsufficientStorage(availableBytes = 1000, requiredBytes = 5000)

        assertThat(exception.message).isEqualTo("Insufficient storage space: available 1000 bytes, required 5000 bytes")
        assertThat(exception.isRetryable).isFalse()
        assertThat(exception).isInstanceOf(ModelDownloadException::class.java)
        assertThat(exception).isInstanceOf(Exception::class.java)
    }

    @Test
    fun `HttpError exception has correct message and evaluates retryable based on code`() {
        // Message format
        assertThat(ModelDownloadException.HttpError(404).message).isEqualTo("HTTP 404")

        // Retryable: true for 500..599, 408, 429
        assertThat(ModelDownloadException.HttpError(500).isRetryable).isTrue()
        assertThat(ModelDownloadException.HttpError(503).isRetryable).isTrue()
        assertThat(ModelDownloadException.HttpError(599).isRetryable).isTrue()
        assertThat(ModelDownloadException.HttpError(408).isRetryable).isTrue()
        assertThat(ModelDownloadException.HttpError(429).isRetryable).isTrue()

        // Non-retryable: others
        assertThat(ModelDownloadException.HttpError(200).isRetryable).isFalse()
        assertThat(ModelDownloadException.HttpError(400).isRetryable).isFalse()
        assertThat(ModelDownloadException.HttpError(404).isRetryable).isFalse()
        assertThat(ModelDownloadException.HttpError(600).isRetryable).isFalse()

        val exception = ModelDownloadException.HttpError(500)
        assertThat(exception).isInstanceOf(ModelDownloadException::class.java)
        assertThat(exception).isInstanceOf(Exception::class.java)
    }

    @Test
    fun `NetworkFailure exception has correct message, preserves cause, and is retryable`() {
        val cause = IOException("connection timeout")
        val exception = ModelDownloadException.NetworkFailure(cause)

        assertThat(exception.message).isEqualTo("Network failure: connection timeout")
        assertThat(exception.cause).isEqualTo(cause)
        assertThat(exception.isRetryable).isTrue()
        assertThat(exception).isInstanceOf(ModelDownloadException::class.java)
        assertThat(exception).isInstanceOf(Exception::class.java)
    }

    @Test
    fun `InstallationFailure exception has correct message, preserves cause, and is not retryable`() {
        val cause = IllegalStateException("corrupted file")
        val exception = ModelDownloadException.InstallationFailure(cause)

        assertThat(exception.message).isEqualTo("Failed to install downloaded model: corrupted file")
        assertThat(exception.cause).isEqualTo(cause)
        assertThat(exception.isRetryable).isFalse()
        assertThat(exception).isInstanceOf(ModelDownloadException::class.java)
        assertThat(exception).isInstanceOf(Exception::class.java)
    }

    @Test
    fun `MalformedInput exception has correct message and is not retryable`() {
        val exception = ModelDownloadException.MalformedInput("invalid url")

        assertThat(exception.message).isEqualTo("Malformed download input: invalid url")
        assertThat(exception.isRetryable).isFalse()
        assertThat(exception).isInstanceOf(ModelDownloadException::class.java)
        assertThat(exception).isInstanceOf(Exception::class.java)
    }
}
