package dev.hossain.codematex.data.remote

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Request payload for the Cloudflare Workers edge code runner:
 * `POST https://code-playground.gohk.xyz/api/v1/execute`
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PlaygroundExecuteRequest(
    val language: String,
    val code: String,
    @EncodeDefault val version: String = "stable",
    val edition: String? = null,
    @EncodeDefault val optimize: String = "0",
    @EncodeDefault val args: List<String> = emptyList(),
    @EncodeDefault val bypassCache: Boolean = false,
)

/**
 * Execution response from the Cloudflare Workers edge code runner.
 *
 * - [status]: One of "success", "compilation_error", "runtime_error", "upstream_timeout",
 *   "unsupported_language", "rate_limited", "unauthorized", or "error".
 * - [output]: Standard output produced by the program.
 * - [error]: Compiler diagnostics or error description (null on success).
 * - [cached]: True if the execution was served from Cloudflare's global edge cache (<50ms).
 * - [executionTimeMs]: Time taken by the compiler/edge in milliseconds.
 */
@Serializable
data class PlaygroundExecuteResponse(
    val status: String,
    val language: String? = null,
    val version: String? = null,
    val output: String = "",
    val error: String? = null,
    val cached: Boolean = false,
    val executionTimeMs: Long = 0,
    val timestamp: Long = 0,
)

/**
 * Retrofit interface for interacting with the Cloudflare Workers playground proxy microservice.
 */
interface PlaygroundProxyApi {
    @POST("api/v1/execute")
    suspend fun execute(
        @Body request: PlaygroundExecuteRequest,
    ): PlaygroundExecuteResponse
}
