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
    private val _selectedModelIdFlow = MutableStateFlow(initialSelectedModelId)

    override val selectedModelIdFlow: Flow<String?> = _selectedModelIdFlow.asStateFlow()

    var shouldThrowOnWrite: Boolean = false

    override suspend fun getSelectedModelId(): String? = _selectedModelIdFlow.value

    override suspend fun setSelectedModelId(modelId: String?) {
        if (shouldThrowOnWrite) {
            throw java.io.IOException("Fake disk write failure")
        }
        _selectedModelIdFlow.value = modelId
    }
}
