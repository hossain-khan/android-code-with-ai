package dev.hossain.codematex.data.repository

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.runtime.LlmEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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

        assertThat(model).isNull()
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

        assertThat(selected).isNotNull()
        assertThat(selected?.id).isEqualTo("litert-community/gemma-4-E2B-it-litert-lm")
        assertThat(selected?.downloadStatus).isEqualTo(DownloadStatus.DOWNLOADED)
        assertThat(selected?.localPath).isEqualTo(
            "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm",
        )
    }

    @Test
    fun `getAvailableModels emits bundled allowlist with not downloaded status`() =
        runTest {
            val (repository, _) = createRepository()

            val models = repository.getAvailableModels().first()

            assertThat(models).hasSize(5)
            assertThat(models.map { it.id })
                .containsExactly(
                    "litert-community/gemma-4-E2B-it-litert-lm",
                    "litert-community/gemma-4-E4B-it-litert-lm",
                    "litert-community/Phi-4-mini-instruct",
                    "litert-community/Qwen2.5-Coder-1.5B-Instruct",
                    "litert-community/Qwen3-0.6B",
                ).inOrder()
            assertThat(models.all { it.downloadStatus == DownloadStatus.NOT_DOWNLOADED }).isTrue()
            assertThat(models.all { it.localPath == null }).isTrue()
        }

    @Test
    fun `getAvailableModels reflects download progress from tracker`() =
        runTest {
            val (repository, deps) = createRepository()
            val modelId = "litert-community/gemma-4-E2B-it-litert-lm"

            deps.downloadTracker.emitProgress(modelId, 42)

            val models = repository.getAvailableModels().first()
            val model = models.first { it.id == modelId }

            assertThat(model.downloadStatus).isEqualTo(DownloadStatus.DOWNLOADING)
            assertThat(model.downloadProgress).isEqualTo(42)
        }

    @Test
    fun `getAvailableModels reflects failed download from tracker`() =
        runTest {
            val (repository, deps) = createRepository()
            val modelId = "litert-community/gemma-4-E4B-it-litert-lm"

            deps.downloadTracker.emitFailed(modelId)

            val models = repository.getAvailableModels().first()
            val model = models.first { it.id == modelId }

            assertThat(model.downloadStatus).isEqualTo(DownloadStatus.FAILED)
            assertThat(model.downloadProgress).isEqualTo(0)
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

            assertThat(model.downloadStatus).isEqualTo(DownloadStatus.DOWNLOADED)
            assertThat(model.downloadProgress).isEqualTo(100)
            assertThat(model.localPath).isEqualTo(path)
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

        assertThat(selected).isNotNull()
        assertThat(selected?.id).isEqualTo("litert-community/gemma-4-E2B-it-litert-lm")
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

        assertThat(selected).isNotNull()
        assertThat(selected?.id).isEqualTo("litert-community/gemma-4-E4B-it-litert-lm")
    }

    @Test
    fun `getSelectedModel returns null when nothing is downloaded`() {
        val (repository, _) = createRepository()

        assertThat(repository.getSelectedModel()).isNull()
    }

    @Test
    fun `selectModel updates selection store`() =
        runTest {
            val (repository, deps) = createRepository()
            val model = testModel(id = "some/model")

            repository.selectModel(model)

            assertThat(deps.selectionStore.getSelectedModelId()).isEqualTo("some/model")
        }

    @Test
    fun `getAvailableModels marks selected model with isSelected true`() =
        runTest {
            val path =
                "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm"
            val (repository, _) =
                createRepository(
                    existingPaths = setOf(path),
                    selectedModelId = "litert-community/gemma-4-E2B-it-litert-lm",
                )

            val models = repository.getAvailableModels().first()
            val selectedModel = models.first { it.id == "litert-community/gemma-4-E2B-it-litert-lm" }
            val otherModel = models.first { it.id == "litert-community/gemma-4-E4B-it-litert-lm" }

            assertThat(selectedModel.isSelected).isTrue()
            assertThat(otherModel.isSelected).isFalse()
        }

    @Test
    fun `downloadModel enqueues work with local path when available`() =
        runTest {
            val (repository, deps) = createRepository()
            val model = testModel(localPath = "/models/custom/task")

            repository.downloadModel(model)

            assertThat(deps.downloadTracker.enqueuedDownloads).hasSize(1)
            val (id, url, path) = deps.downloadTracker.enqueuedDownloads.single()
            assertThat(id).isEqualTo(model.id)
            assertThat(url).isEqualTo(model.downloadUrl)
            assertThat(path).isEqualTo("/models/custom/task")
            assertThat(
                deps.downloadTracker.enqueuedRequests
                    .single()
                    .modelName,
            ).isEqualTo(model.displayName)
        }

    @Test
    fun `downloadModel enqueues work with computed path when local path missing`() =
        runTest {
            val (repository, deps) = createRepository()
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", localPath = null)

            repository.downloadModel(model)

            val (_, _, path) = deps.downloadTracker.enqueuedDownloads.single()
            assertThat(path).isEqualTo(
                "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm",
            )
        }

    @Test
    fun `downloadModel enqueues work with fallback path for unknown model`() =
        runTest {
            val (repository, deps) = createRepository()
            val model = testModel(id = "unknown/vendor", localPath = null)

            repository.downloadModel(model)

            val (_, _, path) = deps.downloadTracker.enqueuedDownloads.single()
            assertThat(path).isEqualTo("/fake/models/unknown_vendor/vendor.litertlm")
        }

    @Test
    fun `cancelDownload cancels work for model`() =
        runTest {
            val (repository, deps) = createRepository()
            val model = testModel(id = "vendor/model")

            repository.cancelDownload(model)

            assertThat(deps.downloadTracker.cancelledDownloads).containsExactly("vendor/model")
        }

    @Test
    fun `deleteModel deletes file and clears selection`() =
        runTest {
            val path = "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm"
            val modelId = "litert-community/gemma-4-E2B-it-litert-lm"
            val (repository, deps) = createRepository(existingPaths = setOf(path), selectedModelId = modelId)

            repository.deleteModel(testModel(id = modelId, localPath = path))

            assertThat(deps.fileStorage.deletedPaths).containsExactly(path)
            assertThat(deps.selectionStore.getSelectedModelId()).isNull()
        }

    @Test
    fun `deleteModel updates getAvailableModels flow with not downloaded status`() =
        runTest {
            val path = "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm"
            val modelId = "litert-community/gemma-4-E2B-it-litert-lm"
            val (repository, _) = createRepository(existingPaths = setOf(path), selectedModelId = modelId)

            val initialModels = repository.getAvailableModels().first()
            assertThat(initialModels.first { it.id == modelId }.downloadStatus).isEqualTo(DownloadStatus.DOWNLOADED)

            repository.deleteModel(testModel(id = modelId, localPath = path))

            val updatedModels = repository.getAvailableModels().first()
            assertThat(updatedModels.first { it.id == modelId }.downloadStatus).isEqualTo(DownloadStatus.NOT_DOWNLOADED)
            assertThat(updatedModels.first { it.id == modelId }.isSelected).isFalse()
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

            assertThat(deps.selectionStore.getSelectedModelId()).isEqualTo("litert-community/gemma-4-E4B-it-litert-lm")
        }

    @Test
    fun `selectModel awaits selectionStore update and is immediately observable`() =
        runTest {
            val path = "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm"
            val modelId = "litert-community/gemma-4-E2B-it-litert-lm"
            val (repository, deps) = createRepository(existingPaths = setOf(path))

            val modelToSelect = testModel(id = modelId, localPath = path, status = DownloadStatus.DOWNLOADED)
            repository.selectModel(modelToSelect)

            assertThat(deps.selectionStore.getSelectedModelId()).isEqualTo(modelId)
        }

    @Test
    fun `deleteModel computes path when local path is missing`() =
        runTest {
            val (repository, deps) = createRepository()
            val modelId = "litert-community/gemma-4-E2B-it-litert-lm"

            repository.deleteModel(testModel(id = modelId, localPath = null))

            assertThat(deps.fileStorage.deletedPaths).containsExactly(
                "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm",
            )
        }

    @Test
    fun `getAvailableModels populates metadata fields from allowlist`() =
        runTest {
            val (repository, _) = createRepository()

            val models = repository.getAvailableModels().first()
            val model = models.first { it.id == "litert-community/gemma-4-E2B-it-litert-lm" }

            assertThat(model.name).isEqualTo("gemma-4-E2B-it-litert-lm")
            assertThat(model.displayName).isEqualTo("Gemma 4-E2B IT")
            assertThat(model.sizeBytes).isEqualTo(2_588_147_712L)
            assertThat(model.minDeviceMemoryInGb).isEqualTo(8)
            assertThat(model.contextWindow).isEqualTo(8192)
            assertThat(model.quantization).isEqualTo("INT4")
            assertThat(model.promptFormat).isEqualTo("GEMMA")
            assertThat(model.publisher).isEqualTo("Google LiteRT Community")
            assertThat(model.license).isEqualTo("Apache 2.0")
            assertThat(model.modelRepoUrl).isEqualTo("https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm")
            assertThat(model.description).isNotEmpty()
            assertThat(model.downloadUrl).contains("light-llm-storage.gohk.xyz")
            assertThat(model.downloadUrl).contains("gemma-4-E2B-it.litertlm")
            assertThat(model.fallbackDownloadUrls.single()).contains("huggingface.co")
            assertThat(model.sha256).isEqualTo("181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c")
        }

    @Test
    fun `getAvailableModels caches last emitted models for selection fallback`() =
        runTest {
            val path =
                "/fake/models/litert-community_gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm"
            val (repository, _) = createRepository(existingPaths = setOf(path))

            repository.getAvailableModels().first()
            val selected = repository.getSelectedModel()

            assertThat(selected).isNotNull()
            assertThat(selected?.id).isEqualTo("litert-community/gemma-4-E2B-it-litert-lm")
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

            assertThat(models).hasSize(1)
            assertThat(models.single().id).isEqualTo("custom/model")
            assertThat(
                models
                    .single()
                    .downloadUrl
                    .substringAfterLast("/")
                    .substringBefore("?"),
            ).isEqualTo("custom.litertlm")
        }

    @Test
    fun `getAvailableModels returns empty list when allowlist is empty`() =
        runTest {
            val (repository, _) = createRepository(allowlist = emptyList())

            val models = repository.getAvailableModels().first()

            assertThat(models).isEmpty()
        }

    @Test
    fun `getAvailableModels populates downloadErrorMessage when WorkInfo fails with error data`() =
        runTest {
            val (repository, deps) = createRepository()
            val modelId = "litert-community/gemma-4-E2B-it-litert-lm"
            deps.downloadTracker.emitFailed(modelId, "SHA-256 checksum mismatch: expected abc, calculated xyz")

            val models = repository.getAvailableModels().first()
            val failedModel = models.first { it.id == modelId }

            assertThat(failedModel.downloadStatus).isEqualTo(DownloadStatus.FAILED)
            assertThat(failedModel.downloadErrorMessage).isEqualTo("SHA-256 checksum mismatch: expected abc, calculated xyz")
        }
}
