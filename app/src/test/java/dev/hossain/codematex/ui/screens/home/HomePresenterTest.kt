package dev.hossain.codematex.ui.screens.home

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.repository.FakeChatSessionRepository
import dev.hossain.codematex.data.repository.FakeLearningRepository
import dev.hossain.codematex.data.repository.FakeModelRepository
import dev.hossain.codematex.system.HardwareEligibility
import dev.hossain.codematex.system.HardwareEligibilityChecker
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import dev.hossain.codematex.ui.screens.chat.ChatScreen
import dev.hossain.codematex.ui.screens.chatsessions.SessionHistoryScreen
import dev.hossain.codematex.ui.screens.onboarding.OnboardingScreen
import dev.hossain.codematex.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeHardwareEligibilityChecker(
    var result: HardwareEligibility = HardwareEligibility.Eligible,
) : HardwareEligibilityChecker {
    override fun checkEligibility(): HardwareEligibility = result
}

/**
 * Unit tests for [HomePresenter].
 */
class HomePresenterTest {
    private val fakeSessionRepo = FakeChatSessionRepository()
    private val fakeModelRepo = FakeModelRepository()
    private val fakeLearningRepo =
        FakeLearningRepository(
            courses =
                listOf(
                    LearningCourse(
                        id = "kotlin-foundations",
                        language = "Kotlin",
                        title = "Kotlin Foundations",
                        description = "Learn Kotlin",
                        version = 1,
                        chapters = emptyList(),
                    ),
                    LearningCourse(
                        id = "rust-foundations",
                        language = "Rust",
                        title = "Rust Foundations",
                        description = "Learn Rust",
                        version = 1,
                        chapters = emptyList(),
                    ),
                ),
        )

    @Test
    fun `given device is eligible - emits success state with topics and available courses`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val presenter =
                HomePresenter(
                    navigator = navigator,
                    screen = HomeScreen,
                    sessionRepository = fakeSessionRepo,
                    modelRepository = fakeModelRepo,
                    hardwareEligibilityChecker = FakeHardwareEligibilityChecker(HardwareEligibility.Eligible),
                    learningRepository = fakeLearningRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                assertThat(state.topics).containsExactlyElementsIn(CodingTopic.selectableEntries).inOrder()
                assertThat(state.topicsWithCourses).containsExactly(CodingTopic.KOTLIN, CodingTopic.RUST)
            }
        }

    @Test
    fun `given device is ineligible - emits ineligible device state and dismiss transitions to success`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val ineligible =
                HardwareEligibility.Ineligible(
                    reason = "Low RAM",
                    detectedRamGb = 5.5,
                    minRequiredRamGb = 8.0,
                    is64BitSupported = true,
                )
            val presenter =
                HomePresenter(
                    navigator = navigator,
                    screen = HomeScreen,
                    sessionRepository = fakeSessionRepo,
                    modelRepository = fakeModelRepo,
                    hardwareEligibilityChecker = FakeHardwareEligibilityChecker(ineligible),
                    learningRepository = fakeLearningRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.IneligibleDevice
                assertThat(state.reason).isEqualTo("Low RAM")
                assertThat(state.detectedRamGb).isWithin(0.01).of(5.5)

                // Dismiss warning
                state.eventSink(HomeScreen.Event.DismissIneligibilityWarning)
                val successState = expectMostRecentItem() as HomeScreen.State.Success
                assertThat(successState.topics).containsExactlyElementsIn(CodingTopic.selectableEntries).inOrder()
            }
        }

    @Test
    fun `given topic selected event - navigates to chat screen`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val presenter =
                HomePresenter(
                    navigator = navigator,
                    screen = HomeScreen,
                    sessionRepository = fakeSessionRepo,
                    modelRepository = fakeModelRepo,
                    hardwareEligibilityChecker = FakeHardwareEligibilityChecker(HardwareEligibility.Eligible),
                    learningRepository = fakeLearningRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.TopicSelected(CodingTopic.KOTLIN))
                assertThat(navigator.awaitNextScreen()).isEqualTo(ChatScreen(CodingTopic.KOTLIN))
            }
        }

    @Test
    fun `given manage models event - navigates to model picker screen`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val presenter =
                HomePresenter(
                    navigator = navigator,
                    screen = HomeScreen,
                    sessionRepository = fakeSessionRepo,
                    modelRepository = fakeModelRepo,
                    hardwareEligibilityChecker = FakeHardwareEligibilityChecker(HardwareEligibility.Eligible),
                    learningRepository = fakeLearningRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.ManageModels)
                assertThat(navigator.awaitNextScreen()).isEqualTo(ModelPickerScreen)
            }
        }

    @Test
    fun `given view all sessions event - navigates to session history screen`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val presenter =
                HomePresenter(
                    navigator = navigator,
                    screen = HomeScreen,
                    sessionRepository = fakeSessionRepo,
                    modelRepository = fakeModelRepo,
                    hardwareEligibilityChecker = FakeHardwareEligibilityChecker(HardwareEligibility.Eligible),
                    learningRepository = fakeLearningRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.ViewAllSessions)
                assertThat(navigator.awaitNextScreen()).isEqualTo(SessionHistoryScreen)
            }
        }

    @Test
    fun `given app tour event - navigates to onboarding screen`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val presenter =
                HomePresenter(
                    navigator = navigator,
                    screen = HomeScreen,
                    sessionRepository = fakeSessionRepo,
                    modelRepository = fakeModelRepo,
                    hardwareEligibilityChecker = FakeHardwareEligibilityChecker(HardwareEligibility.Eligible),
                    learningRepository = fakeLearningRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.AppTour)
                assertThat(navigator.awaitNextScreen()).isEqualTo(OnboardingScreen)
            }
        }

    @Test
    fun `given open settings event - navigates to settings screen`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val presenter =
                HomePresenter(
                    navigator = navigator,
                    screen = HomeScreen,
                    sessionRepository = fakeSessionRepo,
                    modelRepository = fakeModelRepo,
                    hardwareEligibilityChecker = FakeHardwareEligibilityChecker(HardwareEligibility.Eligible),
                    learningRepository = fakeLearningRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.OpenSettings)
                assertThat(navigator.awaitNextScreen()).isEqualTo(SettingsScreen)
            }
        }
}
