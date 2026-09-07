package dev.hossain.codematex.domain.runner

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dev.hossain.codematex.data.remote.RustPlaygroundApi
import dev.hossain.codematex.data.remote.RustPlaygroundRequest
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Executes Rust code snippets via the official Rust Playground evaluation API (`https://play.rust-lang.org/evaluate.json`).
 */
@ContributesBinding(AppScope::class)
class RustPlaygroundCodeRunner(
    private val api: RustPlaygroundApi,
    @ApplicationContext private val context: Context,
) : PlaygroundCodeRunner {
    internal var isOnlineChecker: () -> Boolean = { checkNetworkOnline() }

    override fun supports(language: String): Boolean =
        language.equals("rust", ignoreCase = true) || language.equals("rs", ignoreCase = true)

    override suspend fun runSnippet(
        code: String,
        language: String,
    ): PlaygroundExecutionResult {
        if (!supports(language)) {
            return PlaygroundExecutionResult.NetworkError("Language '$language' is not supported by the Rust playground.")
        }

        if (!isOnlineChecker()) {
            return PlaygroundExecutionResult.NetworkError(
                "Internet connection required to run code on the Rust Playground.",
            )
        }

        return try {
            val response = api.evaluate(RustPlaygroundRequest(code = code))
            val error = response.error?.trim()
            if (!error.isNullOrEmpty()) {
                PlaygroundExecutionResult.CompilationError(error)
            } else {
                PlaygroundExecutionResult.Success(
                    response.result ?: "Program executed successfully with no output.",
                )
            }
        } catch (_: SocketTimeoutException) {
            PlaygroundExecutionResult.NetworkError(
                "Execution timed out. The playground took too long to respond.",
            )
        } catch (_: IOException) {
            PlaygroundExecutionResult.NetworkError(
                "Network error: Unable to reach the Rust playground. Please check your connection.",
            )
        } catch (e: Exception) {
            PlaygroundExecutionResult.NetworkError(
                e.message ?: "An unexpected error occurred while executing the snippet.",
            )
        }
    }

    private fun checkNetworkOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
