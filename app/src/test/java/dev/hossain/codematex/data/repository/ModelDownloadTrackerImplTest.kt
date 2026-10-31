package dev.hossain.codematex.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.work.ModelDownloadWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ModelDownloadTrackerImplTest {
    private lateinit var userPreferencesStore: FakeUserPreferencesStore
    private lateinit var downloadPreferences: ModelDownloadPreferences

    private var lastEnqueuedWorkName: String? = null
    private var lastExistingWorkPolicy: ExistingWorkPolicy? = null
    private var lastEnqueuedRequest: OneTimeWorkRequest? = null

    @Before
    fun setUp() {
        userPreferencesStore = FakeUserPreferencesStore(initialWifiOnlyDownload = true)
        downloadPreferences = ModelDownloadPreferencesImpl(userPreferencesStore)
        lastEnqueuedWorkName = null
        lastExistingWorkPolicy = null
        lastEnqueuedRequest = null
    }

    private suspend fun enqueueDownload(
        modelId: String,
        urls: List<String>,
        path: String,
        expectedSha256: String? = null,
        modelName: String? = null,
        preferences: ModelDownloadPreferences = downloadPreferences,
    ) {
        val request =
            ModelDownloadTrackerImpl.buildDownloadRequest(
                modelId = modelId,
                urls = urls,
                path = path,
                expectedSha256 = expectedSha256,
                modelName = modelName,
                downloadPreferences = preferences,
            )
        lastEnqueuedWorkName = modelId
        lastExistingWorkPolicy = ExistingWorkPolicy.REPLACE
        lastEnqueuedRequest = request
    }

    @Test
    fun `given wifi-only enabled when enqueueDownload called then sets UNMETERED constraint`() =
        runTest {
            userPreferencesStore.setWifiOnlyDownloadEnabled(true)

            enqueueDownload(
                modelId = "vendor/model-1",
                urls = listOf("https://example.com/model.bin"),
                path = "/path/to/model.bin",
                expectedSha256 = "sha256hash",
                modelName = "Test Model",
            )

            assertThat(lastEnqueuedWorkName).isEqualTo("vendor/model-1")
            assertThat(lastExistingWorkPolicy).isEqualTo(ExistingWorkPolicy.REPLACE)
            val request = lastEnqueuedRequest
            assertThat(request).isNotNull()
            assertThat(request!!.workSpec.constraints.requiredNetworkType).isEqualTo(NetworkType.UNMETERED)
            assertThat(request.workSpec.input.getString(ModelDownloadWorker.KEY_MODEL_ID)).isEqualTo("vendor/model-1")
            assertThat(request.workSpec.input.getString(ModelDownloadWorker.KEY_PATH)).isEqualTo("/path/to/model.bin")
            assertThat(request.workSpec.input.getString(ModelDownloadWorker.KEY_SHA256)).isEqualTo("sha256hash")
            assertThat(request.workSpec.input.getString(ModelDownloadWorker.KEY_MODEL_NAME)).isEqualTo("Test Model")
        }

    @Test
    fun `given wifi-only disabled when enqueueDownload called then sets CONNECTED constraint`() =
        runTest {
            userPreferencesStore.setWifiOnlyDownloadEnabled(false)

            enqueueDownload(
                modelId = "vendor/model-2",
                urls = listOf("https://example.com/model2.bin"),
                path = "/path/to/model2.bin",
            )

            assertThat(lastEnqueuedWorkName).isEqualTo("vendor/model-2")
            val request = lastEnqueuedRequest
            assertThat(request).isNotNull()
            assertThat(request!!.workSpec.constraints.requiredNetworkType).isEqualTo(NetworkType.CONNECTED)
        }

    @Test
    fun `given preference read error when enqueueDownload called then logs error and falls back to UNMETERED`() =
        runTest {
            val failingPreferences =
                object : ModelDownloadPreferences {
                    override val downloadOverWifiOnlyFlow: Flow<Boolean> get() = flowOf(true)

                    override suspend fun getDownloadOverWifiOnly(): Boolean = throw IOException("Disk read error")

                    override suspend fun setDownloadOverWifiOnly(enabled: Boolean) {}
                }

            enqueueDownload(
                modelId = "vendor/model-3",
                urls = listOf("https://example.com/model3.bin"),
                path = "/path/to/model3.bin",
                preferences = failingPreferences,
            )

            val request = lastEnqueuedRequest
            assertThat(request).isNotNull()
            assertThat(request!!.workSpec.constraints.requiredNetworkType).isEqualTo(NetworkType.UNMETERED)
        }

    @Test(expected = CancellationException::class)
    fun `given coroutine cancellation when reading preference then propagates CancellationException and does not enqueue`() =
        runTest {
            val cancellingPreferences =
                object : ModelDownloadPreferences {
                    override val downloadOverWifiOnlyFlow: Flow<Boolean> get() = flowOf(true)

                    override suspend fun getDownloadOverWifiOnly(): Boolean = throw CancellationException("Caller coroutine cancelled")

                    override suspend fun setDownloadOverWifiOnly(enabled: Boolean) {}
                }

            try {
                enqueueDownload(
                    modelId = "vendor/model-cancelled",
                    urls = listOf("https://example.com/cancelled.bin"),
                    path = "/path/to/cancelled.bin",
                    preferences = cancellingPreferences,
                )
            } finally {
                assertThat(lastEnqueuedRequest).isNull()
            }
        }
}
