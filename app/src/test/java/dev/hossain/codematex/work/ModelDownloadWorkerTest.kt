package dev.hossain.codematex.work

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

            assertEquals(WorkResult.success(), result)
            assertEquals(
                listOf(FakeModelDownloader.DownloadCall("https://example.com/model.bin", "/models/model.bin")),
                fakeDownloader.downloads,
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
            assertEquals(WorkResult.failure(expectedErrorData), result)
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
            assertEquals(WorkResult.failure(expectedErrorData), result)
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

            assertTrue(result is WorkResult.Failure)
            assertTrue(result.outputData.getBoolean(ModelDownloadWorker.KEY_ERROR_RETRYABLE, false))
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

            assertTrue(result is WorkResult.Failure)
            assertFalse(result.outputData.getBoolean(ModelDownloadWorker.KEY_ERROR_RETRYABLE, true))
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

            assertEquals(listOf(0, 25, 50, 75, 100), progress)
            assertEquals(listOf(0, 25, 50, 75, 100), fakeDownloader.progressReports)
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

            assertEquals(
                listOf(
                    Triple(10, 100_000_000L, 1_000_000_000L),
                    Triple(50, 500_000_000L, 1_000_000_000L),
                    Triple(100, 1_000_000_000L, 1_000_000_000L),
                ),
                detailedProgress,
            )
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
                    onProgress = { assertTrue(false) },
                )

            assertEquals(WorkResult.success(), result)
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

            assertEquals(WorkResult.success(), result)
            assertEquals(1, fakeDownloader.multiUrlDownloads.size)
            assertEquals(urls, fakeDownloader.multiUrlDownloads.single().urls)
            assertEquals("/models/model.bin", fakeDownloader.multiUrlDownloads.single().outputPath)
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

            assertEquals(WorkResult.success(), result)
            assertEquals(expectedSha256, fakeDownloader.multiUrlDownloads.single().expectedSha256)
        }

    @Test
    fun `isRetryable returns true for network IOException`() {
        assertTrue(ModelDownloadWorker.isRetryable(IOException("connection reset")))
    }

    @Test
    fun `isRetryable returns false for ModelDownloadException permanent failures`() {
        assertFalse(ModelDownloadWorker.isRetryable(ModelDownloadException.ChecksumMismatch("a", "b")))
        assertFalse(ModelDownloadWorker.isRetryable(ModelDownloadException.InsufficientStorage(1, 2)))
        assertFalse(ModelDownloadWorker.isRetryable(ModelDownloadException.InstallationFailure(IOException())))
    }

    @Test
    fun `isRetryable returns true for HTTP 5xx and false for HTTP 4xx`() {
        assertTrue(ModelDownloadWorker.isRetryable(ModelDownloadException.HttpError(500)))
        assertTrue(ModelDownloadWorker.isRetryable(ModelDownloadException.HttpError(503)))
        assertFalse(ModelDownloadWorker.isRetryable(ModelDownloadException.HttpError(404)))
        assertFalse(ModelDownloadWorker.isRetryable(ModelDownloadException.HttpError(400)))
    }
}
