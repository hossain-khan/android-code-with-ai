package dev.hossain.codematex.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.data.repository.course.LearningRepository
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.HardwareEligibility
import dev.hossain.codematex.system.HardwareEligibilityChecker
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import dev.hossain.codematex.ui.screens.chat.ChatScreen
import dev.hossain.codematex.ui.screens.chatsessions.SessionHistoryScreen
import dev.hossain.codematex.ui.screens.lessons.ChapterScreen
import dev.hossain.codematex.ui.screens.lessons.LessonCatalogScreen
import dev.hossain.codematex.ui.screens.onboarding.OnboardingScreen
import dev.hossain.codematex.ui.screens.settings.SettingsScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber

@AssistedInject
class HomePresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: HomeScreen,
    private val sessionRepository: ChatSessionRepository,
    private val modelRepository: ModelRepository,
    private val hardwareEligibilityChecker: HardwareEligibilityChecker,
    private val learningRepository: LearningRepository,
    private val llmEngine: LlmEngine,
) : Presenter<HomeScreen.State> {
    @Composable
    override fun present(): HomeScreen.State {
        var recentSessions by rememberRetained { mutableStateOf<List<ChatSession>>(emptyList()) }
        var topicsWithCourses by rememberRetained { mutableStateOf<Set<CodingTopic>>(emptySet()) }
        var availableCourses by rememberRetained { mutableStateOf<List<LearningCourse>>(emptyList()) }
        var isLoading by rememberRetained { mutableStateOf(true) }
        var isWarningDismissed by rememberRetained { mutableStateOf(false) }

        val hardwareEligibility = remember { hardwareEligibilityChecker.checkEligibility() }
        val selectedModel = modelRepository.getSelectedModel()
        val hasDownloadedModel = selectedModel != null
        val selectedModelName = selectedModel?.displayName
        val isModelInMemory = llmEngine.isInitialized()
        val memoryBackend = llmEngine.getActiveBackend()?.name

        LaunchedEffect(Unit) {
            topicsWithCourses = learningRepository.getTopicsWithCourses()
            launch {
                learningRepository.getCourses().collect { courses ->
                    availableCourses = courses
                }
            }
            Timber.d("HomePresenter: Loading sessions")
            sessionRepository
                .getAllSessions()
                .catch {
                    Timber.e(it, "HomePresenter: Failed to load sessions")
                    isLoading = false
                }.collect { sessions ->
                    Timber.d("HomePresenter: Loaded ${sessions.size} sessions")
                    recentSessions = sessions.take(5)
                    isLoading = false
                }
        }

        val eventSink: (HomeScreen.Event) -> Unit = { event ->
            when (event) {
                is HomeScreen.Event.TopicSelected -> {
                    navigator.goTo(ChatScreen(topic = event.topic))
                }

                is HomeScreen.Event.SessionClicked -> {
                    val session = recentSessions.find { it.id == event.sessionId }
                    if (session != null) {
                        navigator.goTo(ChatScreen(topic = session.topic, sessionId = session.id))
                    }
                }

                is HomeScreen.Event.CourseClicked -> {
                    navigator.goTo(ChapterScreen(event.courseId))
                }

                HomeScreen.Event.ManageModels -> {
                    navigator.goTo(ModelPickerScreen)
                }

                HomeScreen.Event.GuidedLessons -> {
                    navigator.goTo(LessonCatalogScreen)
                }

                HomeScreen.Event.ViewAllSessions -> {
                    navigator.goTo(SessionHistoryScreen)
                }

                HomeScreen.Event.AppTour -> {
                    navigator.goTo(OnboardingScreen)
                }

                HomeScreen.Event.OpenSettings -> {
                    navigator.goTo(SettingsScreen)
                }

                HomeScreen.Event.DismissIneligibilityWarning -> {
                    isWarningDismissed = true
                }
            }
        }

        if (hardwareEligibility is HardwareEligibility.Ineligible && !isWarningDismissed) {
            return HomeScreen.State.IneligibleDevice(
                reason = hardwareEligibility.reason,
                detectedRamGb = hardwareEligibility.detectedRamGb,
                minRequiredRamGb = hardwareEligibility.minRequiredRamGb,
                is64BitSupported = hardwareEligibility.is64BitSupported,
                eventSink = eventSink,
            )
        }

        return if (isLoading) {
            HomeScreen.State.Loading
        } else {
            HomeScreen.State.Success(
                recentSessions = recentSessions,
                topics = CodingTopic.selectableEntries,
                hasDownloadedModel = hasDownloadedModel,
                selectedModelName = selectedModelName,
                isModelInMemory = isModelInMemory,
                memoryBackend = memoryBackend,
                topicsWithCourses = topicsWithCourses,
                availableCourses = availableCourses,
                eventSink = eventSink,
            )
        }
    }

    @CircuitInject(HomeScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: HomeScreen,
        ): HomePresenter
    }
}
