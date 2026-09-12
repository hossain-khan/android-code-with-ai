package dev.hossain.codematex.ui.screens.home

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.system.FakeHardwareEligibilityChecker
import dev.hossain.codematex.system.HardwareEligibility
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import dev.hossain.codematex.ui.screens.chat.ChatScreen
import dev.hossain.codematex.ui.screens.chatsessions.SessionHistoryScreen
import dev.hossain.codematex.ui.screens.lessons.ChapterScreen
import dev.hossain.codematex.ui.screens.lessons.LessonCatalogScreen
import dev.hossain.codematex.ui.screens.onboarding.OnboardingScreen
import dev.hossain.codematex.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for coordinator [HomePresenter].
 */
class HomePresenterTest {
    private fun createPresenter(
        navigator: FakeNavigator = FakeNavigator(HomeScreen),
        hardwareEligibility: HardwareEligibility = HardwareEligibility.Eligible,
    ): HomePresenter =
        HomePresenter(
            navigator = navigator,
            screen = HomeScreen,
            hardwareEligibilityChecker = FakeHardwareEligibilityChecker(hardwareEligibility),
        )

    @Test
    fun `given device is eligible - emits success state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state).isInstanceOf(HomeScreen.State.Success::class.java)
            }
        }

    @Test
    fun `given device is ineligible - emits ineligible device state and dismiss transitions to success`() =
        runTest {
            val ineligible =
                HardwareEligibility.Ineligible(
                    reason = "Low RAM",
                    detectedRamGb = 5.5,
                    minRequiredRamGb = 8.0,
                    is64BitSupported = true,
                )
            val presenter = createPresenter(hardwareEligibility = ineligible)

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.IneligibleDevice
                assertThat(state.reason).isEqualTo("Low RAM")
                assertThat(state.detectedRamGb).isWithin(0.01).of(5.5)

                // Dismiss warning
                state.eventSink(HomeScreen.Event.DismissIneligibilityWarning)
                val successState = expectMostRecentItem()
                assertThat(successState).isInstanceOf(HomeScreen.State.Success::class.java)
            }
        }

    @Test
    fun `given topic selected event - navigates to chat screen`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val presenter = createPresenter(navigator = navigator)

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.TopicSelected(CodingTopic.KOTLIN))
                assertThat(navigator.awaitNextScreen()).isEqualTo(ChatScreen(CodingTopic.KOTLIN))
            }
        }

    @Test
    fun `given session selected event - navigates to chat screen with session id`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val presenter = createPresenter(navigator = navigator)

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.SessionSelected(CodingTopic.KOTLIN, "session-123"))
                assertThat(navigator.awaitNextScreen()).isEqualTo(
                    ChatScreen(topic = CodingTopic.KOTLIN, sessionId = "session-123"),
                )
            }
        }

    @Test
    fun `given manage models event - navigates to model picker screen`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val presenter = createPresenter(navigator = navigator)

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
            val presenter = createPresenter(navigator = navigator)

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
            val presenter = createPresenter(navigator = navigator)

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
            val presenter = createPresenter(navigator = navigator)

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.OpenSettings)
                assertThat(navigator.awaitNextScreen()).isEqualTo(SettingsScreen)
            }
        }

    @Test
    fun `given course clicked event - navigates to chapter screen`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val presenter = createPresenter(navigator = navigator)

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.CourseClicked("kotlin-foundations"))
                assertThat(navigator.awaitNextScreen()).isEqualTo(ChapterScreen("kotlin-foundations"))
            }
        }

    @Test
    fun `given guided lessons event - navigates to lesson catalog screen`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val presenter = createPresenter(navigator = navigator)

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.GuidedLessons)
                assertThat(navigator.awaitNextScreen()).isEqualTo(LessonCatalogScreen())
            }
        }
}
