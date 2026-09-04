package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.runtime.LlmEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class FakeModelRepository(
    availableModels: List<AiModel> = emptyList(),
    private var selectedModel: AiModel? = null,
    var getException: Exception? = null,
) : ModelRepository {
    var downloadCalls = mutableListOf<AiModel>()
    var cancelDownloadCalls = mutableListOf<AiModel>()
    var deleteCalls = mutableListOf<AiModel>()
    val modelsFlow = MutableStateFlow(availableModels)

    override fun getAvailableModels(): Flow<List<AiModel>> = getException?.let { flow { throw it } } ?: modelsFlow

    override fun getSelectedModel(): AiModel? = selectedModel

    fun emitModels(models: List<AiModel>) {
        modelsFlow.value = models
    }

    override suspend fun selectModel(model: AiModel) {
        selectedModel = model
    }

    override suspend fun downloadModel(model: AiModel) {
        downloadCalls.add(model)
    }

    override suspend fun cancelDownload(model: AiModel) {
        cancelDownloadCalls.add(model)
    }

    override suspend fun deleteModel(model: AiModel) {
        deleteCalls.add(model)
    }
}

fun testModel(
    id: String = "google/gemma-2-2b-it",
    downloadStatus: DownloadStatus = DownloadStatus.DOWNLOADED,
    localPath: String? = "/models/gemma-2-2b-it.task",
): AiModel =
    AiModel(
        id = id,
        name = id.substringAfterLast("/"),
        displayName = id.substringAfterLast("/"),
        downloadUrl = "https://huggingface.co/$id/resolve/main/${id.substringAfterLast("/")}.task",
        sizeBytes = 2_500_000_000,
        localPath = localPath,
        downloadStatus = downloadStatus,
        preferredBackend = LlmEngine.Backend.CPU,
    )
