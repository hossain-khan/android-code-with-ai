package dev.hossain.codematex.data.repository

import android.content.Context
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import java.io.File
import javax.inject.Inject

/**
 * Abstraction over file-system operations for downloaded models.
 *
 * This interface keeps [ModelRepositoryImpl] testable on the JVM by allowing
 * tests to supply an in-memory fake instead of touching the Android file system.
 */
interface ModelFileStorage {
    /**
     * Returns the directory where models are stored.
     */
    fun getModelsDir(): File

    /**
     * Returns `true` if a model file exists at [path].
     */
    fun modelExists(path: String): Boolean

    /**
     * Deletes the model file at [path] and returns whether the delete succeeded.
     */
    fun deleteModel(path: String): Boolean

    /**
     * Computes the local file path for a model.
     */
    fun getLocalPath(
        modelId: String,
        modelFile: String,
    ): String
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelFileStorageImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ModelFileStorage {
        private val modelsDir = File(context.getExternalFilesDir(null), "models")

        override fun getModelsDir(): File = modelsDir

        override fun modelExists(path: String): Boolean = File(path).exists()

        override fun deleteModel(path: String): Boolean = File(path).delete()

        override fun getLocalPath(
            modelId: String,
            modelFile: String,
        ): String {
            val normalizedName = modelId.replace("/", "_")
            return "${modelsDir.absolutePath}/$normalizedName/$modelFile"
        }
    }
