package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.runtime.LlmEngine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelRepositoryImpl
    @Inject
    constructor() : ModelRepository {
        private var selectedModel: AiModel? = null

        override fun getAvailableModels(): Flow<List<AiModel>> = flowOf(emptyList())

        override fun getSelectedModel(): AiModel? = selectedModel

        override suspend fun selectModel(model: AiModel) {
            selectedModel = model
        }

        override suspend fun downloadModel(model: AiModel) {
            // TODO: Implement via WorkManager
        }

        override suspend fun cancelDownload(model: AiModel) {
            // TODO: Implement via WorkManager
        }

        override suspend fun deleteModel(model: AiModel) {
            // TODO: Implement file deletion
        }
    }
