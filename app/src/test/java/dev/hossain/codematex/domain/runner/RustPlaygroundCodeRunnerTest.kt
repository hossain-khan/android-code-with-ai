package dev.hossain.codematex.domain.runner

import android.content.Context
import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.remote.PlaygroundExecuteRequest
import dev.hossain.codematex.data.remote.PlaygroundExecuteResponse
import dev.hossain.codematex.data.remote.PlaygroundProxyApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class RustPlaygroundCodeRunnerTest {
    private val fakeApi = FakePlaygroundProxyApi()
    private val runner =
        RustPlaygroundCodeRunner(
            api = fakeApi,
            context = FakeTestContext(),
        ).apply {
            isOnlineChecker = { true }
        }

    @Test
    fun `supports returns true for rust and rs regardless of case`() {
        assertThat(runner.supports("rust")).isTrue()
        assertThat(runner.supports("Rust")).isTrue()
        assertThat(runner.supports("RUST")).isTrue()
        assertThat(runner.supports("rs")).isTrue()
        assertThat(runner.supports("RS")).isTrue()
        assertThat(runner.supports("go")).isFalse()
        assertThat(runner.supports("python")).isFalse()
    }

    @Test
    fun `given unsupported language - returns network error`() =
        runTest {
            val result = runner.runSnippet("package main", "go")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.NetworkError::class.java)
            val error = result as PlaygroundExecutionResult.NetworkError
            assertThat(error.message).contains("not supported")
        }

    @Test
    fun `given offline - returns network error requesting connection`() =
        runTest {
            runner.isOnlineChecker = { false }

            val result = runner.runSnippet("fn main() {}", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.NetworkError::class.java)
            val error = result as PlaygroundExecutionResult.NetworkError
            assertThat(error.message).contains("Internet connection required")
        }

    @Test
    fun `given successful execution - returns success result`() =
        runTest {
            fakeApi.responseToReturn =
                PlaygroundExecuteResponse(
                    status = "success",
                    output = "Hello, world!\n",
                    cached = true,
                    executionTimeMs = 32,
                )

            val result = runner.runSnippet("fn main() { println!(\"Hello, world!\"); }", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.Success::class.java)
            val success = result as PlaygroundExecutionResult.Success
            assertThat(success.output).isEqualTo("Hello, world!\n")
            assertThat(fakeApi.lastRequest?.code).isEqualTo("fn main() { println!(\"Hello, world!\"); }")
            assertThat(fakeApi.lastRequest?.language).isEqualTo("rust")
            assertThat(fakeApi.lastRequest?.edition).isEqualTo("2021")
        }

    @Test
    fun `given compilation error status - returns compilation error diagnostic`() =
        runTest {
            fakeApi.responseToReturn =
                PlaygroundExecuteResponse(
                    status = "compilation_error",
                    error = "error[E0308]: mismatched types",
                )

            val result = runner.runSnippet("fn main() { let x: i32 = \"bad\"; }", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.CompilationError::class.java)
            val compilationError = result as PlaygroundExecutionResult.CompilationError
            assertThat(compilationError.diagnostic).contains("error[E0308]")
        }

    @Test
    fun `given unauthorized status - returns unauthorized error`() =
        runTest {
            fakeApi.responseToReturn =
                PlaygroundExecuteResponse(
                    status = "unauthorized",
                    error = "Missing Authorization header",
                )

            val result = runner.runSnippet("fn main() {}", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.NetworkError::class.java)
            val error = result as PlaygroundExecutionResult.NetworkError
            assertThat(error.message).contains("Unauthorized")
        }

    @Test
    fun `given rate limited status - returns rate limited error`() =
        runTest {
            fakeApi.responseToReturn =
                PlaygroundExecuteResponse(
                    status = "rate_limited",
                    error = "Rate limit exceeded",
                )

            val result = runner.runSnippet("fn main() {}", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.NetworkError::class.java)
            val error = result as PlaygroundExecutionResult.NetworkError
            assertThat(error.message).contains("Rate limited")
        }

    @Test
    fun `given upstream timeout status - returns timeout error`() =
        runTest {
            fakeApi.responseToReturn =
                PlaygroundExecuteResponse(
                    status = "upstream_timeout",
                    error = "Upstream playground failed to respond within 15 seconds.",
                )

            val result = runner.runSnippet("fn main() {}", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.NetworkError::class.java)
            val error = result as PlaygroundExecutionResult.NetworkError
            assertThat(error.message).contains("timed out")
        }

    @Test
    fun `given socket timeout exception - returns execution timed out error`() =
        runTest {
            fakeApi.exceptionToThrow = SocketTimeoutException("Read timed out")

            val result = runner.runSnippet("fn main() {}", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.NetworkError::class.java)
            val error = result as PlaygroundExecutionResult.NetworkError
            assertThat(error.message).contains("timed out")
        }

    @Test
    fun `given http exception - returns formatted playground error with code and body`() =
        runTest {
            val errorResponseBody =
                "{\"error\":\"Internal Server Error\"}".toResponseBody("application/json".toMediaType())
            fakeApi.exceptionToThrow =
                retrofit2.HttpException(retrofit2.Response.error<PlaygroundExecuteResponse>(500, errorResponseBody))

            val result = runner.runSnippet("fn main() {}", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.NetworkError::class.java)
            val error = result as PlaygroundExecutionResult.NetworkError
            assertThat(error.message).contains("500")
            assertThat(error.message).contains("Internal Server Error")
        }

    @Test
    fun `given io exception - returns unable to reach playground proxy error`() =
        runTest {
            fakeApi.exceptionToThrow = IOException("Connection refused")

            val result = runner.runSnippet("fn main() {}", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.NetworkError::class.java)
            val error = result as PlaygroundExecutionResult.NetworkError
            assertThat(error.message).contains("Unable to reach the playground proxy")
        }

    @Test
    fun `PlaygroundExecuteRequest serializes default values properly`() {
        val json =
            kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        val request = PlaygroundExecuteRequest(language = "rust", code = "fn main() {}")
        val jsonString = json.encodeToString(PlaygroundExecuteRequest.serializer(), request)

        assertThat(jsonString).contains("\"language\":\"rust\"")
        assertThat(jsonString).contains("\"code\":\"fn main() {}\"")
        assertThat(jsonString).contains("\"version\":\"stable\"")
        assertThat(jsonString).contains("\"optimize\":\"0\"")
        assertThat(jsonString).contains("\"bypassCache\":false")
    }

    private class FakePlaygroundProxyApi : PlaygroundProxyApi {
        var responseToReturn: PlaygroundExecuteResponse = PlaygroundExecuteResponse(status = "success")
        var exceptionToThrow: Exception? = null
        var lastRequest: PlaygroundExecuteRequest? = null

        override suspend fun execute(request: PlaygroundExecuteRequest): PlaygroundExecuteResponse {
            lastRequest = request
            exceptionToThrow?.let { throw it }
            return responseToReturn
        }
    }

    @Suppress("DEPRECATION")
    private class FakeTestContext : android.content.ContextWrapper(null) {
        override fun getApplicationContext(): Context = this

        override fun getSystemService(name: String): Any? = null
    }
}
