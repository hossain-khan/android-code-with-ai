package dev.hossain.codematex.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory fake of [ModelSelectionStore] for unit tests.
 */
class FakeModelSelectionStore(
    initialSelectedModelId: String? = null,
) : ModelSelectionStore {
    override val selectedModelIdFlow: Flow<String?>
        field = MutableStateFlow(initialSelectedModelId)

    override var selectedModelId: String?
        get() = selectedModelIdFlow.value
        set(value) {
            selectedModelIdFlow.value = value
        }
}
