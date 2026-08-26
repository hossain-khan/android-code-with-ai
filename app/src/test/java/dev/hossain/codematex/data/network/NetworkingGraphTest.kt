package dev.hossain.codematex.data.network

import dev.hossain.codematex.BuildConfig
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

        assertEquals(30.seconds.inWholeMilliseconds, client.connectTimeoutMillis.toLong())
        assertEquals(30.seconds.inWholeMilliseconds, client.readTimeoutMillis.toLong())

        val loggingInterceptor = client.interceptors.filterIsInstance<HttpLoggingInterceptor>().firstOrNull()
        if (BuildConfig.DEBUG) {
            org.junit.Assert.assertNotNull(loggingInterceptor)
            assertEquals(HttpLoggingInterceptor.Level.HEADERS, loggingInterceptor?.level)
        } else {
            org.junit.Assert.assertNull(loggingInterceptor)
        }
    }

    @Test
    fun `provideJson is configured to be lenient and ignore unknown keys`() {
        val json = networkingGraph.provideJson()

        assertTrue(json.configuration.ignoreUnknownKeys)
        assertTrue(json.configuration.isLenient)
    }

    @Test
    fun `provideRetrofit creates Retrofit instance with correct baseUrl and converter`() {
        val client = networkingGraph.provideOkHttpClient()
        val json = networkingGraph.provideJson()

        val retrofit = networkingGraph.provideRetrofit(client, json)

        assertEquals("https://example.com/", retrofit.baseUrl().toString())
        assertEquals(client, retrofit.callFactory())
        assertTrue(retrofit.converterFactories().isNotEmpty())
    }
}
