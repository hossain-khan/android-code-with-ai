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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
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

        private val storageChanges = MutableStateFlow(0)

        override fun getAvailableModels(): Flow<List<AiModel>> {
            val allowlist = allowlistDataSource.loadAllowlist()
            if (allowlist.isEmpty()) {
                cachedModels = emptyList()
                return flowOf(emptyList())
            }

            val modelIds = allowlist.map { it.modelId }
            val progressFlows = modelIds.map { id -> downloadTracker.getWorkInfoFlow(id) }

            // Cache file-existence checks because modelExists() hits disk. The cache is
            // invalidated when storage changes (delete) or when a WorkManager job reaches a
            // terminal state, so download completion/failure is reflected on the next emission.
            var lastStorageVersion = -1
            val downloadedCache = mutableMapOf<String, Boolean>()

            return combine(
                combine(progressFlows) { it.toList() },
                selectionStore.selectedModelIdFlow,
                storageChanges,
            ) { workInfoLists, savedSelectedId, storageVersion ->
                val hasTerminalWorkState =
                    workInfoLists.any { workInfos ->
                        workInfos.any { it.state.isFinished }
                    }
                if (storageVersion != lastStorageVersion || hasTerminalWorkState) {
                    lastStorageVersion = storageVersion
                    downloadedCache.clear()
                }

                val progressMap = mutableMapOf<String, Triple<DownloadStatus, Int, String?>>()
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
                    val errorMessage =
                        if (status == DownloadStatus.FAILED) {
                            latestWork?.outputData?.getString(ModelDownloadWorker.KEY_ERROR_MESSAGE)
                        } else {
                            null
                        }
                    progressMap[modelId] = Triple(status, progress, errorMessage)
                }

                val downloadedModelIds = mutableSetOf<String>()
                val rawModels =
                    allowlist.map { entry ->
                        val localPath = fileStorage.getLocalPath(entry.modelId, entry.modelFile)
                        val isDownloaded =
                            downloadedCache.getOrPut(entry.modelId) {
                                fileStorage.modelExists(localPath)
                            }
                        if (isDownloaded) {
                            downloadedModelIds.add(entry.modelId)
                        }
                        val (status, progress, errorMessage) =
                            if (isDownloaded) {
                                Triple(DownloadStatus.DOWNLOADED, 100, null)
                            } else {
                                progressMap[entry.modelId] ?: Triple(DownloadStatus.NOT_DOWNLOADED, 0, null)
                            }

                        buildAiModel(entry, localPath, isDownloaded, status, progress, errorMessage)
                    }

                val effectiveSelectedId =
                    savedSelectedId?.takeIf { downloadedModelIds.contains(it) }
                        ?: downloadedModelIds.firstOrNull()

                val models =
                    rawModels.map { model ->
                        model.copy(
                            isSelected =
                                model.downloadStatus == DownloadStatus.DOWNLOADED &&
                                    model.id == effectiveSelectedId,
                        )
                    }

                cachedModels = models
                models
            }
        }

        override fun getSelectedModel(): AiModel? {
            val explicitlySelected = cachedModels.find { it.isSelected && it.downloadStatus == DownloadStatus.DOWNLOADED }
            if (explicitlySelected != null) return explicitlySelected

            // Fallback: auto-select the first available downloaded model
            val firstDownloaded = cachedModels.find { it.downloadStatus == DownloadStatus.DOWNLOADED }
            return firstDownloaded?.copy(isSelected = true)
        }

        override suspend fun selectModel(model: AiModel) {
            selectionStore.setSelectedModelId(model.id)
        }

        override suspend fun downloadModel(model: AiModel) {
            val path = model.localPath ?: getModelLocalPathById(model.id)
            val candidateUrls = listOf(model.downloadUrl) + model.fallbackDownloadUrls
            downloadTracker.enqueueDownload(
                modelId = model.id,
                urls = candidateUrls,
                path = path,
                expectedSha256 = model.sha256,
                modelName = model.displayName,
            )
        }

        override suspend fun cancelDownload(model: AiModel) {
            downloadTracker.cancelDownload(model.id)
        }

        override suspend fun deleteModel(model: AiModel) {
            val path = model.localPath ?: getModelLocalPathById(model.id)
            val deleted = fileStorage.deleteModel(path)
            if (!deleted) {
                Timber.w("ModelRepositoryImpl: File delete returned false for path %s", path)
            }
            if (selectionStore.getSelectedModelId() == model.id) {
                selectionStore.setSelectedModelId(null)
            }
            storageChanges.value++
        }

        private fun buildAiModel(
            entry: ModelEntry,
            localPath: String = fileStorage.getLocalPath(entry.modelId, entry.modelFile),
            isDownloaded: Boolean = fileStorage.modelExists(localPath),
            status: DownloadStatus = if (isDownloaded) DownloadStatus.DOWNLOADED else DownloadStatus.NOT_DOWNLOADED,
            progress: Int = if (isDownloaded) 100 else 0,
            downloadErrorMessage: String? = null,
            isSelected: Boolean = false,
        ): AiModel =
            AiModel(
                id = entry.modelId,
                name = entry.modelId.substringAfterLast("/"),
                displayName = entry.modelId.substringAfterLast("/"),
                downloadUrl = entry.downloadUrl ?: buildDownloadUrl(entry),
                fallbackDownloadUrls =
                    entry.fallbackDownloadUrls.ifEmpty {
                        listOf(buildFallbackDownloadUrl(entry))
                    },
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
                sha256 = entry.sha256,
                downloadErrorMessage = downloadErrorMessage,
                isSelected = isSelected,
            )

        private fun buildDownloadUrl(entry: ModelEntry): String =
            "https://light-llm-storage.gohk.xyz/models/${entry.modelId}/${entry.modelFile}"

        private fun buildFallbackDownloadUrl(entry: ModelEntry): String =
            "https://huggingface.co/${entry.modelId}/resolve/${entry.commitHash}/${entry.modelFile}?download=true"

        private fun getModelLocalPathById(modelId: String): String {
            allowlistDataSource
                .loadAllowlist()
                .firstOrNull { it.modelId == modelId }
                ?.let { return fileStorage.getLocalPath(it.modelId, it.modelFile) }

            return fileStorage.getLocalPath(modelId, "${modelId.substringAfterLast("/")}.litertlm")
        }
    }
