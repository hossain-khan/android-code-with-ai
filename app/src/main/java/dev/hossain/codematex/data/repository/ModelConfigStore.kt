package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.hossain.codematex.data.model.ModelConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

/**
 * Manages model configuration hyperparameters (temperature, topK, topP, maxTokens).
 */
interface ModelConfigStore {
    /**
     * The current snapshot of default active model hyperparameters.
     */
    val config: ModelConfig

    /**
     * Observable [StateFlow] emitting updated default model hyperparameters when modified.
     */
    val configFlow: StateFlow<ModelConfig>

    /**
     * Updates the current default hyperparameter configuration to [newConfig].
     */
    fun updateConfig(newConfig: ModelConfig)

    /**
     * Observable flow of configuration for the specified [modelId].
     */
    fun getConfigFlow(modelId: String): Flow<ModelConfig>

    /**
     * Returns the current configuration for the given [modelId], or default [ModelConfig] if not explicitly configured.
     */
    suspend fun getConfig(modelId: String): ModelConfig

    /**
     * Persists the given [config] for the specified [modelId].
     */
    suspend fun setConfig(
        modelId: String,
        config: ModelConfig,
    )

    /**
     * Resets the configuration for [modelId] back to the default [ModelConfig].
     */
    suspend fun resetConfig(modelId: String)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelConfigStoreImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ModelConfigStore {
        private val defaultFallbackConfig = MutableStateFlow(ModelConfig())

        override val configFlow: StateFlow<ModelConfig> = defaultFallbackConfig.asStateFlow()

        override val config: ModelConfig
            get() = defaultFallbackConfig.value

        override fun updateConfig(newConfig: ModelConfig) {
            defaultFallbackConfig.value = newConfig
        }

        override fun getConfigFlow(modelId: String): Flow<ModelConfig> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "ModelConfigStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    val rawJson = prefs[keyForModel(modelId)]
                    if (rawJson != null) {
                        try {
                            json.decodeFromString<ModelConfig>(rawJson)
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to parse stored ModelConfig for $modelId, falling back to default")
                            ModelConfig()
                        }
                    } else {
                        ModelConfig()
                    }
                }.distinctUntilChanged()

        override suspend fun getConfig(modelId: String): ModelConfig = getConfigFlow(modelId).first()

        override suspend fun setConfig(
            modelId: String,
            config: ModelConfig,
        ) {
            val jsonString = json.encodeToString(ModelConfig.serializer(), config)
            dataStore.edit { prefs ->
                prefs[keyForModel(modelId)] = jsonString
            }
        }

        override suspend fun resetConfig(modelId: String) {
            dataStore.edit { prefs ->
                prefs.remove(keyForModel(modelId))
            }
        }

        companion object {
            private val json =
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }

            private fun keyForModel(modelId: String) = stringPreferencesKey("model_config_$modelId")
        }
    }
