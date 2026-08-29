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
 *
 * Official Android Reference:
 * - [Manage your app's memory](https://developer.android.com/topic/performance/memory/manage-app-memory)
 * - [Low memory killers (LMK)](https://developer.android.com/topic/performance/vitals/lmk)
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
 *
 * ## Official Android Documentation & Guidelines:
 * - [Manage your app's memory](https://developer.android.com/topic/performance/memory/manage-app-memory)
 * - [Low memory killers (LMK) on Android Vitals](https://developer.android.com/topic/performance/vitals/lmk)
 * - [ComponentCallbacks2.onTrimMemory](https://developer.android.com/reference/android/content/ComponentCallbacks2#onTrimMemory(int))
 * - [ActivityManager.MemoryInfo](https://developer.android.com/reference/android/app/ActivityManager#getMemoryInfo(android.app.ActivityManager.MemoryInfo))
 * - [ApplicationExitInfo.REASON_LOW_MEMORY](https://developer.android.com/reference/android/app/ApplicationExitInfo#REASON_LOW_MEMORY)
 */
interface SystemMemoryManager {
    /**
     * Evaluates whether the device has sufficient available RAM to safely initialize an on-device model.
     *
     * Official Guide:
     * - https://developer.android.com/reference/android/app/ActivityManager#getMemoryInfo(android.app.ActivityManager.MemoryInfo)
     *
     * @param requiredHeadroomBytes Minimum recommended available RAM in bytes (default: 1.2 GB).
     */
    fun checkMemoryHeadroom(requiredHeadroomBytes: Long = DEFAULT_MIN_HEADROOM_BYTES): MemoryHeadroomResult

    /**
     * Handles OS memory trimming signals ([ComponentCallbacks2.onTrimMemory]).
     *
     * Official Guide:
     * - https://developer.android.com/reference/android/content/ComponentCallbacks2#onTrimMemory(int)
     */
    fun onTrimMemory(level: Int)

    /**
     * Handles system-wide critical low memory events ([ComponentCallbacks2.onLowMemory]).
     *
     * Official Guide:
     * - https://developer.android.com/reference/android/content/ComponentCallbacks#onLowMemory()
     */
    fun onLowMemory()

    /**
     * Registers memory callbacks and activity lifecycle listeners on the [application].
     *
     * Official Guide:
     * - https://developer.android.com/reference/android/app/Application#registerActivityLifecycleCallbacks(android.app.Application.ActivityLifecycleCallbacks)
     * - https://developer.android.com/reference/android/app/Application#registerComponentCallbacks(android.content.ComponentCallbacks)
     */
    fun register(application: Application)

    /**
     * Inspects historical process termination reasons (API 30+) to log LMK diagnoses.
     *
     * Official Guide:
     * - https://developer.android.com/reference/android/app/ApplicationExitInfo#REASON_LOW_MEMORY
     */
    fun checkHistoricalExitReasons()

    companion object {
        /**
         * Minimum recommended free RAM headroom (1.2 GB) required to safely initialize on-device models.
         * On-device LLMs allocate directly into unified native C++ and GPU memory; maintaining this
         * buffer prevents device thrashing and foreground process terminations.
         */
        const val DEFAULT_MIN_HEADROOM_BYTES: Long = 1_200_000_000L // 1.2 GB

        /**
         * Background idle grace duration before evicting model weights from native memory (3 minutes).
         * Allows quick app-switching (e.g. checking a notification or copying code) without reloading weights.
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

        /**
         * ComponentCallbacks2 listener for responding to memory pressure events.
         * See: https://developer.android.com/reference/android/content/ComponentCallbacks2
         */
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

        /**
         * ActivityLifecycleCallbacks listener for tracking foreground visibility.
         * When all activities stop (app backgrounded), starts the background eviction grace timer.
         * When any activity starts or resumes, immediately cancels the background eviction timer.
         * See: https://developer.android.com/reference/android/app/Application.ActivityLifecycleCallbacks
         */
        private val activityLifecycleCallbacks =
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    startedActivityCount++
                    Timber.d(
                        "SystemMemoryManager [LIFECYCLE_CHANGE]: Activity started (%s). Active count: %d",
                        activity.localClassName,
                        startedActivityCount,
                    )
                    cancelBackgroundEviction()
                }

                override fun onActivityStopped(activity: Activity) {
                    startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
                    Timber.d(
                        "SystemMemoryManager [LIFECYCLE_CHANGE]: Activity stopped (%s). Active count: %d",
                        activity.localClassName,
                        startedActivityCount,
                    )
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

        /**
         * Evaluates available RAM against the required headroom threshold.
         * Uses [ActivityManager.MemoryInfo] to read totalMem, availMem, and lowMemory.
         * See: https://developer.android.com/topic/performance/memory/manage-app-memory#CheckMemory
         */
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
                    "SystemMemoryManager [HEADROOM_CONSTRAINED]: Device memory is constrained " +
                        "(available=%.1f MB, required=%.1f MB, isLowMemory=%b)",
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
                Timber.d(
                    "SystemMemoryManager [HEADROOM_CHECK]: Memory headroom sufficient (available=%.1f MB, required=%.1f MB)",
                    availMemBytes / (1024f * 1024f),
                    requiredHeadroomBytes / (1024f * 1024f),
                )
                MemoryHeadroomResult.Sufficient
            }
        }

        /**
         * Handles system trimming signals.
         * - TRIM_MEMORY_UI_HIDDEN: App UI is no longer visible; start grace timer before model eviction.
         * - TRIM_MEMORY_RUNNING_CRITICAL / TRIM_MEMORY_BACKGROUND / etc: Immediate eviction to prevent LMK.
         * See: https://developer.android.com/reference/android/content/ComponentCallbacks2#onTrimMemory(int)
         */
        @Suppress("DEPRECATION")
        override fun onTrimMemory(level: Int) {
            Timber.i("SystemMemoryManager [ON_TRIM_MEMORY]: Received OS trim level=%d", level)
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
                    Timber.w(
                        "SystemMemoryManager [ON_TRIM_MEMORY]: Critical memory trim signal (level=%d). " +
                            "Releasing LLM native buffers immediately to avoid Low Memory Killer (LMK).",
                        level,
                    )
                    cancelBackgroundEviction()
                    evictModelMemory("TrimLevel=$level")
                }

                else -> {
                    Timber.d("SystemMemoryManager [ON_TRIM_MEMORY]: Non-critical trim level %d received", level)
                }
            }
        }

        /**
         * Handles system-wide critical low memory events.
         * See: https://developer.android.com/reference/android/content/ComponentCallbacks#onLowMemory()
         */
        override fun onLowMemory() {
            Timber.w(
                "SystemMemoryManager [ON_LOW_MEMORY]: System-wide low memory event triggered by OS. " +
                    "Releasing LLM native buffers immediately.",
            )
            cancelBackgroundEviction()
            evictModelMemory("onLowMemory")
        }

        /**
         * Registers memory and lifecycle callbacks on the application instance.
         */
        override fun register(application: Application) {
            application.registerComponentCallbacks(componentCallbacks)
            application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
            checkHistoricalExitReasons()
            Timber.d("SystemMemoryManager: Registered memory and activity lifecycle callbacks")
        }

        /**
         * Inspects recent process termination reasons using ApplicationExitInfo (API 30+).
         * Helps diagnose if the previous app run was killed by the OS Low Memory Killer (LMK).
         * See: https://developer.android.com/reference/android/app/ApplicationExitInfo#REASON_LOW_MEMORY
         */
        override fun checkHistoricalExitReasons() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val exitReasons = activityManager?.getHistoricalProcessExitReasons(context.packageName, 0, 3)
                    exitReasons?.firstOrNull()?.let { lastExit ->
                        when (lastExit.reason) {
                            android.app.ApplicationExitInfo.REASON_LOW_MEMORY -> {
                                Timber.w(
                                    "SystemMemoryManager [LMK_DIAGNOSTIC]: Previous process run was killed by Low Memory Killer (LMK). " +
                                        "PSS: %.1f MB, RSS: %.1f MB, Description: %s",
                                    lastExit.pss / (1024f * 1024f),
                                    lastExit.rss / (1024f * 1024f),
                                    lastExit.description ?: "None",
                                )
                            }

                            android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> {
                                Timber.w(
                                    "SystemMemoryManager [CRASH_DIAGNOSTIC]: Previous process run had native crash. Description: %s",
                                    lastExit.description ?: "None",
                                )
                            }

                            else -> {
                                Timber.d(
                                    "SystemMemoryManager [EXIT_DIAGNOSTIC]: Previous process exit reason code: %d",
                                    lastExit.reason,
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "SystemMemoryManager [EXIT_DIAGNOSTIC]: Could not retrieve historical process exit reasons")
                }
            }
        }

        /**
         * Schedules background eviction after [backgroundEvictionDelayMs].
         */
        private fun scheduleBackgroundEviction() {
            if (!llmEngine.isInitialized()) {
                Timber.d("SystemMemoryManager [TRIM_TIMER_SKIP]: Engine not loaded in memory, skipping eviction schedule")
                return
            }
            cancelBackgroundEviction()
            Timber.i(
                "SystemMemoryManager [TRIM_TIMER_START]: App UI is in background. " +
                    "Scheduling native LLM memory eviction in %d ms (grace period)",
                backgroundEvictionDelayMs,
            )
            backgroundEvictionJob =
                scope.launch {
                    delay(backgroundEvictionDelayMs)
                    evictModelMemory("BackgroundIdleTimeout")
                }
        }

        /**
         * Cancels any active background eviction timer when returning to the foreground.
         */
        private fun cancelBackgroundEviction() {
            if (backgroundEvictionJob?.isActive == true) {
                Timber.i("SystemMemoryManager [TRIM_TIMER_CANCEL]: App returned to foreground, cancelling background eviction")
                backgroundEvictionJob?.cancel()
            }
            backgroundEvictionJob = null
        }

        /**
         * Invokes [LlmEngine.cleanup] to deallocate native C++ tensors and unified GPU OpenCL buffers.
         */
        private fun evictModelMemory(triggerReason: String) {
            if (llmEngine.isInitialized()) {
                Timber.i(
                    "SystemMemoryManager [EVICT_START]: Releasing native model buffers and accelerator sessions (trigger=%s)",
                    triggerReason,
                )
                try {
                    llmEngine.cleanup()
                    Timber.i(
                        "SystemMemoryManager [EVICT_SUCCESS]: Native buffers successfully released back to OS (trigger=%s)",
                        triggerReason,
                    )
                } catch (e: Exception) {
                    Timber.e(
                        e,
                        "SystemMemoryManager [EVICT_ERROR]: Failed to cleanup LLM engine (trigger=%s)",
                        triggerReason,
                    )
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
