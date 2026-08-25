package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Abstraction over persistent storage for the currently selected model id.
 *
 * Tests can supply an in-memory fake to avoid depending on Android DataStore.
 */
interface ModelSelectionStore {
    /**
     * The id of the currently selected model, or `null` if none is selected.
     */
    var selectedModelId: String?

    /**
     * Observable flow of the currently selected model id.
     */
    val selectedModelIdFlow: Flow<String?>
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelSelectionStoreImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ModelSelectionStore {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val modelIdState: StateFlow<String?> =
            dataStore.data
                .map { prefs -> prefs[KEY_SELECTED_MODEL_ID] }
                .stateIn(
                    scope = scope,
                    started = SharingStarted.Eagerly,
                    initialValue = null,
                )

        override val selectedModelIdFlow: Flow<String?>
            get() = modelIdState

        override var selectedModelId: String?
            get() = modelIdState.value
            set(value) {
                scope.launch {
                    dataStore.edit { prefs ->
                        if (value != null) {
                            prefs[KEY_SELECTED_MODEL_ID] = value
                        } else {
                            prefs.remove(KEY_SELECTED_MODEL_ID)
                        }
                    }
                }
            }

        companion object {
            private val KEY_SELECTED_MODEL_ID = stringPreferencesKey("selected_model_id")
        }
    }
