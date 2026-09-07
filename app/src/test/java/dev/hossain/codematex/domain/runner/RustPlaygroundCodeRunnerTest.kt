package dev.hossain.codematex.domain.runner

import android.content.Context
import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.remote.RustPlaygroundApi
import dev.hossain.codematex.data.remote.RustPlaygroundRequest
import dev.hossain.codematex.data.remote.RustPlaygroundResponse
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class RustPlaygroundCodeRunnerTest {
    private val fakeApi = FakeRustPlaygroundApi()
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
    fun `given successful compilation - returns success result`() =
        runTest {
            fakeApi.responseToReturn =
                RustPlaygroundResponse(
                    result = "Hello, world!\n",
                    error = null,
                )

            val result = runner.runSnippet("fn main() { println!(\"Hello, world!\"); }", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.Success::class.java)
            val success = result as PlaygroundExecutionResult.Success
            assertThat(success.output).isEqualTo("Hello, world!\n")
            assertThat(fakeApi.lastRequest?.code).isEqualTo("fn main() { println!(\"Hello, world!\"); }")
            assertThat(fakeApi.lastRequest?.edition).isEqualTo("2021")
        }

    @Test
    fun `given compilation error - returns compilation error diagnostic`() =
        runTest {
            fakeApi.responseToReturn =
                RustPlaygroundResponse(
                    result = "Compiling playground v0.0.1...",
                    error = "error[E0308]: mismatched types",
                )

            val result = runner.runSnippet("fn main() { let x: i32 = \"bad\"; }", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.CompilationError::class.java)
            val compilationError = result as PlaygroundExecutionResult.CompilationError
            assertThat(compilationError.diagnostic).contains("error[E0308]")
        }

    @Test
    fun `given timeout exception - returns execution timed out error`() =
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
                okhttp3.ResponseBody.Companion.run {
                    "{\"error\":\"missing field `edition`\"}".toResponseBody(
                        okhttp3.MediaType.Companion.run { "application/json".toMediaType() },
                    )
                }
            fakeApi.exceptionToThrow =
                retrofit2.HttpException(retrofit2.Response.error<RustPlaygroundResponse>(400, errorResponseBody))

            val result = runner.runSnippet("fn main() {}", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.NetworkError::class.java)
            val error = result as PlaygroundExecutionResult.NetworkError
            assertThat(error.message).contains("400")
            assertThat(error.message).contains("missing field `edition`")
        }

    @Test
    fun `given io exception - returns unable to reach playground error`() =
        runTest {
            fakeApi.exceptionToThrow = IOException("Connection refused")

            val result = runner.runSnippet("fn main() {}", "rust")

            assertThat(result).isInstanceOf(PlaygroundExecutionResult.NetworkError::class.java)
            val error = result as PlaygroundExecutionResult.NetworkError
            assertThat(error.message).contains("Unable to reach the Rust playground")
        }

    private class FakeRustPlaygroundApi : RustPlaygroundApi {
        var responseToReturn: RustPlaygroundResponse = RustPlaygroundResponse()
        var exceptionToThrow: Exception? = null
        var lastRequest: RustPlaygroundRequest? = null

        override suspend fun evaluate(request: RustPlaygroundRequest): RustPlaygroundResponse {
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
