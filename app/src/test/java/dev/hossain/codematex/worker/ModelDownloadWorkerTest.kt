package dev.hossain.codematex.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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
    fun `given download fails - execute download returns failure`() =
        runTest {
            val fakeDownloader = FakeModelDownloader()
            fakeDownloader.nextResult = kotlin.Result.failure(IllegalStateException("boom"))

            val result =
                ModelDownloadWorker.executeDownload(
                    url = "https://example.com/model.bin",
                    outputPath = "/models/model.bin",
                    modelDownloader = fakeDownloader,
                    isStopped = { false },
                    onProgress = {},
                )

            assertEquals(WorkResult.failure(), result)
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
}
