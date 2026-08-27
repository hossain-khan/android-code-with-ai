package dev.hossain.codematex.work

import com.google.common.truth.Truth.assertThat
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.security.MessageDigest
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

    private fun createDownloader(): HttpModelDownloader = HttpModelDownloader(OkHttpClient())

    @Test
    fun `given server returns file - download writes file to output path`() =
        runTest {
            val content = ByteArray(1_000) { it.toByte() }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result).isEqualTo(Result.success(Unit))
            assertThat(File(outputPath).exists()).isTrue()
            assertThat(File(outputPath).length()).isEqualTo(content.size.toLong())
            assertThat(File(outputPath).readBytes()).isEqualTo(content)
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

            val downloader = createDownloader()

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result).isEqualTo(Result.success(Unit))
            assertThat(File(outputPath).exists()).isTrue()
            assertThat(File(outputPath).length()).isEqualTo(content.size.toLong())
            assertThat(File(outputPath).readBytes()).isEqualTo(content)
        }

    @Test
    fun `given partial file exists but server returns 200 OK - download truncates and writes full file without appending`() =
        runTest {
            val content = ByteArray(1_000) { it.toByte() }
            // Server always returns 200 OK with full content, ignoring Range request header
            server.createContext("/model.bin") { exchange ->
                exchange.sendResponseHeaders(200, content.size.toLong())
                exchange.responseBody.use { output ->
                    output.write(content)
                }
            }

            val outputPath = File(outputDir, "model.bin").absolutePath
            val tmpFile = File("$outputPath.codematextmp")
            val stalePartialContent = ByteArray(400) { 0xFF.toByte() }
            tmpFile.parentFile?.mkdirs()
            tmpFile.writeBytes(stalePartialContent)

            val downloader = createDownloader()

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result).isEqualTo(Result.success(Unit))
            assertThat(File(outputPath).exists()).isTrue()
            // Must be exactly 1000 bytes, not 1400 bytes!
            assertThat(File(outputPath).length()).isEqualTo(1_000L)
            assertThat(File(outputPath).readBytes()).isEqualTo(content)
        }

    @Test
    fun `given server returns error - download returns failure`() =
        runTest {
            server.createContext("/model.bin") { exchange ->
                exchange.sendResponseHeaders(500, 0)
                exchange.close()
            }

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result.isFailure).isTrue()
            val error = result.exceptionOrNull()
            assertThat(error).isInstanceOf(ModelDownloadException.HttpError::class.java)
            assertThat((error as ModelDownloadException.HttpError).responseCode).isEqualTo(500)
            assertThat(error.isRetryable).isTrue()
        }

    @Test
    fun `given download is cancelled - download throws cancellation`() =
        runTest {
            val content = ByteArray(10_000_000) { it.toByte() }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()
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
                assertThat(e.message).isEqualTo("Download cancelled")
            }
        }

    @Test
    fun `given known content length - download reports progress`() =
        runTest {
            val content = ByteArray(100_000) { it.toByte() }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()
            val progressReports = mutableListOf<Int>()

            downloader.download(
                url = serverUrl(),
                outputPath = outputPath,
                onProgress = { progressReports += it },
                shouldCancel = { false },
            )

            assertThat(progressReports).isNotEmpty()
            assertThat(progressReports.last()).isEqualTo(100)
        }

    @Test
    fun `given known content length - download reports bytes and total bytes`() =
        runTest {
            val content = ByteArray(100_000) { it.toByte() }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()
            val byteReports = mutableListOf<Pair<Long, Long>>()

            downloader.download(
                url = serverUrl(),
                outputPath = outputPath,
                onProgress = { _, bytesDownloaded, totalBytes ->
                    byteReports += Pair(bytesDownloaded, totalBytes)
                },
                shouldCancel = { false },
            )

            assertThat(byteReports).isNotEmpty()
            assertThat(byteReports.last().second).isEqualTo(100_000L)
            assertThat(byteReports.last().first).isEqualTo(100_000L)
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
            val downloader = createDownloader()

            val result =
                downloader.download(
                    urls = listOf(serverUrl("/bad-primary.bin"), serverUrl("/good-fallback.bin")),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result).isEqualTo(Result.success(Unit))
            assertThat(File(outputPath).exists()).isTrue()
            assertThat(File(outputPath).length()).isEqualTo(content.size.toLong())
            assertThat(File(outputPath).readBytes()).isEqualTo(content)
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
            val downloader = createDownloader()

            val result =
                downloader.download(
                    urls = listOf(serverUrl("/bad1.bin"), serverUrl("/bad2.bin")),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result.isFailure).isTrue()
        }

    @Test
    fun `given empty urls list - download returns failure`() =
        runTest {
            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()

            val result =
                downloader.download(
                    urls = emptyList(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        }

    @Test
    fun `given matching sha256 checksum - download succeeds and creates final file`() =
        runTest {
            val content = "valid model content".toByteArray()
            val expectedSha256 =
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(content)
                    .joinToString("") { "%02x".format(it) }

            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    expectedSha256 = expectedSha256,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result).isEqualTo(Result.success(Unit))
            assertThat(File(outputPath).exists()).isTrue()
            assertThat(File(outputPath).length()).isEqualTo(content.size.toLong())
            assertThat(File("$outputPath.codematextmp").exists()).isFalse()
        }

    @Test
    fun `given mismatching sha256 checksum - download fails and deletes temp file`() =
        runTest {
            val content = "corrupt model content".toByteArray()
            val wrongSha256 = "0000000000000000000000000000000000000000000000000000000000000000"

            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    expectedSha256 = wrongSha256,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result.isFailure).isTrue()
            val error = result.exceptionOrNull()
            assertThat(error).isInstanceOf(ModelDownloadException.ChecksumMismatch::class.java)
            assertThat((error as ModelDownloadException).isRetryable).isFalse()
            assertThat(File(outputPath).exists()).isFalse()
            assertThat(File("$outputPath.codematextmp").exists()).isFalse()
        }

    @Test
    fun `given primary url has checksum mismatch - fallback url with valid checksum succeeds`() =
        runTest {
            val badContent = "bad content from primary mirror".toByteArray()
            val goodContent = "good verified content from fallback mirror".toByteArray()
            val expectedSha256 =
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(goodContent)
                    .joinToString("") { "%02x".format(it) }

            server.createContext("/primary.bin", ModelFileHandler(badContent))
            server.createContext("/fallback.bin", ModelFileHandler(goodContent))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()

            val result =
                downloader.download(
                    urls = listOf(serverUrl("/primary.bin"), serverUrl("/fallback.bin")),
                    outputPath = outputPath,
                    expectedSha256 = expectedSha256,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result).isEqualTo(Result.success(Unit))
            assertThat(File(outputPath).exists()).isTrue()
            assertThat(File(outputPath).length()).isEqualTo(goodContent.size.toLong())
            assertThat(File(outputPath).readBytes()).isEqualTo(goodContent)
            assertThat(File("$outputPath.codematextmp").exists()).isFalse()
        }

    @Test
    fun `given insufficient disk space - download fails immediately with IOException`() =
        runTest {
            val content = ByteArray(10_000) { it.toByte() }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()
            // Simulate only 1,000 bytes available when 10,000 bytes are required
            downloader.spaceChecker = { 1_000L }

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result.isFailure).isTrue()
            val error = result.exceptionOrNull()
            assertThat(error).isInstanceOf(ModelDownloadException.InsufficientStorage::class.java)
            assertThat((error as ModelDownloadException).isRetryable).isFalse()
            assertThat(File(outputPath).exists()).isFalse()
        }

    @Test
    fun `given final move fails - download fails and cleans up temp file`() =
        runTest {
            val content = ByteArray(1_000) { it.toByte() }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()
            downloader.fileMover = { _, _ -> throw IOException("Forced move failure") }

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result.isFailure).isTrue()
            val error = result.exceptionOrNull()
            assertThat(error).isInstanceOf(ModelDownloadException.InstallationFailure::class.java)
            assertThat((error as ModelDownloadException).isRetryable).isFalse()
            assertThat(File(outputPath).exists()).isFalse()
            assertThat(File("$outputPath.codematextmp").exists()).isFalse()
        }

    @Test
    fun `given destination already exists - atomic install replaces it`() =
        runTest {
            val content = ByteArray(1_000) { it.toByte() }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val existingFile = File(outputPath)
            existingFile.parentFile?.mkdirs()
            existingFile.writeBytes(ByteArray(100) { 0xFF.toByte() })

            val downloader = createDownloader()

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result).isEqualTo(Result.success(Unit))
            assertThat(existingFile.exists()).isTrue()
            assertThat(existingFile.length()).isEqualTo(content.size.toLong())
            assertThat(existingFile.readBytes()).isEqualTo(content)
            assertThat(File("$outputPath.codematextmp").exists()).isFalse()
        }

    @Test
    fun `given destination size does not match - download fails and removes destination`() =
        runTest {
            val content = ByteArray(1_000) { it.toByte() }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()
            downloader.fileMover = { tmpFile, destination ->
                destination.parentFile?.mkdirs()
                destination.writeBytes(ByteArray(100) { 0xFF.toByte() })
                tmpFile.delete()
            }

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(ModelDownloadException.InstallationFailure::class.java)
            assertThat(File(outputPath).exists()).isFalse()
        }

    @Test
    fun `given destination hash does not match - download fails and removes destination`() =
        runTest {
            val content = ByteArray(1_000) { it.toByte() }
            val expectedSha256 =
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(content)
                    .joinToString("") { "%02x".format(it) }
            server.createContext("/model.bin", ModelFileHandler(content))

            val outputPath = File(outputDir, "model.bin").absolutePath
            val downloader = createDownloader()
            // Move the correct bytes, but then corrupt the destination before verification.
            downloader.fileMover = { tmpFile, destination ->
                destination.parentFile?.mkdirs()
                destination.writeBytes(content)
                destination.writeBytes(ByteArray(1) { 0xFF.toByte() })
                tmpFile.delete()
            }

            val result =
                downloader.download(
                    url = serverUrl(),
                    outputPath = outputPath,
                    expectedSha256 = expectedSha256,
                    onProgress = {},
                    shouldCancel = { false },
                )

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(ModelDownloadException.InstallationFailure::class.java)
            assertThat(File(outputPath).exists()).isFalse()
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
