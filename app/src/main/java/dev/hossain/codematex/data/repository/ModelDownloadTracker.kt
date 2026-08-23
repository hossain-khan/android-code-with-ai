package dev.hossain.codematex.data.repository

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.hossain.codematex.worker.ModelDownloadWorker
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Abstraction over WorkManager used to track and enqueue model downloads.
 *
 * This allows JVM unit tests to observe download/cancel calls without
 * initializing the real WorkManager framework.
 */
interface ModelDownloadTracker {
    /**
     * Returns a [Flow] of [WorkInfo] lists for the unique work identified by
     * [modelId].
     */
    fun getWorkInfoFlow(modelId: String): Flow<List<WorkInfo>>

    /**
     * Enqueues a download for [modelId] from [urls] to [path], trying candidate URLs in order
     * and optionally verifying against [expectedSha256].
     */
    fun enqueueDownload(
        modelId: String,
        urls: List<String>,
        path: String,
        expectedSha256: String? = null,
    )

    /**
     * Enqueues a download for [modelId] from [url] to [path].
     */
    fun enqueueDownload(
        modelId: String,
        url: String,
        path: String,
        expectedSha256: String? = null,
    ) = enqueueDownload(modelId, listOf(url), path, expectedSha256)

    /**
     * Cancels the download identified by [modelId].
     */
    fun cancelDownload(modelId: String)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelDownloadTrackerImpl
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) : ModelDownloadTracker {
        override fun getWorkInfoFlow(modelId: String): Flow<List<WorkInfo>> = workManager.getWorkInfosForUniqueWorkFlow(modelId)

        override fun enqueueDownload(
            modelId: String,
            urls: List<String>,
            path: String,
            expectedSha256: String?,
        ) {
            val builder =
                Data
                    .Builder()
                    .putStringArray(ModelDownloadWorker.KEY_URLS, urls.toTypedArray())
                    .putString(ModelDownloadWorker.KEY_URL, urls.firstOrNull() ?: "")
                    .putString(ModelDownloadWorker.KEY_PATH, path)

            if (expectedSha256 != null) {
                builder.putString(ModelDownloadWorker.KEY_SHA256, expectedSha256)
            }

            val data = builder.build()

            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val request =
                OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                    .setInputData(data)
                    .setConstraints(constraints)
                    .build()

            workManager.enqueueUniqueWork(
                modelId,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        override fun cancelDownload(modelId: String) {
            workManager.cancelUniqueWork(modelId)
        }
    }
