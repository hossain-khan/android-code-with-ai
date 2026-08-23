package dev.hossain.codematex.worker

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.createTempDirectory

class HttpModelDownloaderTest {
    private lateinit var server: HttpServer
    private lateinit var outputDir: File
    private var port: Int = 0

    @Before
    fun setUp() {
        outputDir = createTempDirectory("http-downloader-test").toFile()
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.start()
        port = server.address.port
    }

    @After
    fun tearDown() {
        server.stop(0)
        outputDir.deleteRecursively()
    }

    private fun serverUrl(path: String = "/model.bin"): String = "http://localhost:$port$path"

    @Test
    fun `given server returns file - download writes file to output path`() =
        runTest {
            val content = ByteArray(1_000) { it.toByte() }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = HttpModelDownloader()

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertEquals(Result.success(Unit), result)
            assertTrue(File(outputPath).exists())
            assertEquals(content.size.toLong(), File(outputPath).length())
            assertTrue(content.contentEquals(File(outputPath).readBytes()))
        }

    @Test
    fun `given partial file exists - download resumes from partial position`() =
        runTest {
            val content = ByteArray(1_000) { it.toByte() }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val tmpFile = File("$outputPath.codematextmp")
            val partialContent = content.copyOfRange(0, 400)
            tmpFile.parentFile?.mkdirs()
            tmpFile.writeBytes(partialContent)

            val downloader = HttpModelDownloader()

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertEquals(Result.success(Unit), result)
            assertTrue(File(outputPath).exists())
            assertEquals(content.size.toLong(), File(outputPath).length())
            assertTrue(content.contentEquals(File(outputPath).readBytes()))
        }

    @Test
    fun `given server returns error - download returns failure`() =
        runTest {
            server.createContext("/model.bin") { exchange ->
                exchange.sendResponseHeaders(500, 0)
                exchange.close()
            }

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = HttpModelDownloader()

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("HTTP 500") == true)
        }

    @Test
    fun `given download is cancelled - download throws cancellation`() =
        runTest {
            val content = ByteArray(10_000_000) { it.toByte() }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = HttpModelDownloader()
            val cancelled = AtomicBoolean(false)

            try {
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    onProgress = {
                        if (it > 0) {
                            cancelled.set(true)
                        }
                    },
                    shouldCancel = { cancelled.get() },
                )
                throw AssertionError("Expected CancellationException")
            } catch (e: CancellationException) {
                assertEquals("Download cancelled", e.message)
            }
        }

    @Test
    fun `given known content length - download reports progress`() =
        runTest {
            val content = ByteArray(100_000) { it.toByte() }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = HttpModelDownloader()
            val progressReports = mutableListOf<Int>()

            downloader.download(
                url = serverUrl(),
                outputPath = outputPath,
                onProgress = { progressReports += it },
                shouldCancel = { false },
            )

            assertTrue(progressReports.isNotEmpty())
            assertTrue(progressReports.last() == 100)
        }

    @Test
    fun `given primary url fails - fallback url succeeds and writes full file`() =
        runTest {
            val content = ByteArray(1_000) { it.toByte() }
            server.createContext("/bad-primary.bin") { exchange ->
                exchange.sendResponseHeaders(500, 0)
                exchange.close()
            }
            server.createContext("/good-fallback.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = HttpModelDownloader()

            val result =
                downloader.download(
                    urls = listOf(serverUrl("/bad-primary.bin"), serverUrl("/good-fallback.bin")),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertEquals(Result.success(Unit), result)
            assertTrue(File(outputPath).exists())
            assertEquals(content.size.toLong(), File(outputPath).length())
            assertTrue(content.contentEquals(File(outputPath).readBytes()))
        }

    @Test
    fun `given all candidate urls fail - download returns failure`() =
        runTest {
            server.createContext("/bad1.bin") { exchange ->
                exchange.sendResponseHeaders(500, 0)
                exchange.close()
            }
            server.createContext("/bad2.bin") { exchange ->
                exchange.sendResponseHeaders(404, 0)
                exchange.close()
            }

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = HttpModelDownloader()

            val result =
                downloader.download(
                    urls = listOf(serverUrl("/bad1.bin"), serverUrl("/bad2.bin")),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertTrue(result.isFailure)
        }

    @Test
    fun `given empty urls list - download returns failure`() =
        runTest {
            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = HttpModelDownloader()

            val result =
                downloader.download(
                    urls = emptyList(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }

    private class ModelFileHandler(
        private val content: ByteArray,
    ) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val rangeHeader = exchange.requestHeaders.getFirst("Range")
            val start =
                rangeHeader
                    ?.let {
                        val match = Regex("""bytes=(\d+)-""").find(it)
                        match?.groupValues?.get(1)?.toLongOrNull()
                    } ?: 0L

            val end = content.size.toLong()
            val status = if (rangeHeader != null) 206 else 200
            val responseLength = end - start

            exchange.sendResponseHeaders(status, responseLength)
            exchange.responseBody.use { output ->
                output.write(content, start.toInt(), responseLength.toInt())
            }
        }
    }
}
