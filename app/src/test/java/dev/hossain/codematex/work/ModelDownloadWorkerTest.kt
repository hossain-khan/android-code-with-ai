package dev.hossain.codematex.work

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import androidx.work.ListenableWorker.Result as WorkResult

/**
 * Unit tests for [ModelDownloadWorker] coordination logic.
 *
 * These tests exercise [ModelDownloadWorker.executeDownload], which contains
 * the WorkManager-independent download coordination logic extracted from
 * [ModelDownloadWorker.doWork] so it can be verified on the JVM without
 * Robolectric or WorkManager test infrastructure.
 */
class ModelDownloadWorkerTest {
    @Test
    fun `given successful download - execute download returns success`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            val progress = mutableListOf<Int>()

            val result =
                ModelDownloadWorker.executeDownload(
                    url = "https://example.com/model.bin",
                    outputPath = "/models/model.bin",
                    modelDownloader = fakeDownloader,
                    isStopped = { false },
                    onProgress = { progress += it },
                )

            assertThat(result).isEqualTo(WorkResult.success())
            assertThat(fakeDownloader.downloads).containsExactly(
                FakeModelDownloader.DownloadCall("https://example.com/model.bin", "/models/model.bin"),
            )
        }

    @Test
    fun `given permanent download failure - execute download returns failure with non-retryable data`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            fakeDownloader.nextResult =
                kotlin.Result.failure(
                    ModelDownloadException.ChecksumMismatch(
                        expected = "expected",
                        actual = "actual",
                    ),
                )

            val result =
                ModelDownloadWorker.executeDownload(
                    url = "https://example.com/model.bin",
                    outputPath = "/models/model.bin",
                    modelDownloader = fakeDownloader,
                    isStopped = { false },
                    onProgress = {},
                )

            val expectedErrorData =
                androidx.work.Data
                    .Builder()
                    .putString(ModelDownloadWorker.KEY_ERROR_MESSAGE, "SHA-256 checksum mismatch: expected expected, calculated actual")
                    .putBoolean(ModelDownloadWorker.KEY_ERROR_RETRYABLE, false)
                    .build()
            assertThat(result).isEqualTo(WorkResult.failure(expectedErrorData))
        }

    @Test
    fun `given transient download failure - execute download returns failure with retryable data`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            fakeDownloader.nextResult = kotlin.Result.failure(ModelDownloadException.NetworkFailure(IOException("timeout")))

            val result =
                ModelDownloadWorker.executeDownload(
                    url = "https://example.com/model.bin",
                    outputPath = "/models/model.bin",
                    modelDownloader = fakeDownloader,
                    isStopped = { false },
                    onProgress = {},
                )

            val expectedErrorData =
                androidx.work.Data
                    .Builder()
                    .putString(ModelDownloadWorker.KEY_ERROR_MESSAGE, "Network failure: timeout")
                    .putBoolean(ModelDownloadWorker.KEY_ERROR_RETRYABLE, true)
                    .build()
            assertThat(result).isEqualTo(WorkResult.failure(expectedErrorData))
        }

    @Test
    fun `given http 500 error - execute download marks error as retryable`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            fakeDownloader.nextResult = kotlin.Result.failure(ModelDownloadException.HttpError(500))

            val result =
                ModelDownloadWorker.executeDownload(
                    url = "https://example.com/model.bin",
                    outputPath = "/models/model.bin",
                    modelDownloader = fakeDownloader,
                    isStopped = { false },
                    onProgress = {},
                )

            assertThat(result).isInstanceOf(WorkResult.Failure::class.java)
            assertThat((result as WorkResult.Failure).outputData.getBoolean(ModelDownloadWorker.KEY_ERROR_RETRYABLE, false)).isTrue()
        }

    @Test
    fun `given http 404 error - execute download marks error as non-retryable`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            fakeDownloader.nextResult = kotlin.Result.failure(ModelDownloadException.HttpError(404))

            val result =
                ModelDownloadWorker.executeDownload(
                    url = "https://example.com/model.bin",
                    outputPath = "/models/model.bin",
                    modelDownloader = fakeDownloader,
                    isStopped = { false },
                    onProgress = {},
                )

            assertThat(result).isInstanceOf(WorkResult.Failure::class.java)
            assertThat((result as WorkResult.Failure).outputData.getBoolean(ModelDownloadWorker.KEY_ERROR_RETRYABLE, true)).isFalse()
        }

    @Test
    fun `given downloader reports progress - execute download forwards progress`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            fakeDownloader.progressToReport = listOf(0, 25, 50, 75, 100)
            val progress = mutableListOf<Int>()

            ModelDownloadWorker.executeDownload(
                url = "https://example.com/model.bin",
                outputPath = "/models/model.bin",
                modelDownloader = fakeDownloader,
                isStopped = { false },
                onProgress = { progress += it },
            )

            assertThat(progress).containsExactly(0, 25, 50, 75, 100).inOrder()
            assertThat(fakeDownloader.progressReports).containsExactly(0, 25, 50, 75, 100).inOrder()
        }

    @Test
    fun `given downloader reports detailed byte progress - execute download forwards byte progress`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            fakeDownloader.detailedProgressToReport =
                listOf(
                    Triple(10, 100_000_000L, 1_000_000_000L),
                    Triple(50, 500_000_000L, 1_000_000_000L),
                    Triple(100, 1_000_000_000L, 1_000_000_000L),
                )
            val detailedProgress = mutableListOf<Triple<Int, Long, Long>>()

            ModelDownloadWorker.executeDownload(
                url = "https://example.com/model.bin",
                outputPath = "/models/model.bin",
                modelDownloader = fakeDownloader,
                isStopped = { false },
                onProgress = { percent, bytes, total ->
                    detailedProgress += Triple(percent, bytes, total)
                },
            )

            assertThat(detailedProgress)
                .containsExactly(
                    Triple(10, 100_000_000L, 1_000_000_000L),
                    Triple(50, 500_000_000L, 1_000_000_000L),
                    Triple(100, 1_000_000_000L, 1_000_000_000L),
                ).inOrder()
        }

    @Test(expected = CancellationException::class)
    fun `given worker is stopped during progress - execute download throws cancellation`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            fakeDownloader.progressToReport = listOf(0, 25, 50)
            var stopped = false

            ModelDownloadWorker.executeDownload(
                url = "https://example.com/model.bin",
                outputPath = "/models/model.bin",
                modelDownloader = fakeDownloader,
                isStopped = { stopped },
                onProgress = {
                    if (it == 25) stopped = true
                },
            )
        }

    @Test(expected = CancellationException::class)
    fun `given downloader is cancelled - execute download throws cancellation`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            fakeDownloader.shouldFailWithCancellation = true

            ModelDownloadWorker.executeDownload(
                url = "https://example.com/model.bin",
                outputPath = "/models/model.bin",
                modelDownloader = fakeDownloader,
                isStopped = { false },
                onProgress = {},
            )
        }

    @Test
    fun `given no progress reported - execute download still returns success`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            fakeDownloader.progressToReport = emptyList()

            val result =
                ModelDownloadWorker.executeDownload(
                    url = "https://example.com/model.bin",
                    outputPath = "/models/model.bin",
                    modelDownloader = fakeDownloader,
                    isStopped = { false },
                    onProgress = { assertThat(true).isFalse() },
                )

            assertThat(result).isEqualTo(WorkResult.success())
        }

    @Test
    fun `given multiple candidate urls - execute download passes candidate urls to downloader`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            val urls = listOf("https://r2.example.com/model.bin", "https://hf.example.com/model.bin")

            val result =
                ModelDownloadWorker.executeDownload(
                    urls = urls,
                    outputPath = "/models/model.bin",
                    modelDownloader = fakeDownloader,
                    isStopped = { false },
                    onProgress = {},
                )

            assertThat(result).isEqualTo(WorkResult.success())
            assertThat(fakeDownloader.multiUrlDownloads).hasSize(1)
            assertThat(fakeDownloader.multiUrlDownloads.single().urls).isEqualTo(urls)
            assertThat(fakeDownloader.multiUrlDownloads.single().outputPath).isEqualTo("/models/model.bin")
        }

    @Test
    fun `given expectedSha256 provided - execute download forwards checksum to downloader`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            val urls = listOf("https://r2.example.com/model.bin")
            val expectedSha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"

            val result =
                ModelDownloadWorker.executeDownload(
                    urls = urls,
                    outputPath = "/models/model.bin",
                    expectedSha256 = expectedSha256,
                    modelDownloader = fakeDownloader,
                    isStopped = { false },
                    onProgress = {},
                )

            assertThat(result).isEqualTo(WorkResult.success())
            assertThat(fakeDownloader.multiUrlDownloads.single().expectedSha256).isEqualTo(expectedSha256)
        }

    @Test
    fun `isRetryable returns true for network IOException`() {
        assertThat(ModelDownloadWorker.isRetryable(IOException("connection reset"))).isTrue()
    }

    @Test
    fun `isRetryable returns false for ModelDownloadException permanent failures`() {
        assertThat(ModelDownloadWorker.isRetryable(ModelDownloadException.ChecksumMismatch("a", "b"))).isFalse()
        assertThat(ModelDownloadWorker.isRetryable(ModelDownloadException.InsufficientStorage(1, 2))).isFalse()
        assertThat(ModelDownloadWorker.isRetryable(ModelDownloadException.InstallationFailure(IOException()))).isFalse()
    }

    @Test
    fun `isRetryable returns true for HTTP 5xx and false for HTTP 4xx`() {
        assertThat(ModelDownloadWorker.isRetryable(ModelDownloadException.HttpError(500))).isTrue()
        assertThat(ModelDownloadWorker.isRetryable(ModelDownloadException.HttpError(503))).isTrue()
        assertThat(ModelDownloadWorker.isRetryable(ModelDownloadException.HttpError(404))).isFalse()
        assertThat(ModelDownloadWorker.isRetryable(ModelDownloadException.HttpError(400))).isFalse()
    }

    @Test
    fun `channel constants are properly defined`() {
        assertThat(ModelDownloadWorker.CHANNEL_ID_DOWNLOAD_PROGRESS).isEqualTo("model_download")
        assertThat(ModelDownloadWorker.CHANNEL_ID_DOWNLOAD_COMPLETE).isEqualTo("model_download_complete")
    }
}
