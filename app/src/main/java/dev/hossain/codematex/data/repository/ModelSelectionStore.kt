package dev.hossain.codematex.data.repository

import android.content.Context
import androidx.core.content.edit
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Abstraction over persistent storage for the currently selected model id.
 *
 * Tests can supply an in-memory fake to avoid depending on Android
 * [SharedPreferences].
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
        @param:ApplicationContext private val context: Context,
    ) : ModelSelectionStore {
        private val prefs = context.getSharedPreferences("model_prefs", Context.MODE_PRIVATE)
        override val selectedModelIdFlow: Flow<String?>
            field = MutableStateFlow(prefs.getString("selected_model_id", null))

        override var selectedModelId: String?
            get() = selectedModelIdFlow.value
            set(value) {
                prefs.edit { putString("selected_model_id", value) }
                selectedModelIdFlow.value = value
            }
    }
