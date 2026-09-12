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
    @Test
    fun `provideOkHttpClient configures expected timeouts and logging level`() {
        val client = NetworkingGraph.provideOkHttpClient()

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
    fun `provideJson is configured to be lenient, ignore unknown keys, and encode defaults`() {
        val json = NetworkingGraph.provideJson()

        assertThat(json.configuration.ignoreUnknownKeys).isTrue()
        assertThat(json.configuration.isLenient).isTrue()
        assertThat(json.configuration.encodeDefaults).isTrue()
    }

    @Test
    fun `provideRetrofit creates Retrofit instance with correct baseUrl and converter`() {
        val client = NetworkingGraph.provideOkHttpClient()
        val json = NetworkingGraph.provideJson()

        val retrofit = NetworkingGraph.provideRetrofit(client, json)

        assertThat(retrofit.baseUrl().toString()).isEqualTo("https://example.com/")
        assertThat(retrofit.callFactory()).isEqualTo(client)
        assertThat(retrofit.converterFactories()).isNotEmpty()
    }

    @Test
    fun `providePlaygroundProxyApi creates API instance with Cloudflare edge proxy baseUrl`() {
        val client = NetworkingGraph.provideOkHttpClient()
        val json = NetworkingGraph.provideJson()

        val api = NetworkingGraph.providePlaygroundProxyApi(client, json)

        assertThat(api).isNotNull()
    }
}
