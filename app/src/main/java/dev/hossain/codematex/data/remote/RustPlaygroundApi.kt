package dev.hossain.codematex.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Request payload for the official Rust Playground evaluation endpoint:
 * `POST https://play.rust-lang.org/evaluate.json`
 */
@Serializable
data class RustPlaygroundRequest(
    val version: String = "stable",
    val optimize: String = "0",
    val code: String,
    val edition: String = "2021",
)

/**
 * Response payload from the official Rust Playground evaluation endpoint.
 *
 * - On successful compilation: [result] contains standard output/diagnostics, and [error] is null.
 * - On compilation failure: [error] contains rustc compiler error diagnostics.
 */
@Serializable
data class RustPlaygroundResponse(
    val result: String? = null,
    val error: String? = null,
)

/**
 * Retrofit interface for interacting with the official Rust Playground API.
 */
interface RustPlaygroundApi {
    @Headers(
        "Accept: */*",
        "Origin: https://doc.rust-lang.org",
        "Referer: https://doc.rust-lang.org/",
        "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36",
    )
    @POST("evaluate.json")
    suspend fun evaluate(
        @Body request: RustPlaygroundRequest,
    ): RustPlaygroundResponse
}
