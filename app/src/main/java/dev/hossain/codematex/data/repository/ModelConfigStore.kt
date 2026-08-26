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
    val config: ModelConfig
    val configFlow: StateFlow<ModelConfig>

    fun updateConfig(newConfig: ModelConfig)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelConfigStoreImpl
    @Inject
    constructor() : ModelConfigStore {
        private val _configFlow = MutableStateFlow(ModelConfig())

        override val configFlow: StateFlow<ModelConfig> = _configFlow.asStateFlow()

        override val config: ModelConfig
            get() = _configFlow.value

        override fun updateConfig(newConfig: ModelConfig) {
            _configFlow.value = newConfig
        }
    }
