package dev.hossain.codematex.data.repository

import androidx.work.Data
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory fake of [ModelDownloadTracker] for unit tests.
 */
class FakeModelDownloadTracker : ModelDownloadTracker {
    private val workInfoFlows = mutableMapOf<String, MutableStateFlow<List<WorkInfo>>>()
    val enqueuedDownloads = mutableListOf<Triple<String, String, String>>()
    val enqueuedMultiUrlDownloads = mutableListOf<Triple<String, List<String>, String>>()
    val cancelledDownloads = mutableListOf<String>()

    override fun getWorkInfoFlow(modelId: String): Flow<List<WorkInfo>> = workInfoFlows.getOrPut(modelId) { MutableStateFlow(emptyList()) }

    override fun enqueueDownload(
        modelId: String,
        urls: List<String>,
        path: String,
    ) {
        enqueuedMultiUrlDownloads += Triple(modelId, urls, path)
        enqueuedDownloads += Triple(modelId, urls.firstOrNull() ?: "", path)
    }

    override fun enqueueDownload(
        modelId: String,
        url: String,
        path: String,
    ) {
        enqueuedDownloads += Triple(modelId, url, path)
        enqueuedMultiUrlDownloads += Triple(modelId, listOf(url), path)
    }

    override fun cancelDownload(modelId: String) {
        cancelledDownloads += modelId
    }

    /**
     * Emits a [WorkInfo] list for [modelId] so that [getWorkInfoFlow] collectors
     * observe the new state.
     */
    fun emitWorkInfo(
        modelId: String,
        workInfos: List<WorkInfo>,
    ) {
        workInfoFlows.getOrPut(modelId) { MutableStateFlow(emptyList()) }.value = workInfos
    }

    /**
     * Convenience helper to emit a single RUNNING [WorkInfo] with the given progress.
     */
    fun emitProgress(
        modelId: String,
        progress: Int,
    ) {
        val data = Data.Builder().putInt(dev.hossain.codematex.worker.ModelDownloadWorker.KEY_PROGRESS, progress).build()
        val workInfo =
            WorkInfo(
                java.util.UUID.randomUUID(),
                WorkInfo.State.RUNNING,
                emptySet<String>(),
                Data.EMPTY,
                data,
                0,
                0,
            )
        emitWorkInfo(modelId, listOf(workInfo))
    }

    /**
     * Convenience helper to emit a single FAILED [WorkInfo].
     */
    fun emitFailed(modelId: String) {
        val workInfo =
            WorkInfo(
                java.util.UUID.randomUUID(),
                WorkInfo.State.FAILED,
                emptySet<String>(),
                Data.EMPTY,
                Data.EMPTY,
                0,
                0,
            )
        emitWorkInfo(modelId, listOf(workInfo))
    }
}
