package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.AiModel
import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    fun getAvailableModels(): Flow<List<AiModel>>

    fun getSelectedModel(): AiModel?

    suspend fun selectModel(model: AiModel)

    suspend fun downloadModel(model: AiModel)

    suspend fun cancelDownload(model: AiModel)

    suspend fun deleteModel(model: AiModel)
}
