package dev.hossain.codematex.system

import android.content.ComponentCallbacks2
import android.content.ContextWrapper
import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.repository.FakeUserPreferencesStore
import dev.hossain.codematex.runtime.FakeLlmEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SystemMemoryManagerTest {
    private lateinit var fakeEngine: FakeLlmEngine

    @Before
    fun setUp() {
        fakeEngine = FakeLlmEngine().apply { isInitializedValue = true }
    }

    @Test
    fun `given sufficient memory when checkMemoryHeadroom then returns Sufficient`() {
        val manager =
            SystemMemoryManagerImpl(
                context = ContextWrapper(null),
                llmEngine = fakeEngine,
                memoryInfoProvider = { Pair(2_000_000_000L, false) },
            )

        val result = manager.checkMemoryHeadroom(requiredHeadroomBytes = 1_200_000_000L)
        assertThat(result).isEqualTo(MemoryHeadroomResult.Sufficient)
    }

    @Test
    fun `given low memory flag active when checkMemoryHeadroom then returns Constrained`() {
        val manager =
            SystemMemoryManagerImpl(
                context = ContextWrapper(null),
                llmEngine = fakeEngine,
                memoryInfoProvider = { Pair(2_000_000_000L, true) },
            )

        val result = manager.checkMemoryHeadroom(requiredHeadroomBytes = 1_200_000_000L)
        assertThat(result).isInstanceOf(MemoryHeadroomResult.Constrained::class.java)
        val constrained = result as MemoryHeadroomResult.Constrained
        assertThat(constrained.isLowMemory).isTrue()
    }

    @Test
    fun `given available memory below threshold when checkMemoryHeadroom then returns Constrained`() {
        val manager =
            SystemMemoryManagerImpl(
                context = ContextWrapper(null),
                llmEngine = fakeEngine,
                memoryInfoProvider = { Pair(800_000_000L, false) },
            )

        val result = manager.checkMemoryHeadroom(requiredHeadroomBytes = 1_200_000_000L)
        assertThat(result).isInstanceOf(MemoryHeadroomResult.Constrained::class.java)
        val constrained = result as MemoryHeadroomResult.Constrained
        assertThat(constrained.availMemBytes).isEqualTo(800_000_000L)
        assertThat(constrained.requiredBytes).isEqualTo(1_200_000_000L)
    }

    @Test
    fun `given TRIM_MEMORY_UI_HIDDEN when delay passes then cleans up model memory`() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val evictionDelay = 1000L
            val manager =
                SystemMemoryManagerImpl(
                    context = ContextWrapper(null),
                    llmEngine = fakeEngine,
                    dispatcher = testDispatcher,
                    backgroundEvictionDelayMs = evictionDelay,
                )

            manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)

            // Before delay: cleanup should not have been called
            advanceTimeBy(500L)
            assertThat(fakeEngine.cleanupCalls).isEqualTo(0)

            // After delay: cleanup should be triggered
            advanceTimeBy(600L)
            advanceUntilIdle()
            assertThat(fakeEngine.cleanupCalls).isEqualTo(1)
        }

    @Test
    fun `given TRIM_MEMORY_UI_HIDDEN when activity resumed before delay then cancels cleanup`() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val evictionDelay = 1000L
            val manager =
                SystemMemoryManagerImpl(
                    context = ContextWrapper(null),
                    llmEngine = fakeEngine,
                    dispatcher = testDispatcher,
                    backgroundEvictionDelayMs = evictionDelay,
                )

            manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)

            advanceTimeBy(500L)
            // Activity resumed (user returns to app)
            manager.notifyActivityResumed()

            // Advance past the initial delay
            advanceTimeBy(1000L)
            assertThat(fakeEngine.cleanupCalls).isEqualTo(0)
        }

    @Test
    fun `given uninitialized engine when TRIM_MEMORY_UI_HIDDEN then does not schedule eviction`() =
        runTest {
            fakeEngine.isInitializedValue = false
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val manager =
                SystemMemoryManagerImpl(
                    context = ContextWrapper(null),
                    llmEngine = fakeEngine,
                    dispatcher = testDispatcher,
                    backgroundEvictionDelayMs = 1000L,
                )

            manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
            advanceTimeBy(2000L)
            assertThat(fakeEngine.cleanupCalls).isEqualTo(0)
        }

    @Test
    @Suppress("DEPRECATION")
    fun `given TRIM_MEMORY_RUNNING_CRITICAL then triggers immediate cleanup`() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val manager =
                SystemMemoryManagerImpl(
                    context = ContextWrapper(null),
                    llmEngine = fakeEngine,
                    dispatcher = testDispatcher,
                )

            manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
            advanceUntilIdle()
            assertThat(fakeEngine.cleanupCalls).isEqualTo(1)
        }

    @Test
    fun `given TRIM_MEMORY_BACKGROUND then triggers immediate cleanup`() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val manager =
                SystemMemoryManagerImpl(
                    context = ContextWrapper(null),
                    llmEngine = fakeEngine,
                    dispatcher = testDispatcher,
                )

            manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
            advanceUntilIdle()
            assertThat(fakeEngine.cleanupCalls).isEqualTo(1)
        }

    @Test
    fun `given onLowMemory then triggers immediate cleanup`() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val manager =
                SystemMemoryManagerImpl(
                    context = ContextWrapper(null),
                    llmEngine = fakeEngine,
                    dispatcher = testDispatcher,
                )

            manager.onLowMemory()
            advanceUntilIdle()
            assertThat(fakeEngine.cleanupCalls).isEqualTo(1)
        }

    @Test
    fun `given all activities stopped then schedules eviction after delay`() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val evictionDelay = 1000L
            val manager =
                SystemMemoryManagerImpl(
                    context = ContextWrapper(null),
                    llmEngine = fakeEngine,
                    dispatcher = testDispatcher,
                    backgroundEvictionDelayMs = evictionDelay,
                )

            manager.notifyActivityStarted()
            manager.notifyActivityStopped()

            advanceTimeBy(500L)
            assertThat(fakeEngine.cleanupCalls).isEqualTo(0)

            advanceTimeBy(600L)
            advanceUntilIdle()
            assertThat(fakeEngine.cleanupCalls).isEqualTo(1)
        }

    @Test
    fun `given userPreferencesStore with custom eviction minutes when initialized then updates backgroundEvictionDelayMs`() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val fakePreferences = FakeUserPreferencesStore(initialRamEvictionMinutes = 10)
            val manager =
                SystemMemoryManagerImpl(
                    context = ContextWrapper(null),
                    llmEngine = fakeEngine,
                    dispatcher = testDispatcher,
                    userPreferencesStore = fakePreferences,
                )

            advanceUntilIdle()
            assertThat(manager.getBackgroundEvictionDelayMs()).isEqualTo(10 * 60 * 1000L)
        }

    @Test
    fun `given running manager when user updates ramEvictionMinutes then dynamically updates eviction delay`() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val fakePreferences = FakeUserPreferencesStore(initialRamEvictionMinutes = 3)
            val manager =
                SystemMemoryManagerImpl(
                    context = ContextWrapper(null),
                    llmEngine = fakeEngine,
                    dispatcher = testDispatcher,
                    userPreferencesStore = fakePreferences,
                )

            advanceUntilIdle()
            assertThat(manager.getBackgroundEvictionDelayMs()).isEqualTo(3 * 60 * 1000L)

            fakePreferences.setRamEvictionMinutes(15)
            advanceUntilIdle()
            assertThat(manager.getBackgroundEvictionDelayMs()).isEqualTo(15 * 60 * 1000L)
        }

    @Test
    fun `given dynamic delay configured when activity stopped then evicts after dynamic delay`() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val fakePreferences = FakeUserPreferencesStore(initialRamEvictionMinutes = 5)
            val manager =
                SystemMemoryManagerImpl(
                    context = ContextWrapper(null),
                    llmEngine = fakeEngine,
                    dispatcher = testDispatcher,
                    userPreferencesStore = fakePreferences,
                )

            runCurrent()

            manager.notifyActivityStarted()
            manager.notifyActivityStopped()

            // 4 minutes pass: should not have evicted yet
            advanceTimeBy(4 * 60 * 1000L)
            assertThat(fakeEngine.cleanupCalls).isEqualTo(0)

            // 1 more minute passes (total 5 minutes): should evict
            advanceTimeBy(1 * 60 * 1000L)
            advanceUntilIdle()
            assertThat(fakeEngine.cleanupCalls).isEqualTo(1)
        }

    @Test
    fun `given eviction scheduled when ramEvictionMinutes updated then reschedules with new delay`() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val fakePreferences = FakeUserPreferencesStore(initialRamEvictionMinutes = 5)
            val manager =
                SystemMemoryManagerImpl(
                    context = ContextWrapper(null),
                    llmEngine = fakeEngine,
                    dispatcher = testDispatcher,
                    userPreferencesStore = fakePreferences,
                )

            runCurrent()

            manager.notifyActivityStarted()
            manager.notifyActivityStopped()

            // 2 minutes pass
            advanceTimeBy(2 * 60 * 1000L)
            assertThat(fakeEngine.cleanupCalls).isEqualTo(0)

            // User increases timeout to 10 minutes while eviction was scheduled
            fakePreferences.setRamEvictionMinutes(10)
            runCurrent()

            // Advance by 4 more minutes (total 6 minutes since stop, past initial 5 min): should not have evicted
            advanceTimeBy(4 * 60 * 1000L)
            assertThat(fakeEngine.cleanupCalls).isEqualTo(0)

            // Advance remaining time for the 10 min window: should evict
            advanceTimeBy(6 * 60 * 1000L)
            advanceUntilIdle()
            assertThat(fakeEngine.cleanupCalls).isEqualTo(1)
        }
}
