package dev.hossain.codematex.data.repository

import java.io.File

/**
 * In-memory fake of [ModelFileStorage] for unit tests.
 */
class FakeModelFileStorage(
    private val modelsDir: File = File("/fake/models"),
    existingPaths: Set<String> = emptySet(),
) : ModelFileStorage {
    private val existingFiles = existingPaths.toMutableSet()
    val deletedPaths = mutableListOf<String>()

    override fun getModelsDir(): File = modelsDir

    override fun modelExists(path: String): Boolean = path in existingFiles

    override fun deleteModel(path: String): Boolean {
        deletedPaths += path
        return existingFiles.remove(path)
    }

    override fun getLocalPath(
        modelId: String,
        modelFile: String,
    ): String {
        val normalizedName = modelId.replace("/", "_")
        return "${modelsDir.absolutePath}/$normalizedName/$modelFile"
    }

    /**
     * Marks [path] as an existing model file.
     */
    fun addModel(path: String) {
        existingFiles += path
    }
}
