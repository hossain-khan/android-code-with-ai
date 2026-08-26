package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.ModelConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Manages model configuration hyperparameters (temperature, topK, topP, maxTokens).
 */
interface ModelConfigStore {
    /**
     * The current snapshot of active model hyperparameters.
     */
    val config: ModelConfig

    /**
     * Observable [StateFlow] emitting updated model hyperparameters when modified.
     */
    val configFlow: StateFlow<ModelConfig>

    /**
     * Updates the current hyperparameter configuration to [newConfig].
     */
    fun updateConfig(newConfig: ModelConfig)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelConfigStoreImpl
    @Inject
    constructor() : ModelConfigStore {
        override val configFlow: StateFlow<ModelConfig>
            field = MutableStateFlow(ModelConfig())

        override val config: ModelConfig
            get() = configFlow.value

        override fun updateConfig(newConfig: ModelConfig) {
            configFlow.value = newConfig
        }
    }
