package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.runtime.LlmEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ModelRepositoryImpl] using in-memory fakes for the Android
 * framework dependencies.
 */
class ModelRepositoryImplTest {
    private fun createRepository(
        existingPaths: Set<String> = emptySet(),
        selectedModelId: String? = null,
        allowlist: List<ModelEntry> = FakeModelAllowlistDataSource.defaultAllowlist(),
    ): Pair<ModelRepositoryImpl, TestDependencies> {
        val fileStorage = FakeModelFileStorage(existingPaths = existingPaths)
        val selectionStore = FakeModelSelectionStore(selectedModelId)
        val downloadTracker = FakeModelDownloadTracker()
        val allowlistDataSource = FakeModelAllowlistDataSource(allowlist)
        val repository = ModelRepositoryImpl(fileStorage, selectionStore, downloadTracker, allowlistDataSource)
        return repository to TestDependencies(fileStorage, selectionStore, downloadTracker, allowlistDataSource)
    }

    private data class TestDependencies(
        val fileStorage: FakeModelFileStorage,
        val selectionStore: FakeModelSelectionStore,
        val downloadTracker: FakeModelDownloadTracker,
        val allowlistDataSource: FakeModelAllowlistDataSource,
    )

    private fun testModel(
        id: String = "litert-community/gemma-4-E2B-it-litert-lm",
        status: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
        localPath: String? = null,
    ): AiModel =
        AiModel(
            id = id,
            name = id.substringAfterLast("/"),
            displayName = id.substringAfterLast("/"),
            downloadUrl = "https://example.com/$id",
            sizeBytes = 1L,
            localPath = localPath,
            downloadStatus = status,
            preferredBackend = LlmEngine.Backend.GPU,
        )

    @Test
    fun `given no models downloaded - initial scan marks all as not downloaded`() {
        val (repository, _) = createRepository()

        val model = repository.getSelectedModel()

        assertNull(model)
    }

    @Test
    fun `given a model file exists - initial scan marks it downloaded`() {
        val (repository, deps) =
            createRepository(
                existingPaths =
                    setOf(
                        "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm",
                    ),
            )

        val selected = repository.getSelectedModel()

        assertNotNull(selected)
        assertEquals("litert-community/gemma-4-E2B-it-litert-lm", selected?.id)
        assertEquals(DownloadStatus.DOWNLOADED, selected?.downloadStatus)
        assertEquals(
            "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm",
            selected?.localPath,
        )
        assertEquals(selected?.id, deps.selectionStore.selectedModelId)
    }

    @Test
    fun `getAvailableModels emits bundled allowlist with not downloaded status`() =
        runTest {
            val (repository, _) = createRepository()

            val models = repository.getAvailableModels().first()

            assertEquals(2, models.size)
            assertEquals(
                listOf(
                    "litert-community/gemma-4-E2B-it-litert-lm",
                    "litert-community/gemma-4-E4B-it-litert-lm",
                ),
                models.map { it.id },
            )
            assertTrue(models.all { it.downloadStatus == DownloadStatus.NOT_DOWNLOADED })
            assertTrue(models.all { it.localPath == null })
        }

    @Test
    fun `getAvailableModels reflects download progress from tracker`() =
        runTest {
            val (repository, deps) = createRepository()
            val modelId = "litert-community/gemma-4-E2B-it-litert-lm"

            deps.downloadTracker.emitProgress(modelId, 42)

            val models = repository.getAvailableModels().first()
            val model = models.first { it.id == modelId }

            assertEquals(DownloadStatus.DOWNLOADING, model.downloadStatus)
            assertEquals(42, model.downloadProgress)
        }

    @Test
    fun `getAvailableModels reflects failed download from tracker`() =
        runTest {
            val (repository, deps) = createRepository()
            val modelId = "litert-community/gemma-4-E4B-it-litert-lm"

            deps.downloadTracker.emitFailed(modelId)

            val models = repository.getAvailableModels().first()
            val model = models.first { it.id == modelId }

            assertEquals(DownloadStatus.FAILED, model.downloadStatus)
            assertEquals(0, model.downloadProgress)
        }

    @Test
    fun `getAvailableModels prefers downloaded file over tracker state`() =
        runTest {
            val modelId = "litert-community/gemma-4-E2B-it-litert-lm"
            val path =
                "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm"
            val (repository, deps) = createRepository(existingPaths = setOf(path))

            deps.downloadTracker.emitProgress(modelId, 42)

            val models = repository.getAvailableModels().first()
            val model = models.first { it.id == modelId }

            assertEquals(DownloadStatus.DOWNLOADED, model.downloadStatus)
            assertEquals(100, model.downloadProgress)
            assertEquals(path, model.localPath)
        }

    @Test
    fun `getSelectedModel returns saved model when downloaded`() {
        val path =
            "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm"
        val (repository, _) =
            createRepository(
                existingPaths = setOf(path),
                selectedModelId = "litert-community/gemma-4-E2B-it-litert-lm",
            )

        val selected = repository.getSelectedModel()

        assertNotNull(selected)
        assertEquals("litert-community/gemma-4-E2B-it-litert-lm", selected?.id)
    }

    @Test
    fun `getSelectedModel falls back to first downloaded when saved is not downloaded`() {
        val path =
            "/fake/models/litert-community_gemma-4-E4B-it-litert-lm/gemma-4-E4B-it.litertlm"
        val (repository, _) =
            createRepository(
                existingPaths = setOf(path),
                selectedModelId = "litert-community/gemma-4-E2B-it-litert-lm",
            )

        val selected = repository.getSelectedModel()

        assertNotNull(selected)
        assertEquals("litert-community/gemma-4-E4B-it-litert-lm", selected?.id)
    }

    @Test
    fun `getSelectedModel returns null when nothing is downloaded`() {
        val (repository, _) = createRepository()

        assertNull(repository.getSelectedModel())
    }

    @Test
    fun `selectModel updates selection store`() =
        runTest {
            val (repository, deps) = createRepository()
            val model = testModel(id = "some/model")

            repository.selectModel(model)

            assertEquals("some/model", deps.selectionStore.selectedModelId)
        }

    @Test
    fun `downloadModel enqueues work with local path when available`() =
        runTest {
            val (repository, deps) = createRepository()
            val model = testModel(localPath = "/models/custom/task")

            repository.downloadModel(model)

            assertEquals(1, deps.downloadTracker.enqueuedDownloads.size)
            val (id, url, path) = deps.downloadTracker.enqueuedDownloads.single()
            assertEquals(model.id, id)
            assertEquals(model.downloadUrl, url)
            assertEquals("/models/custom/task", path)
            assertEquals(
                model.displayName,
                deps.downloadTracker.enqueuedRequests
                    .single()
                    .modelName,
            )
        }

    @Test
    fun `downloadModel enqueues work with computed path when local path missing`() =
        runTest {
            val (repository, deps) = createRepository()
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", localPath = null)

            repository.downloadModel(model)

            val (_, _, path) = deps.downloadTracker.enqueuedDownloads.single()
            assertEquals(
                "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm",
                path,
            )
        }

    @Test
    fun `downloadModel enqueues work with fallback path for unknown model`() =
        runTest {
            val (repository, deps) = createRepository()
            val model = testModel(id = "unknown/vendor", localPath = null)

            repository.downloadModel(model)

            val (_, _, path) = deps.downloadTracker.enqueuedDownloads.single()
            assertEquals("/fake/models/unknown_vendor/vendor.litertlm", path)
        }

    @Test
    fun `cancelDownload cancels work for model`() =
        runTest {
            val (repository, deps) = createRepository()
            val model = testModel(id = "vendor/model")

            repository.cancelDownload(model)

            assertEquals(listOf("vendor/model"), deps.downloadTracker.cancelledDownloads)
        }

    @Test
    fun `deleteModel deletes file and clears selection`() =
        runTest {
            val path = "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm"
            val modelId = "litert-community/gemma-4-E2B-it-litert-lm"
            val (repository, deps) = createRepository(existingPaths = setOf(path), selectedModelId = modelId)

            repository.deleteModel(testModel(id = modelId, localPath = path))

            assertEquals(listOf(path), deps.fileStorage.deletedPaths)
            assertNull(deps.selectionStore.selectedModelId)
        }

    @Test
    fun `deleteModel does not clear selection when deleting different model`() =
        runTest {
            val path = "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm"
            val (repository, deps) =
                createRepository(
                    existingPaths = setOf(path),
                    selectedModelId = "litert-community/gemma-4-E4B-it-litert-lm",
                )

            repository.deleteModel(testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", localPath = path))

            assertEquals("litert-community/gemma-4-E4B-it-litert-lm", deps.selectionStore.selectedModelId)
        }

    @Test
    fun `deleteModel computes path when local path is missing`() =
        runTest {
            val (repository, deps) = createRepository()
            val modelId = "litert-community/gemma-4-E2B-it-litert-lm"

            repository.deleteModel(testModel(id = modelId, localPath = null))

            assertEquals(
                listOf(
                    "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm",
                ),
                deps.fileStorage.deletedPaths,
            )
        }

    @Test
    fun `getAvailableModels populates metadata fields from allowlist`() =
        runTest {
            val (repository, _) = createRepository()

            val models = repository.getAvailableModels().first()
            val model = models.first { it.id == "litert-community/gemma-4-E2B-it-litert-lm" }

            assertEquals("gemma-4-E2B-it-litert-lm", model.name)
            assertEquals("gemma-4-E2B-it-litert-lm", model.displayName)
            assertEquals(2_588_147_712L, model.sizeBytes)
            assertEquals(8, model.minDeviceMemoryInGb)
            assertEquals("Google LiteRT Community", model.publisher)
            assertEquals("Apache 2.0", model.license)
            assertEquals("https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm", model.modelRepoUrl)
            assertTrue(model.description.isNotBlank())
            assertTrue(model.downloadUrl.contains("light-llm-storage.gohk.xyz"))
            assertTrue(model.downloadUrl.contains("gemma-4-E2B-it.litertlm"))
            assertTrue(model.fallbackDownloadUrls.single().contains("huggingface.co"))
            assertEquals("181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c", model.sha256)
        }

    @Test
    fun `getAvailableModels caches last emitted models for selection fallback`() =
        runTest {
            val path =
                "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm"
            val (repository, _) = createRepository(existingPaths = setOf(path))

            repository.getAvailableModels().first()
            val selected = repository.getSelectedModel()

            assertNotNull(selected)
            assertEquals("litert-community/gemma-4-E2B-it-litert-lm", selected?.id)
        }

    @Test
    fun `getAvailableModels uses injected allowlist data source`() =
        runTest {
            val customEntry =
                ModelEntry(
                    modelId = "custom/model",
                    modelFile = "custom.litertlm",
                    commitHash = "abc123",
                    sizeInBytes = 1_000,
                    taskTypes = listOf("llm_chat"),
                    runtimeType = "LITERT_LM",
                )
            val (repository, _) = createRepository(allowlist = listOf(customEntry))

            val models = repository.getAvailableModels().first()

            assertEquals(1, models.size)
            assertEquals("custom/model", models.single().id)
            assertEquals(
                "custom.litertlm",
                models
                    .single()
                    .downloadUrl
                    .substringAfterLast("/")
                    .substringBefore("?"),
            )
        }

    @Test
    fun `getAvailableModels returns empty list when allowlist is empty`() =
        runTest {
            val (repository, _) = createRepository(allowlist = emptyList())

            val models = repository.getAvailableModels().first()

            assertTrue(models.isEmpty())
        }

    @Test
    fun `getAvailableModels populates downloadErrorMessage when WorkInfo fails with error data`() =
        runTest {
            val (repository, deps) = createRepository()
            val modelId = "litert-community/gemma-4-E2B-it-litert-lm"
            deps.downloadTracker.emitFailed(modelId, "SHA-256 checksum mismatch: expected abc, calculated xyz")

            val models = repository.getAvailableModels().first()
            val failedModel = models.first { it.id == modelId }

            assertEquals(DownloadStatus.FAILED, failedModel.downloadStatus)
            assertEquals("SHA-256 checksum mismatch: expected abc, calculated xyz", failedModel.downloadErrorMessage)
        }
}
