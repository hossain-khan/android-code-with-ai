package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.AiModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository managing on-device AI model discovery, selection, downloads, and storage lifecycle.
 */
interface ModelRepository {
    /**
     * Returns an observable [Flow] of all supported AI models with their live download states.
     */
    fun getAvailableModels(): Flow<List<AiModel>>

    /**
     * Returns the currently active/selected [AiModel], or `null` if no model is selected or downloaded.
     */
    fun getSelectedModel(): AiModel?

    /**
     * Sets [model] as the active on-device model for inference sessions.
     */
    suspend fun selectModel(model: AiModel)

    /**
     * Enqueues a background download task for the given [model].
     */
    suspend fun downloadModel(model: AiModel)

    /**
     * Cancels an active or pending download for [model].
     */
    suspend fun cancelDownload(model: AiModel)

    /**
     * Deletes the local model weights file for [model] from disk and resets its download state.
     */
    suspend fun deleteModel(model: AiModel)
}
