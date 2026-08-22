package dev.hossain.codematex.data.repository

/**
 * In-memory fake of [ModelSelectionStore] for unit tests.
 */
class FakeModelSelectionStore(
    initialSelectedModelId: String? = null,
) : ModelSelectionStore {
    override var selectedModelId: String? = initialSelectedModelId
}
