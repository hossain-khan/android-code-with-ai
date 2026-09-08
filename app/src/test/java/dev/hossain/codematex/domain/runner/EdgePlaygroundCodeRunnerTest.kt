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

class EdgePlaygroundCodeRunnerTest {
    private val fakeApi = FakePlaygroundProxyApi()
    private val runner =
        EdgePlaygroundCodeRunner(
            api = fakeApi,
            context = FakeTestContext(),
        ).apply {
            isOnlineChecker = { true }
        }

    @Test
    fun `supports returns true for all supported languages regardless of case`() {
        // Rust
        assertThat(runner.supports("rust")).isTrue()
        assertThat(runner.supports("Rust")).isTrue()
        assertThat(runner.supports("RUST")).isTrue()
        assertThat(runner.supports("rs")).isTrue()
        assertThat(runner.supports("RS")).isTrue()

        // Kotlin
        assertThat(runner.supports("kotlin")).isTrue()
        assertThat(runner.supports("Kotlin")).isTrue()
        assertThat(runner.supports("kt")).isTrue()
        assertThat(runner.supports("KT")).isTrue()

        // Go
        assertThat(runner.supports("go")).isTrue()
        assertThat(runner.supports("Go")).isTrue()
        assertThat(runner.supports("golang")).isTrue()
        assertThat(runner.supports("GOLANG")).isTrue()

        // Python
        assertThat(runner.supports("python")).isTrue()
        assertThat(runner.supports("Python")).isTrue()
        assertThat(runner.supports("py")).isTrue()
        assertThat(runner.supports("python3")).isTrue()
        assertThat(runner.supports("cpython")).isTrue()

        // TypeScript
        assertThat(runner.supports("typescript")).isTrue()
        assertThat(runner.supports("TypeScript")).isTrue()
        assertThat(runner.supports("ts")).isTrue()
        assertThat(runner.supports("TS")).isTrue()

        // Unsupported
        assertThat(runner.supports("swift")).isFalse()
        assertThat(runner.supports("java")).isFalse()
        assertThat(runner.supports("csharp")).isFalse()
    }

    @Test
    fun `given unsupported language - returns network error`() =
        runTest {
            val result = runner.runSnippet("print(\"Hello\")", "swift")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.NetworkError::class.java)
            val error = result as PlaygroundExecutionResult.NetworkError
            assertThat(error.message).contains("not supported")
        }

    @Test
    fun `given offline - returns network error requesting connection`() =
        runTest {
            runner.isOnlineChecker = { false }

            val result = runner.runSnippet("fun main() {}", "kotlin")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.NetworkError::class.java)
            val error = result as PlaygroundExecutionResult.NetworkError
            assertThat(error.message).contains("Internet connection required")
        }

    @Test
    fun `given successful execution for rust - passes edition 2021 and returns success`() =
        runTest {
            fakeApi.responseToReturn =
                PlaygroundExecuteResponse(
                    status = "success",
                    output = "Hello, Rust!\n",
                    cached = true,
                    executionTimeMs = 32,
                )

            val result = runner.runSnippet("fn main() { println!(\"Hello, Rust!\"); }", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.Success::class.java)
            val success = result as PlaygroundExecutionResult.Success
            assertThat(success.output).isEqualTo("Hello, Rust!")
            assertThat(fakeApi.lastRequest?.code).isEqualTo("fn main() { println!(\"Hello, Rust!\"); }")
            assertThat(fakeApi.lastRequest?.language).isEqualTo("rust")
            assertThat(fakeApi.lastRequest?.edition).isEqualTo("2021")
        }

    @Test
    fun `given successful execution for kotlin - passes language and returns success`() =
        runTest {
            fakeApi.responseToReturn =
                PlaygroundExecuteResponse(
                    status = "success",
                    output = "Hello, Kotlin!\n",
                    cached = true,
                    executionTimeMs = 45,
                )

            val result = runner.runSnippet("fun main() { println(\"Hello, Kotlin!\") }", "kotlin")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.Success::class.java)
            val success = result as PlaygroundExecutionResult.Success
            assertThat(success.output).isEqualTo("Hello, Kotlin!")
            assertThat(fakeApi.lastRequest?.language).isEqualTo("kotlin")
            assertThat(fakeApi.lastRequest?.edition).isNull()
        }

    @Test
    fun `given successful execution for go - passes language and returns success`() =
        runTest {
            fakeApi.responseToReturn =
                PlaygroundExecuteResponse(
                    status = "success",
                    output = "Hello, Go!\n",
                    cached = true,
                    executionTimeMs = 50,
                )

            val result = runner.runSnippet("package main\nfunc main() {}", "go")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.Success::class.java)
            val success = result as PlaygroundExecutionResult.Success
            assertThat(success.output).isEqualTo("Hello, Go!")
            assertThat(fakeApi.lastRequest?.language).isEqualTo("go")
            assertThat(fakeApi.lastRequest?.edition).isNull()
        }

    @Test
    fun `given successful execution for python - passes language and returns success`() =
        runTest {
            fakeApi.responseToReturn =
                PlaygroundExecuteResponse(
                    status = "success",
                    output = "Hello, Python!\n",
                    cached = true,
                    executionTimeMs = 20,
                )

            val result = runner.runSnippet("print(\"Hello, Python!\")", "python")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.Success::class.java)
            val success = result as PlaygroundExecutionResult.Success
            assertThat(success.output).isEqualTo("Hello, Python!")
            assertThat(fakeApi.lastRequest?.language).isEqualTo("python")
            assertThat(fakeApi.lastRequest?.edition).isNull()
        }

    @Test
    fun `given successful execution for typescript - passes language and returns success`() =
        runTest {
            fakeApi.responseToReturn =
                PlaygroundExecuteResponse(
                    status = "success",
                    output = "Hello, TypeScript!\n",
                    cached = true,
                    executionTimeMs = 25,
                )

            val result = runner.runSnippet("console.log(\"Hello, TypeScript!\");", "typescript")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.Success::class.java)
            val success = result as PlaygroundExecutionResult.Success
            assertThat(success.output).isEqualTo("Hello, TypeScript!")
            assertThat(fakeApi.lastRequest?.language).isEqualTo("typescript")
            assertThat(fakeApi.lastRequest?.edition).isNull()
        }

    @Test
    fun `given output with trailing newlines - trims trailing newlines but preserves internal newlines`() =
        runTest {
            fakeApi.responseToReturn =
                PlaygroundExecuteResponse(
                    status = "success",
                    output = "line 1\nline 2\n\n",
                    cached = false,
                    executionTimeMs = 20,
                )

            val result = runner.runSnippet("println(\"line 1\"); println(\"line 2\");", "kotlin")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.Success::class.java)
            val success = result as PlaygroundExecutionResult.Success
            assertThat(success.output).isEqualTo("line 1\nline 2")
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
                explicitNulls = false
            }
        val request = PlaygroundExecuteRequest(language = "rust", code = "fn main() {}")
        val jsonString = json.encodeToString(PlaygroundExecuteRequest.serializer(), request)

        assertThat(jsonString).contains("\"language\":\"rust\"")
        assertThat(jsonString).contains("\"code\":\"fn main() {}\"")
        assertThat(jsonString).contains("\"version\":\"stable\"")
        assertThat(jsonString).contains("\"optimize\":\"0\"")
        assertThat(jsonString).contains("\"bypassCache\":false")
        assertThat(jsonString).doesNotContain("\"edition\"")
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
