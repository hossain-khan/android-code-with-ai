package dev.hossain.codematex.data.network

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.BuildConfig
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for [NetworkingGraph].
 */
class NetworkingGraphTest {
    private val networkingGraph = object : NetworkingGraph {}

    @Test
    fun `provideOkHttpClient configures expected timeouts and logging level`() {
        val client = networkingGraph.provideOkHttpClient()

        assertThat(client.connectTimeoutMillis.toLong()).isEqualTo(30.seconds.inWholeMilliseconds)
        assertThat(client.readTimeoutMillis.toLong()).isEqualTo(30.seconds.inWholeMilliseconds)

        val loggingInterceptor = client.interceptors.filterIsInstance<HttpLoggingInterceptor>().firstOrNull()
        if (BuildConfig.DEBUG) {
            assertThat(loggingInterceptor).isNotNull()
            assertThat(loggingInterceptor?.level).isEqualTo(HttpLoggingInterceptor.Level.HEADERS)
        } else {
            assertThat(loggingInterceptor).isNull()
        }
    }

    @Test
    fun `provideJson is configured to be lenient and ignore unknown keys`() {
        val json = networkingGraph.provideJson()

        assertThat(json.configuration.ignoreUnknownKeys).isTrue()
        assertThat(json.configuration.isLenient).isTrue()
    }

    @Test
    fun `provideRetrofit creates Retrofit instance with correct baseUrl and converter`() {
        val client = networkingGraph.provideOkHttpClient()
        val json = networkingGraph.provideJson()

        val retrofit = networkingGraph.provideRetrofit(client, json)

        assertThat(retrofit.baseUrl().toString()).isEqualTo("https://example.com/")
        assertThat(retrofit.callFactory()).isEqualTo(client)
        assertThat(retrofit.converterFactories()).isNotEmpty()
    }
}
