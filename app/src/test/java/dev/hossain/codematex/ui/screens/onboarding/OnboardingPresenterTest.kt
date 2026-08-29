package dev.hossain.codematex.ui.screens.onboarding

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.repository.FakeModelRepository
import dev.hossain.codematex.data.repository.FakeUserPreferencesStore
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import dev.hossain.codematex.ui.screens.home.HomeScreen
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class OnboardingPresenterTest {
    private lateinit var fakeNavigator: FakeNavigator
    private lateinit var fakeUserPreferencesStore: FakeUserPreferencesStore
    private lateinit var fakeModelRepository: FakeModelRepository

    @Before
    fun setUp() {
        fakeNavigator = FakeNavigator(OnboardingScreen)
        fakeUserPreferencesStore = FakeUserPreferencesStore(initialOnboardingCompleted = false)
        fakeModelRepository = FakeModelRepository()
    }

    private fun createPresenter(
        modelRepo: FakeModelRepository = fakeModelRepository,
        prefsStore: FakeUserPreferencesStore = fakeUserPreferencesStore,
    ): OnboardingPresenter =
        OnboardingPresenter(
            navigator = fakeNavigator,
            screen = OnboardingScreen,
            userPreferencesStore = prefsStore,
            modelRepository = modelRepo,
        )

    @Test
    fun `given initial presentation then emits default content state on page 0`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = awaitItem() as OnboardingScreen.State.Content
                assertThat(state.currentPage).isEqualTo(0)
                assertThat(state.pageCount).isEqualTo(4)
                assertThat(state.hasDownloadedModel).isFalse()
            }
        }

    @Test
    fun `given PageChanged event then updates currentPage`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val initialState = awaitItem() as OnboardingScreen.State.Content
                assertThat(initialState.currentPage).isEqualTo(0)

                initialState.eventSink(OnboardingScreen.Event.PageChanged(2))
                val updatedState = awaitItem() as OnboardingScreen.State.Content
                assertThat(updatedState.currentPage).isEqualTo(2)
            }
        }

    @Test
    fun `given NextClicked event when on intermediate page then advances to next page`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val initialState = awaitItem() as OnboardingScreen.State.Content
                assertThat(initialState.currentPage).isEqualTo(0)

                initialState.eventSink(OnboardingScreen.Event.NextClicked)
                val secondPage = awaitItem() as OnboardingScreen.State.Content
                assertThat(secondPage.currentPage).isEqualTo(1)

                secondPage.eventSink(OnboardingScreen.Event.NextClicked)
                val thirdPage = awaitItem() as OnboardingScreen.State.Content
                assertThat(thirdPage.currentPage).isEqualTo(2)
            }
        }

    @Test
    fun `given SkipClicked event when no model downloaded then sets completed and navigates to ModelPicker`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = awaitItem() as OnboardingScreen.State.Content
                state.eventSink(OnboardingScreen.Event.SkipClicked)

                assertThat(fakeUserPreferencesStore.isOnboardingCompleted()).isTrue()
                val nextScreen = fakeNavigator.awaitNextScreen()
                assertThat(nextScreen).isEqualTo(ModelPickerScreen)
            }
        }

    @Test
    fun `given GetStartedClicked event when model already downloaded then sets completed and resets to HomeScreen`() =
        runTest {
            val downloadedModel = testModel(id = "gemma-2b", downloadStatus = DownloadStatus.DOWNLOADED)
            val modelRepo = FakeModelRepository(availableModels = listOf(downloadedModel), selectedModel = downloadedModel)
            val presenter = createPresenter(modelRepo = modelRepo)

            presenter.test {
                val state = awaitItem() as OnboardingScreen.State.Content
                assertThat(state.hasDownloadedModel).isTrue()

                state.eventSink(OnboardingScreen.Event.GetStartedClicked)

                assertThat(fakeUserPreferencesStore.isOnboardingCompleted()).isTrue()
                val resetRootEvent = fakeNavigator.awaitResetRoot()
                assertThat(resetRootEvent.newRoot).isEqualTo(HomeScreen)
            }
        }
}
