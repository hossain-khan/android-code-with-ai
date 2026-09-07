package dev.hossain.codematex.domain.runner

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dev.hossain.codematex.data.remote.PlaygroundExecuteRequest
import dev.hossain.codematex.data.remote.PlaygroundProxyApi
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Executes code snippets via the Cloudflare Workers edge playground proxy microservice (`https://code-playground.gohk.xyz`).
 */
@ContributesBinding(AppScope::class)
class RustPlaygroundCodeRunner(
    private val api: PlaygroundProxyApi,
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
            return PlaygroundExecutionResult.NetworkError("Language '$language' is not supported by the playground runner.")
        }

        if (!isOnlineChecker()) {
            return PlaygroundExecutionResult.NetworkError(
                "Internet connection required to run code on the playground.",
            )
        }

        return try {
            val response =
                api.execute(
                    PlaygroundExecuteRequest(
                        language = language.lowercase(),
                        code = code,
                        edition = "2021",
                    ),
                )

            when (response.status) {
                "success" -> {
                    val output = response.output.ifEmpty { "Program executed successfully with no output." }
                    PlaygroundExecutionResult.Success(output)
                }

                "compilation_error", "runtime_error" -> {
                    val errorDiagnostic = response.error?.trim().orEmpty()
                    PlaygroundExecutionResult.CompilationError(
                        errorDiagnostic.ifEmpty { "Execution failed with status: ${response.status}" },
                    )
                }

                "unauthorized" -> {
                    PlaygroundExecutionResult.NetworkError(
                        "Unauthorized: Missing or invalid playground authorization token.",
                    )
                }

                "rate_limited" -> {
                    PlaygroundExecutionResult.NetworkError(
                        "Rate limited: Too many execution requests. Please wait a moment.",
                    )
                }

                "upstream_timeout" -> {
                    PlaygroundExecutionResult.NetworkError(
                        "Execution timed out. The upstream playground took too long to respond.",
                    )
                }

                "unsupported_language" -> {
                    PlaygroundExecutionResult.NetworkError(
                        response.error ?: "Language '$language' is not supported by the playground.",
                    )
                }

                else -> {
                    val message = response.error?.trim().orEmpty()
                    PlaygroundExecutionResult.NetworkError(
                        message.ifEmpty { "Execution failed with status: ${response.status}" },
                    )
                }
            }
        } catch (_: SocketTimeoutException) {
            PlaygroundExecutionResult.NetworkError(
                "Execution timed out. The playground proxy took too long to respond.",
            )
        } catch (e: retrofit2.HttpException) {
            val errorBody =
                e
                    .response()
                    ?.errorBody()
                    ?.string()
                    ?.trim()
            val errorMessage =
                if (!errorBody.isNullOrEmpty()) {
                    "Playground error (${e.code()}): $errorBody"
                } else {
                    "HTTP ${e.code()}: Unable to execute snippet on the playground proxy."
                }
            Timber.w(e, "Playground proxy execution failed: %s", errorMessage)
            PlaygroundExecutionResult.NetworkError(errorMessage)
        } catch (_: IOException) {
            PlaygroundExecutionResult.NetworkError(
                "Network error: Unable to reach the playground proxy. Please check your connection.",
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
