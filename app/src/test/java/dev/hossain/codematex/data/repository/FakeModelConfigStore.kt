package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.ModelConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeModelConfigStore(
    initialConfig: ModelConfig = ModelConfig(),
) : ModelConfigStore {
    private val defaultState = MutableStateFlow(initialConfig)
    private val configs = mutableMapOf<String, MutableStateFlow<ModelConfig>>()

    override val configFlow: StateFlow<ModelConfig> = defaultState.asStateFlow()
    override val config: ModelConfig get() = defaultState.value

    override fun updateConfig(newConfig: ModelConfig) {
        defaultState.value = newConfig
    }

    override fun getConfigFlow(modelId: String): Flow<ModelConfig> =
        configs.getOrPut(modelId) { MutableStateFlow(ModelConfig()) }.asStateFlow()

    override suspend fun getConfig(modelId: String): ModelConfig = configs[modelId]?.value ?: ModelConfig()

    override suspend fun setConfig(
        modelId: String,
        config: ModelConfig,
    ) {
        val flow = configs.getOrPut(modelId) { MutableStateFlow(ModelConfig()) }
        flow.value = config
    }

    override suspend fun resetConfig(modelId: String) {
        val flow = configs[modelId]
        if (flow != null) {
            flow.value = ModelConfig()
        }
        configs.remove(modelId)
    }
}
