@file:Suppress("OPT_IN_USAGE_FUTURE_ERROR")

package dev.hossain.codematex.data.repository

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.di.ApplicationContext
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.worker.ModelDownloadWorker
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import java.io.File
import javax.inject.Inject

@Serializable
data class ModelAllowlist(
    val models: List<ModelEntry>,
)

@Serializable
data class ModelEntry(
    val modelId: String,
    val modelFile: String,
    val commitHash: String,
    val sizeInBytes: Long,
    val taskTypes: List<String>,
    val runtimeType: String,
)

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class ModelRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ModelRepository {
        private val workManager = WorkManager.getInstance(context)
        private val modelsDir = File(context.getExternalFilesDir(null), "models")

        private var selectedModelId: String? = null
        private var cachedModels: List<AiModel> = emptyList()

        override fun getAvailableModels(): Flow<List<AiModel>> =
            callbackFlow {
                val allowlist = loadAllowlist()
                val models =
                    allowlist.map { entry ->
                        val localPath = getModelLocalPath(entry)
                        val file = File(localPath)
                        val downloadStatus =
                            if (file.exists()) {
                                DownloadStatus.DOWNLOADED
                            } else {
                                getDownloadStatus(entry.modelId)
                            }

                        AiModel(
                            id = entry.modelId,
                            name = entry.modelId.substringAfterLast("/"),
                            displayName = entry.modelId.substringAfterLast("/"),
                            downloadUrl = buildDownloadUrl(entry),
                            sizeBytes = entry.sizeInBytes,
                            localPath = localPath.takeIf { file.exists() },
                            downloadStatus = downloadStatus,
                            preferredBackend = LlmEngine.Backend.CPU,
                        )
                    }
                cachedModels = models
                trySend(models)
                awaitClose {}
            }

        override fun getSelectedModel(): AiModel? = cachedModels.find { it.id == selectedModelId }

        override suspend fun selectModel(model: AiModel) {
            selectedModelId = model.id
        }

        override suspend fun downloadModel(model: AiModel) {
            val data =
                Data
                    .Builder()
                    .putString(ModelDownloadWorker.KEY_URL, model.downloadUrl)
                    .putString(ModelDownloadWorker.KEY_PATH, model.localPath ?: getModelLocalPathById(model.id))
                    .build()

            val request =
                OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                    .setInputData(data)
                    .build()

            workManager.enqueueUniqueWork(
                model.id,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        override suspend fun cancelDownload(model: AiModel) {
            workManager.cancelUniqueWork(model.id)
        }

        override suspend fun deleteModel(model: AiModel) {
            val path = model.localPath ?: getModelLocalPathById(model.id)
            File(path).delete()
            if (selectedModelId == model.id) {
                selectedModelId = null
            }
        }

        private fun loadAllowlist(): List<ModelEntry> {
            // TODO: Fetch from remote URL. For now, return bundled allowlist.
            return listOf(
                ModelEntry(
                    modelId = "litert-community/gemma-4-E2B-it-litert-lm",
                    modelFile = "gemma-4-E2B-it.litertlm",
                    commitHash = "6e5c4f1e395deb959c494953478fa5cec4b8008f",
                    sizeInBytes = 2_588_147_712,
                    taskTypes = listOf("llm_chat"),
                    runtimeType = "LITERT_LM",
                ),
                ModelEntry(
                    modelId = "litert-community/gemma-4-E4B-it-litert-lm",
                    modelFile = "gemma-4-E4B-it.litertlm",
                    commitHash = "28299f30ee4d43294517a4ac93abd6163412f07f",
                    sizeInBytes = 3_659_530_240,
                    taskTypes = listOf("llm_chat"),
                    runtimeType = "LITERT_LM",
                ),
            )
        }

        private fun buildDownloadUrl(entry: ModelEntry): String =
            "https://huggingface.co/${entry.modelId}/resolve/${entry.commitHash}/${entry.modelFile}?download=true"

        private fun getModelLocalPath(entry: ModelEntry): String {
            val normalizedName = entry.modelId.replace("/", "_")
            return "${modelsDir.absolutePath}/$normalizedName/${entry.modelFile}"
        }

        private fun getModelLocalPathById(modelId: String): String {
            val normalizedName = modelId.replace("/", "_")
            return "${modelsDir.absolutePath}/$normalizedName/${modelId.substringAfterLast("/")}.litertlm"
        }

        private fun getDownloadStatus(modelId: String): DownloadStatus {
            val workInfos = workManager.getWorkInfosForUniqueWork(modelId).get()
            val latestWork = workInfos.lastOrNull()
            return when (latestWork?.state) {
                WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> DownloadStatus.DOWNLOADING
                WorkInfo.State.FAILED -> DownloadStatus.FAILED
                else -> DownloadStatus.NOT_DOWNLOADED
            }
        }
    }
