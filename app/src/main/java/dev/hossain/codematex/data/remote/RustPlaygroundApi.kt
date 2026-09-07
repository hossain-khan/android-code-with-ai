package dev.hossain.codematex.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
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
    @POST("evaluate.json")
    suspend fun evaluate(
        @Body request: RustPlaygroundRequest,
    ): RustPlaygroundResponse
}
