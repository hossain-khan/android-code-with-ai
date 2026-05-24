package dev.hossain.codematex.circuit

import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.runtime.LlmEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeModelRepository(
    private val availableModels: List<AiModel> = emptyList(),
    private var selectedModel: AiModel? = null,
) : ModelRepository {
    var downloadCalls = mutableListOf<AiModel>()
    var cancelDownloadCalls = mutableListOf<AiModel>()
    var deleteCalls = mutableListOf<AiModel>()

    override fun getAvailableModels(): Flow<List<AiModel>> = flowOf(availableModels)

    override fun getSelectedModel(): AiModel? = selectedModel

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
