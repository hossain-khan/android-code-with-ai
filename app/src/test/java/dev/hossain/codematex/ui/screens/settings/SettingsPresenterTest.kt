package dev.hossain.codematex.ui.screens.settings

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.data.repository.FakeChatSessionRepository
import dev.hossain.codematex.data.repository.FakeModelDownloadPreferences
import dev.hossain.codematex.data.repository.FakeModelRepository
import dev.hossain.codematex.data.repository.FakeUserPreferencesStore
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.data.repository.testSession
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import dev.hossain.codematex.ui.screens.onboarding.OnboardingScreen
import dev.hossain.codematex.ui.screens.settings.code.CodeBlockSettingsScreen
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SettingsPresenterTest {
    private lateinit var fakeNavigator: FakeNavigator
    private lateinit var fakeUserPreferencesStore: FakeUserPreferencesStore
    private lateinit var fakeModelRepository: FakeModelRepository
    private lateinit var fakeChatSessionRepository: FakeChatSessionRepository
    private lateinit var fakeModelDownloadPreferences: FakeModelDownloadPreferences

    @Before
    fun setUp() {
        fakeNavigator = FakeNavigator(SettingsScreen)
        fakeUserPreferencesStore = FakeUserPreferencesStore()
        fakeModelRepository = FakeModelRepository()
        fakeChatSessionRepository = FakeChatSessionRepository()
        fakeModelDownloadPreferences = FakeModelDownloadPreferences()
    }

    private fun createPresenter(
        userPreferencesStore: FakeUserPreferencesStore = fakeUserPreferencesStore,
        modelRepository: FakeModelRepository = fakeModelRepository,
        chatSessionRepository: FakeChatSessionRepository = fakeChatSessionRepository,
        modelDownloadPreferences: FakeModelDownloadPreferences = fakeModelDownloadPreferences,
    ): SettingsPresenter =
        SettingsPresenter(
            navigator = fakeNavigator,
            screen = SettingsScreen,
            userPreferencesStore = userPreferencesStore,
            modelRepository = modelRepository,
            chatSessionRepository = chatSessionRepository,
            modelDownloadPreferences = modelDownloadPreferences,
        )

    @Test
    fun `given initial presentation then emits default settings content state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                assertThat(state.selectedPersona).isEqualTo(TutorPersona.SENIOR_ENGINEER)
                assertThat(state.isWifiOnlyDownload).isTrue()
                assertThat(state.showLineNumbers).isFalse()
                assertThat(state.hapticFeedbackEnabled).isTrue()
                assertThat(state.ramEvictionMinutes).isEqualTo(3)
                assertThat(state.storageUsedBytes).isEqualTo(0L)
                assertThat(state.downloadedModelCount).isEqualTo(0)
                assertThat(state.sessionCount).isEqualTo(0)
            }
        }

    @Test
    fun `given PersonaSelected event then updates persona in store and closes dialog`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                state.eventSink(SettingsScreen.Event.PersonaSelected(TutorPersona.BEGINNER_FRIENDLY))

                val updatedState = expectMostRecentItem() as SettingsScreen.State.Content
                assertThat(updatedState.selectedPersona).isEqualTo(TutorPersona.BEGINNER_FRIENDLY)
                assertThat(fakeUserPreferencesStore.getSelectedPersona()).isEqualTo(TutorPersona.BEGINNER_FRIENDLY)
                assertThat(updatedState.showPersonaDialog).isFalse()
            }
        }

    @Test
    fun `given WifiOnlyToggled event then updates store`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                state.eventSink(SettingsScreen.Event.WifiOnlyToggled(false))

                val updatedState = expectMostRecentItem() as SettingsScreen.State.Content
                assertThat(updatedState.isWifiOnlyDownload).isFalse()
                assertThat(fakeUserPreferencesStore.isWifiOnlyDownloadEnabled()).isFalse()
                assertThat(fakeModelDownloadPreferences.getDownloadOverWifiOnly()).isFalse()
            }
        }

    @Test
    fun `given LineNumbersToggled event then updates store`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                state.eventSink(SettingsScreen.Event.LineNumbersToggled(true))

                val updatedState = expectMostRecentItem() as SettingsScreen.State.Content
                assertThat(updatedState.showLineNumbers).isTrue()
                assertThat(fakeUserPreferencesStore.isShowLineNumbersEnabled()).isTrue()
            }
        }

    @Test
    fun `given HapticsToggled event then updates store`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                state.eventSink(SettingsScreen.Event.HapticsToggled(false))

                val updatedState = expectMostRecentItem() as SettingsScreen.State.Content
                assertThat(updatedState.hapticFeedbackEnabled).isFalse()
                assertThat(fakeUserPreferencesStore.isHapticFeedbackEnabled()).isFalse()
            }
        }

    @Test
    fun `given RamEvictionSelected event then updates store and closes dialog`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                state.eventSink(SettingsScreen.Event.RamEvictionSelected(5))

                val updatedState = expectMostRecentItem() as SettingsScreen.State.Content
                assertThat(updatedState.ramEvictionMinutes).isEqualTo(5)
                assertThat(fakeUserPreferencesStore.getRamEvictionMinutes()).isEqualTo(5)
                assertThat(updatedState.showRamEvictionDialog).isFalse()
            }
        }

    @Test
    fun `given DeveloperProfileClicked event then navigates to DeveloperProfileSettingsScreen`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                state.eventSink(SettingsScreen.Event.DeveloperProfileClicked)

                assertThat(
                    fakeNavigator.awaitNextScreen(),
                ).isEqualTo(dev.hossain.codematex.ui.screens.settings.profile.DeveloperProfileSettingsScreen)
            }
        }

    @Test
    fun `given CodeBlockSettingsClicked event then navigates to CodeBlockSettingsScreen`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                state.eventSink(SettingsScreen.Event.CodeBlockSettingsClicked)

                assertThat(fakeNavigator.awaitNextScreen()).isEqualTo(CodeBlockSettingsScreen)
            }
        }

    @Test
    fun `given OpenDebugScreen event then navigates to DebugScreen`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                state.eventSink(SettingsScreen.Event.OpenDebugScreen)

                assertThat(fakeNavigator.awaitNextScreen()).isEqualTo(dev.hossain.codematex.ui.screens.debug.DebugScreen)
            }
        }

    @Test
    fun `given ManageModelsClicked event then navigates to ModelPickerScreen`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                state.eventSink(SettingsScreen.Event.ManageModelsClicked)

                assertThat(fakeNavigator.awaitNextScreen()).isEqualTo(ModelPickerScreen)
            }
        }

    @Test
    fun `given ConfirmClearHistory event then clears all sessions in repository`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                state.eventSink(SettingsScreen.Event.ShowClearHistoryDialog(true))

                val dialogOpenState = awaitItem() as SettingsScreen.State.Content
                assertThat(dialogOpenState.showClearHistoryConfirmation).isTrue()

                dialogOpenState.eventSink(SettingsScreen.Event.ConfirmClearHistory)

                val dialogClosedState = awaitItem() as SettingsScreen.State.Content
                assertThat(dialogClosedState.showClearHistoryConfirmation).isFalse()
                assertThat(fakeChatSessionRepository.clearAllSessionsCalled).isTrue()
            }
        }

    @Test
    fun `given ReplayTourClicked event then navigates to OnboardingScreen`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                state.eventSink(SettingsScreen.Event.ReplayTourClicked)

                assertThat(fakeNavigator.awaitNextScreen()).isEqualTo(OnboardingScreen)
            }
        }

    @Test
    fun `given BackClicked event then pops navigator`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                state.eventSink(SettingsScreen.Event.BackClicked)

                fakeNavigator.awaitPop()
            }
        }

    @Test
    fun `given downloaded models and sessions then calculates storage and session counts`() =
        runTest {
            val model = testModel(id = "gemma-2b", downloadStatus = DownloadStatus.DOWNLOADED)
            val modelRepo = FakeModelRepository(availableModels = listOf(model))
            val sessionRepo = FakeChatSessionRepository(sessions = listOf(testSession(id = "s1"), testSession(id = "s2")))
            val presenter = createPresenter(modelRepository = modelRepo, chatSessionRepository = sessionRepo)

            presenter.test {
                val state = expectMostRecentItem() as SettingsScreen.State.Content
                assertThat(state.downloadedModelCount).isEqualTo(1)
                assertThat(state.storageUsedBytes).isEqualTo(model.sizeBytes)
                assertThat(state.sessionCount).isEqualTo(2)
            }
        }
}
