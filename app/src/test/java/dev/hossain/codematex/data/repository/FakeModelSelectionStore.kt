package dev.hossain.codematex.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/**
 * In-memory fake of [ModelSelectionStore] for unit tests.
 */
class FakeModelSelectionStore(
    initialSelectedModelId: String? = null,
) : ModelSelectionStore {
    override val selectedModelIdFlow: Flow<String?>
        field = MutableStateFlow(initialSelectedModelId)

    var shouldThrowOnWrite: Boolean = false

    override suspend fun getSelectedModelId(): String? = selectedModelIdFlow.value

    override suspend fun setSelectedModelId(modelId: String?) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        selectedModelIdFlow.value = modelId
    }
}
