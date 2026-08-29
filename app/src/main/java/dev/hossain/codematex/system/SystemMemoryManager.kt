package dev.hossain.codematex.system

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.Immutable
import dev.hossain.codematex.di.ApplicationContext
import dev.hossain.codematex.runtime.LlmEngine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject

/**
 * Result of a memory headroom evaluation before initiating heavy workloads.
 */
sealed interface MemoryHeadroomResult {
    /**
     * Sufficient physical RAM headroom is available to safely load on-device models.
     */
    data object Sufficient : MemoryHeadroomResult

    /**
     * System RAM is constrained or near low-memory threshold, posing a high risk of LMK termination.
     *
     * @property availMemBytes Current available system memory in bytes.
     * @property requiredBytes Minimum required memory headroom in bytes.
     * @property isLowMemory Whether the OS lowMemory threshold flag is currently active.
     */
    @Immutable
    @Serializable
    data class Constrained(
        val availMemBytes: Long,
        val requiredBytes: Long,
        val isLowMemory: Boolean,
    ) : MemoryHeadroomResult
}

/**
 * System-wide memory manager that monitors OS memory pressure ([ComponentCallbacks2]),
 * coordinates background model eviction to prevent Low Memory Killer (LMK) process termination,
 * validates pre-flight RAM headroom before loading multi-gigabyte models, and tracks historical
 * LMK process exit reasons ([android.app.ApplicationExitInfo]).
 */
interface SystemMemoryManager {
    /**
     * Evaluates whether the device has sufficient available RAM to safely initialize an on-device model.
     *
     * @param requiredHeadroomBytes Minimum recommended available RAM in bytes (default: 1.2 GB).
     */
    fun checkMemoryHeadroom(requiredHeadroomBytes: Long = DEFAULT_MIN_HEADROOM_BYTES): MemoryHeadroomResult

    /**
     * Handles OS memory trimming signals ([ComponentCallbacks2.onTrimMemory]).
     */
    fun onTrimMemory(level: Int)

    /**
     * Handles system-wide critical low memory events ([ComponentCallbacks2.onLowMemory]).
     */
    fun onLowMemory()

    /**
     * Registers memory callbacks and activity lifecycle listeners on the [application].
     */
    fun register(application: Application)

    /**
     * Inspects historical process termination reasons (API 30+) to log LMK diagnoses.
     */
    fun checkHistoricalExitReasons()

    companion object {
        /**
         * Minimum recommended free RAM headroom (1.2 GB) required to safely initialize on-device models.
         */
        const val DEFAULT_MIN_HEADROOM_BYTES: Long = 1_200_000_000L // 1.2 GB

        /**
         * Background idle grace duration before evicting model weights from native memory (3 minutes).
         */
        const val BACKGROUND_EVICTION_DELAY_MS: Long = 3 * 60 * 1000L // 3 minutes
    }
}

/**
 * Production implementation of [SystemMemoryManager] that listens to Android OS trimming events
 * and activity lifecycles to release multi-gigabyte LLM buffers when the app is backgrounded.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SystemMemoryManagerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val llmEngine: LlmEngine,
    ) : SystemMemoryManager {
        private var dispatcher: CoroutineDispatcher = Dispatchers.Default
        private var backgroundEvictionDelayMs: Long = SystemMemoryManager.BACKGROUND_EVICTION_DELAY_MS
        private var memoryInfoProvider: (() -> Pair<Long, Boolean>)? = null

        /**
         * Secondary constructor for unit testing with custom dispatchers and mock memory providers.
         */
        internal constructor(
            context: Context,
            llmEngine: LlmEngine,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            backgroundEvictionDelayMs: Long = SystemMemoryManager.BACKGROUND_EVICTION_DELAY_MS,
            memoryInfoProvider: (() -> Pair<Long, Boolean>)? = null,
        ) : this(context, llmEngine) {
            this.dispatcher = dispatcher
            this.backgroundEvictionDelayMs = backgroundEvictionDelayMs
            this.memoryInfoProvider = memoryInfoProvider
        }

        private val activityManager by lazy {
            try {
                context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            } catch (e: Exception) {
                null
            }
        }
        private val scope by lazy { CoroutineScope(SupervisorJob() + dispatcher) }

        private var backgroundEvictionJob: Job? = null
        private var startedActivityCount = 0

        private val componentCallbacks =
            object : ComponentCallbacks2 {
                override fun onTrimMemory(level: Int) {
                    this@SystemMemoryManagerImpl.onTrimMemory(level)
                }

                @Deprecated("Deprecated in Java")
                @Suppress("DEPRECATION")
                override fun onLowMemory() {
                    this@SystemMemoryManagerImpl.onLowMemory()
                }

                override fun onConfigurationChanged(newConfig: Configuration) {}
            }

        private val activityLifecycleCallbacks =
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    startedActivityCount++
                    cancelBackgroundEviction()
                }

                override fun onActivityStopped(activity: Activity) {
                    startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
                    if (startedActivityCount == 0) {
                        scheduleBackgroundEviction()
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                    cancelBackgroundEviction()
                }

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) {}

                override fun onActivityPaused(activity: Activity) {}

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) {}

                override fun onActivityDestroyed(activity: Activity) {}
            }

        override fun checkMemoryHeadroom(requiredHeadroomBytes: Long): MemoryHeadroomResult {
            val provider = memoryInfoProvider
            val (availMemBytes, isLowMemory) =
                if (provider != null) {
                    provider.invoke()
                } else {
                    val memoryInfo = ActivityManager.MemoryInfo()
                    activityManager?.getMemoryInfo(memoryInfo)
                    Pair(memoryInfo.availMem, memoryInfo.lowMemory)
                }

            val isConstrained = isLowMemory || availMemBytes < requiredHeadroomBytes
            return if (isConstrained) {
                val availMb = availMemBytes / (1024f * 1024f)
                val reqMb = requiredHeadroomBytes / (1024f * 1024f)
                Timber.w(
                    "SystemMemoryManager: Memory constrained (avail=%.1f MB, required=%.1f MB, isLowMemory=%b)",
                    availMb,
                    reqMb,
                    isLowMemory,
                )
                MemoryHeadroomResult.Constrained(
                    availMemBytes = availMemBytes,
                    requiredBytes = requiredHeadroomBytes,
                    isLowMemory = isLowMemory,
                )
            } else {
                MemoryHeadroomResult.Sufficient
            }
        }

        @Suppress("DEPRECATION")
        override fun onTrimMemory(level: Int) {
            Timber.i("SystemMemoryManager [ON_TRIM_MEMORY]: level=%d", level)
            when (level) {
                ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                    scheduleBackgroundEviction()
                }

                ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
                ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
                ComponentCallbacks2.TRIM_MEMORY_MODERATE,
                ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
                -> {
                    Timber.w("SystemMemoryManager: Critical memory trim signal (level=%d). Evicting LLM immediately.", level)
                    cancelBackgroundEviction()
                    evictModelMemory("TrimLevel=$level")
                }

                else -> {
                    Timber.d("SystemMemoryManager: Non-critical trim level %d received", level)
                }
            }
        }

        override fun onLowMemory() {
            Timber.w("SystemMemoryManager [ON_LOW_MEMORY]: System-wide low memory event. Releasing LLM native buffers immediately.")
            cancelBackgroundEviction()
            evictModelMemory("onLowMemory")
        }

        override fun register(application: Application) {
            application.registerComponentCallbacks(componentCallbacks)
            application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
            checkHistoricalExitReasons()
            Timber.d("SystemMemoryManager: Registered memory and lifecycle callbacks")
        }

        override fun checkHistoricalExitReasons() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val exitReasons = activityManager?.getHistoricalProcessExitReasons(context.packageName, 0, 3)
                    exitReasons?.firstOrNull()?.let { lastExit ->
                        when (lastExit.reason) {
                            android.app.ApplicationExitInfo.REASON_LOW_MEMORY -> {
                                Timber.w(
                                    "SystemMemoryManager [LMK_DIAGNOSTIC]: Previous process was killed by Low Memory Killer (LMK). " +
                                        "PSS: %.1f MB, RSS: %.1f MB, Description: %s",
                                    lastExit.pss / (1024f * 1024f),
                                    lastExit.rss / (1024f * 1024f),
                                    lastExit.description ?: "None",
                                )
                            }

                            android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> {
                                Timber.w(
                                    "SystemMemoryManager [CRASH_DIAGNOSTIC]: Previous process had native crash. Description: %s",
                                    lastExit.description ?: "None",
                                )
                            }

                            else -> {
                                Timber.d("SystemMemoryManager: Previous process exit reason code: %d", lastExit.reason)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "SystemMemoryManager: Could not retrieve historical process exit reasons")
                }
            }
        }

        private fun scheduleBackgroundEviction() {
            if (!llmEngine.isInitialized()) {
                Timber.d("SystemMemoryManager: Engine not loaded, skipping background eviction schedule")
                return
            }
            cancelBackgroundEviction()
            Timber.i(
                "SystemMemoryManager [TRIM_TIMER_START]: App moved to background (TRIM_MEMORY_UI_HIDDEN). " +
                    "Scheduling eviction in %d ms",
                backgroundEvictionDelayMs,
            )
            backgroundEvictionJob =
                scope.launch {
                    delay(backgroundEvictionDelayMs)
                    evictModelMemory("BackgroundIdleTimeout")
                }
        }

        private fun cancelBackgroundEviction() {
            if (backgroundEvictionJob?.isActive == true) {
                Timber.d("SystemMemoryManager [TRIM_TIMER_CANCEL]: App returned to foreground, cancelling background eviction")
                backgroundEvictionJob?.cancel()
            }
            backgroundEvictionJob = null
        }

        private fun evictModelMemory(triggerReason: String) {
            if (llmEngine.isInitialized()) {
                Timber.i("SystemMemoryManager [EVICT_START]: Releasing native model buffers (reason=%s)", triggerReason)
                try {
                    llmEngine.cleanup()
                    Timber.i("SystemMemoryManager [EVICT_SUCCESS]: Native buffers successfully released (reason=%s)", triggerReason)
                } catch (e: Exception) {
                    Timber.e(e, "SystemMemoryManager [EVICT_ERROR]: Failed to cleanup LLM engine")
                }
            }
        }

        // Exposed for testing lifecycle events
        internal fun notifyActivityStarted() {
            startedActivityCount++
            cancelBackgroundEviction()
        }

        internal fun notifyActivityStopped() {
            startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
            if (startedActivityCount == 0) {
                scheduleBackgroundEviction()
            }
        }

        internal fun notifyActivityResumed() {
            cancelBackgroundEviction()
        }
    }
