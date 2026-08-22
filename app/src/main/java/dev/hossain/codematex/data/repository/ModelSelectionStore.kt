package dev.hossain.codematex.data.repository

import android.content.Context
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
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
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelSelectionStoreImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ModelSelectionStore {
        private val prefs = context.getSharedPreferences("model_prefs", Context.MODE_PRIVATE)

        override var selectedModelId: String?
            get() = prefs.getString("selected_model_id", null)
            set(value) {
                prefs.edit().putString("selected_model_id", value).apply()
            }
    }
