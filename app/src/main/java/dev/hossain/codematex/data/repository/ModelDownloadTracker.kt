package dev.hossain.codematex.data.repository

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.hossain.codematex.work.ModelDownloadWorker
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
        modelName: String? = null,
    )

    /**
     * Enqueues a download for [modelId] from [url] to [path].
     */
    fun enqueueDownload(
        modelId: String,
        url: String,
        path: String,
        expectedSha256: String? = null,
        modelName: String? = null,
    ) = enqueueDownload(modelId, listOf(url), path, expectedSha256, modelName)

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
        private val downloadPreferences: ModelDownloadPreferences,
    ) : ModelDownloadTracker {
        override fun getWorkInfoFlow(modelId: String): Flow<List<WorkInfo>> = workManager.getWorkInfosForUniqueWorkFlow(modelId)

        override fun enqueueDownload(
            modelId: String,
            urls: List<String>,
            path: String,
            expectedSha256: String?,
            modelName: String?,
        ) {
            val builder =
                Data
                    .Builder()
                    .putString(ModelDownloadWorker.KEY_MODEL_ID, modelId)
                    .putString(ModelDownloadWorker.KEY_MODEL_NAME, modelName ?: modelId.substringAfterLast("/"))
                    .putStringArray(ModelDownloadWorker.KEY_URLS, urls.toTypedArray())
                    .putString(ModelDownloadWorker.KEY_URL, urls.firstOrNull() ?: "")
                    .putString(ModelDownloadWorker.KEY_PATH, path)

            if (expectedSha256 != null) {
                builder.putString(ModelDownloadWorker.KEY_SHA256, expectedSha256)
            }

            val data = builder.build()

            val isWifiOnly =
                try {
                    kotlinx.coroutines.runBlocking {
                        downloadPreferences.getDownloadOverWifiOnly()
                    }
                } catch (e: Exception) {
                    true
                }

            val networkType =
                if (isWifiOnly) {
                    NetworkType.UNMETERED
                } else {
                    NetworkType.CONNECTED
                }

            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(networkType)
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
