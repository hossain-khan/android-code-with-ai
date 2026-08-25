package dev.hossain.codematex.ui.screens.home

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.FakeChatSessionRepository
import dev.hossain.codematex.data.repository.FakeModelRepository
import dev.hossain.codematex.system.HardwareEligibility
import dev.hossain.codematex.system.HardwareEligibilityChecker
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import dev.hossain.codematex.ui.screens.chat.ChatScreen
import dev.hossain.codematex.ui.screens.chatsessions.SessionHistoryScreen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    @Test
    fun `given device is eligible - emits success state`() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val presenter =
                HomePresenter(
                    navigator = navigator,
                    screen = HomeScreen,
                    sessionRepository = fakeSessionRepo,
                    modelRepository = fakeModelRepo,
                    hardwareEligibilityChecker = FakeHardwareEligibilityChecker(HardwareEligibility.Eligible),
                )

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                assertEquals(CodingTopic.entries, state.topics)
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
                )

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.IneligibleDevice
                assertEquals("Low RAM", state.reason)
                assertEquals(5.5, state.detectedRamGb, 0.01)

                // Dismiss warning
                state.eventSink(HomeScreen.Event.DismissIneligibilityWarning)
                val successState = expectMostRecentItem() as HomeScreen.State.Success
                assertEquals(CodingTopic.entries, successState.topics)
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
                )

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.TopicSelected(CodingTopic.KOTLIN))
                assertEquals(ChatScreen(CodingTopic.KOTLIN), navigator.awaitNextScreen())
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
                )

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.ManageModels)
                assertEquals(ModelPickerScreen, navigator.awaitNextScreen())
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
                )

            presenter.test {
                val state = expectMostRecentItem() as HomeScreen.State.Success
                state.eventSink(HomeScreen.Event.ViewAllSessions)
                assertEquals(SessionHistoryScreen, navigator.awaitNextScreen())
            }
        }
}
