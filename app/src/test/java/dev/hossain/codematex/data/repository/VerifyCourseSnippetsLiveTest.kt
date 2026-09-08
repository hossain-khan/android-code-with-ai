package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.LessonBlock
import dev.hossain.codematex.data.remote.PlaygroundExecuteRequest
import dev.hossain.codematex.data.remote.PlaygroundExecuteResponse
import dev.hossain.codematex.data.repository.course.GoCourseContent
import dev.hossain.codematex.data.repository.course.KotlinCourseContent
import dev.hossain.codematex.data.repository.course.PythonCourseContent
import dev.hossain.codematex.data.repository.course.RustCourseContent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.time.Duration
import java.util.Properties

class VerifyCourseSnippetsLiveTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }

    private val client =
        OkHttpClient
            .Builder()
            .callTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(15))
            .build()

    @org.junit.Ignore(
        "Run explicitly when verifying course snippets: ./gradlew testDebugUnitTest --tests dev.hossain.codematex.data.repository.VerifyCourseSnippetsLiveTest",
    )
    @Test
    fun `verify all foundation course snippets on live edge playground`() {
        Assume.assumeTrue(
            "Skipping live snippet verification: set VERIFY_SNIPPETS_ONLINE=true to run live tests",
            System.getenv("VERIFY_SNIPPETS_ONLINE") == "true",
        )
        val token = loadToken()
        Assume.assumeTrue("Skipping live snippet verification: no auth token found", token != null)

        val courses =
            listOf(
                KotlinCourseContent.course,
                GoCourseContent.course,
                PythonCourseContent.course,
                RustCourseContent.course,
            )

        val results = mutableListOf<SnippetVerificationResult>()

        for (course in courses) {
            println("\n=== Verifying course: ${course.title} (${course.language}) ===")
            val lessons = course.chapters.flatMap { it.lessons }

            for (lesson in lessons) {
                val codeBlock = lesson.blocks.filterIsInstance<LessonBlock.Code>().firstOrNull()
                if (codeBlock == null) {
                    println("  Lesson ${lesson.id}: NO CODE BLOCK")
                    continue
                }

                val edition = if (codeBlock.language.equals("rust", ignoreCase = true)) "2021" else null
                val reqPayload =
                    PlaygroundExecuteRequest(
                        language = codeBlock.language.lowercase(),
                        code = codeBlock.code,
                        edition = edition,
                    )

                val body = json.encodeToString(PlaygroundExecuteRequest.serializer(), reqPayload)

                var attempts = 0
                var parsed: PlaygroundExecuteResponse? = null

                while (attempts < 4 && parsed == null) {
                    attempts++
                    val httpRequest =
                        Request
                            .Builder()
                            .url("https://code-playground.gohk.xyz/api/v1/execute")
                            .header("Authorization", "Bearer $token")
                            .header("Content-Type", "application/json")
                            .post(body.toRequestBody("application/json".toMediaType()))
                            .build()

                    try {
                        client.newCall(httpRequest).execute().use { response ->
                            val respBody = response.body.string()
                            val responseObj = json.decodeFromString(PlaygroundExecuteResponse.serializer(), respBody)
                            if (responseObj.status == "rate_limited" && attempts < 4) {
                                println("  ⏳ Rate limited on ${lesson.id}. Backing off for 3s (attempt $attempts)...")
                                Thread.sleep(3000)
                            } else {
                                parsed = responseObj
                            }
                        }
                    } catch (e: Exception) {
                        if (attempts < 4) {
                            println("  ⏳ Request exception on ${lesson.id}: ${e.message}. Retrying...")
                            Thread.sleep(2000)
                        } else {
                            println("  ⚠️ ${lesson.id} -> Request failed: ${e.message}")
                            results.add(
                                SnippetVerificationResult(
                                    course = course.language,
                                    lessonId = lesson.id,
                                    language = codeBlock.language,
                                    status = "request_exception",
                                    cached = false,
                                    timeMs = 0,
                                    error = e.message,
                                ),
                            )
                        }
                    }
                }

                if (parsed != null) {
                    val result =
                        SnippetVerificationResult(
                            course = course.language,
                            lessonId = lesson.id,
                            language = codeBlock.language,
                            status = parsed.status,
                            cached = parsed.cached,
                            timeMs = parsed.executionTimeMs,
                            error = parsed.error,
                        )
                    results.add(result)

                    val statusIcon = if (parsed.status == "success") "✅" else "❌"
                    val cacheInfo = if (parsed.cached) "[EDGE CACHED]" else "[ORIGIN ${parsed.executionTimeMs}ms]"
                    println("  $statusIcon ${lesson.id} -> ${parsed.status} $cacheInfo")
                    if (parsed.status != "success") {
                        println("     Error: ${parsed.error?.take(200)}")
                    }
                }

                // Small pause between snippets to stay well under rate limits
                Thread.sleep(400)
            }
        }

        println("\n================ VERIFICATION SUMMARY ================")
        val successCount = results.count { it.status == "success" }
        val failed = results.filter { it.status != "success" }
        println("Total tested: ${results.size}, Success: $successCount, Failed: ${failed.size}")

        if (failed.isNotEmpty()) {
            println("\nFailed snippets list:")
            failed.forEach {
                println("  [${it.course}] ${it.lessonId}: ${it.status} - ${it.error?.replace("\n", " ")?.take(120)}")
            }
        }
    }

    private fun loadToken(): String? {
        val envToken = System.getenv("PLAYGROUND_AUTH_TOKEN")
        if (!envToken.isNullOrBlank()) return envToken

        val localPropertiesFile =
            File("../local.properties").takeIf { it.exists() }
                ?: File("local.properties").takeIf { it.exists() }
                ?: File(System.getProperty("user.dir"), "local.properties").takeIf { it.exists() }

        if (localPropertiesFile != null && localPropertiesFile.exists()) {
            val props = Properties()
            localPropertiesFile.inputStream().use { props.load(it) }
            val token = props.getProperty("PLAYGROUND_AUTH_TOKEN")
            if (!token.isNullOrBlank()) return token
        }
        return null
    }

    private data class SnippetVerificationResult(
        val course: String,
        val lessonId: String,
        val language: String,
        val status: String,
        val cached: Boolean,
        val timeMs: Long,
        val error: String?,
    )
}
