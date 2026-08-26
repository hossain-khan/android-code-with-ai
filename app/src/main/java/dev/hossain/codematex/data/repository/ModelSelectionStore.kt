package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * Abstraction over persistent storage for the currently selected model id.
 *
 * Tests can supply an in-memory fake to avoid depending on Android DataStore.
 */
interface ModelSelectionStore {
    /**
     * Observable flow of the currently selected model id.
     */
    val selectedModelIdFlow: Flow<String?>

    /**
     * Returns the currently selected model id, or `null` if none is selected.
     */
    suspend fun getSelectedModelId(): String?

    /**
     * Persists the selected model id, awaiting completion.
     */
    suspend fun setSelectedModelId(modelId: String?)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelSelectionStoreImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ModelSelectionStore {
        override val selectedModelIdFlow: Flow<String?> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "ModelSelectionStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs -> prefs[KEY_SELECTED_MODEL_ID] }
                .distinctUntilChanged()

        override suspend fun getSelectedModelId(): String? = selectedModelIdFlow.first()

        override suspend fun setSelectedModelId(modelId: String?) {
            dataStore.edit { prefs ->
                if (modelId != null) {
                    prefs[KEY_SELECTED_MODEL_ID] = modelId
                } else {
                    prefs.remove(KEY_SELECTED_MODEL_ID)
                }
            }
        }

        companion object {
            private val KEY_SELECTED_MODEL_ID = stringPreferencesKey("selected_model_id")
        }
    }
