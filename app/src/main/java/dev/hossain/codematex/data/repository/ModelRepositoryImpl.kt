package dev.hossain.codematex.data.repository

import androidx.work.WorkInfo
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.worker.ModelDownloadWorker
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class ModelRepositoryImpl
    @Inject
    constructor(
        private val fileStorage: ModelFileStorage,
        private val selectionStore: ModelSelectionStore,
        private val downloadTracker: ModelDownloadTracker,
        private val allowlistDataSource: ModelAllowlistDataSource,
    ) : ModelRepository {
        private var cachedModels: List<AiModel> = initialModelScan()

        private fun initialModelScan(): List<AiModel> =
            try {
                allowlistDataSource.loadAllowlist().map { entry ->
                    buildAiModel(entry)
                }
            } catch (e: Exception) {
                emptyList()
            }

        override fun getAvailableModels(): Flow<List<AiModel>> =
            flow {
                val allowlist = allowlistDataSource.loadAllowlist()
                if (allowlist.isEmpty()) {
                    cachedModels = emptyList()
                    emit(emptyList())
                    return@flow
                }

                val modelIds = allowlist.map { it.modelId }
                val progressFlows = modelIds.map { id -> downloadTracker.getWorkInfoFlow(id) }

                combine(progressFlows) { workInfoLists ->
                    val progressMap = mutableMapOf<String, Pair<DownloadStatus, Int>>()
                    workInfoLists.forEachIndexed { index, workInfos ->
                        val modelId = modelIds[index]
                        val latestWork = workInfos.lastOrNull()
                        val status =
                            when (latestWork?.state) {
                                WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> DownloadStatus.DOWNLOADING
                                WorkInfo.State.FAILED -> DownloadStatus.FAILED
                                else -> DownloadStatus.NOT_DOWNLOADED
                            }
                        val progress = latestWork?.progress?.getInt(ModelDownloadWorker.KEY_PROGRESS, 0) ?: 0
                        progressMap[modelId] = status to progress
                    }

                    val models =
                        allowlist.map { entry ->
                            val localPath = fileStorage.getLocalPath(entry.modelId, entry.modelFile)
                            val isDownloaded = fileStorage.modelExists(localPath)
                            val (status, progress) =
                                if (isDownloaded) {
                                    DownloadStatus.DOWNLOADED to 100
                                } else {
                                    progressMap[entry.modelId] ?: (DownloadStatus.NOT_DOWNLOADED to 0)
                                }

                            buildAiModel(entry, localPath, isDownloaded, status, progress)
                        }
                    cachedModels = models
                    emit(models)
                }.collect {}
            }

        override fun getSelectedModel(): AiModel? {
            val savedId = selectionStore.selectedModelId
            val savedModel = cachedModels.find { it.id == savedId && it.downloadStatus == DownloadStatus.DOWNLOADED }
            if (savedModel != null) return savedModel

            // Fallback: auto-select the first available downloaded model
            val firstDownloaded = cachedModels.find { it.downloadStatus == DownloadStatus.DOWNLOADED }
            if (firstDownloaded != null) {
                selectionStore.selectedModelId = firstDownloaded.id
                return firstDownloaded
            }
            return null
        }

        override suspend fun selectModel(model: AiModel) {
            selectionStore.selectedModelId = model.id
        }

        override suspend fun downloadModel(model: AiModel) {
            val path = model.localPath ?: getModelLocalPathById(model.id)
            downloadTracker.enqueueDownload(model.id, model.downloadUrl, path)
        }

        override suspend fun cancelDownload(model: AiModel) {
            downloadTracker.cancelDownload(model.id)
        }

        override suspend fun deleteModel(model: AiModel) {
            val path = model.localPath ?: getModelLocalPathById(model.id)
            fileStorage.deleteModel(path)
            if (selectionStore.selectedModelId == model.id) {
                selectionStore.selectedModelId = null
            }
        }

        private fun buildAiModel(
            entry: ModelEntry,
            localPath: String = fileStorage.getLocalPath(entry.modelId, entry.modelFile),
            isDownloaded: Boolean = fileStorage.modelExists(localPath),
            status: DownloadStatus = if (isDownloaded) DownloadStatus.DOWNLOADED else DownloadStatus.NOT_DOWNLOADED,
            progress: Int = if (isDownloaded) 100 else 0,
        ): AiModel =
            AiModel(
                id = entry.modelId,
                name = entry.modelId.substringAfterLast("/"),
                displayName = entry.modelId.substringAfterLast("/"),
                downloadUrl = buildDownloadUrl(entry),
                sizeBytes = entry.sizeInBytes,
                localPath = localPath.takeIf { isDownloaded },
                downloadStatus = status,
                preferredBackend = LlmEngine.Backend.GPU,
                minDeviceMemoryInGb = entry.minDeviceMemoryInGb,
                downloadProgress = progress,
                publisher = entry.publisher,
                modelRepoUrl = "https://huggingface.co/${entry.modelId}",
                license = entry.license,
                licenseUrl = entry.licenseUrl,
                description = entry.description,
            )

        private fun buildDownloadUrl(entry: ModelEntry): String =
            "https://huggingface.co/${entry.modelId}/resolve/${entry.commitHash}/${entry.modelFile}?download=true"

        private fun getModelLocalPathById(modelId: String): String {
            allowlistDataSource
                .loadAllowlist()
                .firstOrNull { it.modelId == modelId }
                ?.let { return fileStorage.getLocalPath(it.modelId, it.modelFile) }

            return fileStorage.getLocalPath(modelId, "${modelId.substringAfterLast("/")}.litertlm")
        }
    }
