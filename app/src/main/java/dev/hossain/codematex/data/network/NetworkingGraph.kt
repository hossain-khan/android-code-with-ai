package dev.hossain.codematex.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.hossain.codematex.BuildConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Metro bindings for the networking layer contributed to [AppScope].
 *
 * Provides the [OkHttpClient], [Retrofit], and [Json] as singletons
 * using `@ContributesTo` so they are automatically aggregated into the app's
 * dependency graph without requiring manual wiring.
 *
 * See https://zacsweers.github.io/metro/latest/aggregation/ for more on aggregation.
 */
@ContributesTo(AppScope::class)
interface NetworkingGraph {
    /**
     * Provides a configured [OkHttpClient] with:
     * - HTTP request/response header logging (HEADERS level in debug, NONE in release to avoid buffering large payloads)
     * - 30-second connect and read timeouts
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            // Note: Do NOT use Level.BODY here. Level.BODY attempts to buffer the full response
                            // payload into an in-memory byte buffer (okio.Buffer) in RAM before logging.
                            // Downloading multi-gigabyte on-device LLM model files (e.g. 2.5GB Gemma weights)
                            // with Level.BODY will immediately exhaust the JVM heap limit and cause OutOfMemoryError.
                            level = HttpLoggingInterceptor.Level.HEADERS
                        },
                    )
                }
            }.connectTimeout(30.seconds.toJavaDuration())
            .readTimeout(30.seconds.toJavaDuration())
            .build()

    /**
     * Provides a [Json] instance configured to be lenient with unknown keys,
     * ensuring forward-compatibility as the API evolves.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    /**
     * Provides a [Retrofit] instance configured with a base URL,
     * the shared [OkHttpClient], and the kotlinx-serialization converter.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://example.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
