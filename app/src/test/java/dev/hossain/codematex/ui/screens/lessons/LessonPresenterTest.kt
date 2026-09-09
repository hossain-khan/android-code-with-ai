package dev.hossain.codematex.ui.screens.lessons

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.FakeLearningRepository
import dev.hossain.codematex.data.repository.course.GoCourseContent
import dev.hossain.codematex.data.repository.course.KotlinCourseContent
import dev.hossain.codematex.data.repository.course.PythonCourseContent
import dev.hossain.codematex.data.repository.course.RustCourseContent
import dev.hossain.codematex.data.repository.course.TypeScriptCourseContent
import dev.hossain.codematex.domain.runner.FakePlaygroundCodeRunner
import dev.hossain.codematex.domain.runner.PlaygroundExecutionResult
import dev.hossain.codematex.system.FakeNetworkMonitor
import dev.hossain.codematex.ui.screens.chat.ChatScreen
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LessonPresenterTest {
    private val fakeLearningRepository = FakeLearningRepository()
    private val fakePlaygroundRunner = FakePlaygroundCodeRunner()
    private val fakeNetworkMonitor = FakeNetworkMonitor()

    private fun createPresenter(
        navigator: FakeNavigator,
        screen: LessonScreen,
        networkMonitor: FakeNetworkMonitor = fakeNetworkMonitor,
    ): LessonPresenter =
        LessonPresenter(
            navigator = navigator,
            screen = screen,
            learningRepository = fakeLearningRepository,
            playgroundCodeRunner = fakePlaygroundRunner,
            networkMonitor = networkMonitor,
        )

    @Test
    fun `given valid lesson - emits success state with lesson and course`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter = createPresenter(navigator, LessonScreen("kotlin-hello-world"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(state.lesson.id).isEqualTo("kotlin-hello-world")
                assertThat(state.course.id).isEqualTo(KotlinCourseContent.COURSE_ID)
                assertThat(state.isCompleted).isFalse()
                assertThat(state.nextLessonId).isEqualTo("kotlin-variables")
            }
        }

    @Test
    fun `given mark completed event - updates lesson completion`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter = createPresenter(navigator, LessonScreen("kotlin-hello-world"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                state.eventSink(LessonScreen.Event.MarkCompleted)

                val updatedState = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(updatedState.isCompleted).isTrue()
            }
        }

    @Test
    fun `given next lesson event - navigates to next lesson screen`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter = createPresenter(navigator, LessonScreen("kotlin-hello-world"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                state.eventSink(LessonScreen.Event.NextLesson)

                assertThat(navigator.awaitNextScreen()).isEqualTo(LessonScreen("kotlin-variables"))
            }
        }

    @Test
    fun `given next lesson event with catalog backstack - pops current lesson before advancing`() =
        runTest {
            val navigator =
                FakeNavigator(
                    LessonCatalogScreen,
                    LessonScreen("kotlin-hello-world"),
                )

            val presenter = createPresenter(navigator, LessonScreen("kotlin-hello-world"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                state.eventSink(LessonScreen.Event.NextLesson)

                navigator.awaitPop()
                assertThat(navigator.awaitNextScreen()).isEqualTo(LessonScreen("kotlin-variables"))
            }
        }

    @Test
    fun `given ask ai event - navigates to ChatScreen with saveToHistory false and initialPrompt`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter = createPresenter(navigator, LessonScreen("kotlin-hello-world"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                state.eventSink(LessonScreen.Event.AskAi)

                val chatScreen = navigator.awaitNextScreen() as ChatScreen
                assertThat(chatScreen.topic).isEqualTo(CodingTopic.KOTLIN)
                assertThat(chatScreen.saveToHistory).isFalse()
                assertThat(chatScreen.showCourseBanner).isFalse()
                assertThat(chatScreen.initialPrompt).contains("Hello, Kotlin")
                assertThat(chatScreen.initialPrompt).contains("Kotlin Foundations")
            }
        }

    @Test
    fun `given python lesson - resolves python course and tutor topic`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("python-hello-world"))
            val presenter = createPresenter(navigator, LessonScreen("python-hello-world"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(state.course.id).isEqualTo(PythonCourseContent.COURSE_ID)
                state.eventSink(LessonScreen.Event.AskAi)

                val chatScreen = navigator.awaitNextScreen() as ChatScreen
                assertThat(chatScreen.topic).isEqualTo(CodingTopic.PYTHON)
                assertThat(chatScreen.showCourseBanner).isFalse()
                assertThat(chatScreen.initialPrompt).contains("Hello, Python")
                assertThat(chatScreen.initialPrompt).contains("Python Foundations")
            }
        }

    @Test
    fun `given typescript lesson - resolves typescript course and tutor topic`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("typescript-first-program"))
            val presenter = createPresenter(navigator, LessonScreen("typescript-first-program"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(state.course.id).isEqualTo(TypeScriptCourseContent.COURSE_ID)
                state.eventSink(LessonScreen.Event.AskAi)

                val chatScreen = navigator.awaitNextScreen() as ChatScreen
                assertThat(chatScreen.topic).isEqualTo(CodingTopic.TYPESCRIPT)
                assertThat(chatScreen.showCourseBanner).isFalse()
                assertThat(chatScreen.initialPrompt).contains("Your First TypeScript Program")
                assertThat(chatScreen.initialPrompt).contains("TypeScript Foundations")
            }
        }

    @Test
    fun `given go lesson - resolves go course and tutor topic`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("go-first-program"))
            val presenter = createPresenter(navigator, LessonScreen("go-first-program"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(state.course.id).isEqualTo(GoCourseContent.COURSE_ID)
                state.eventSink(LessonScreen.Event.AskAi)

                val chatScreen = navigator.awaitNextScreen() as ChatScreen
                assertThat(chatScreen.topic).isEqualTo(CodingTopic.GO)
                assertThat(chatScreen.showCourseBanner).isFalse()
                assertThat(chatScreen.initialPrompt).contains("Your First Go Program")
                assertThat(chatScreen.initialPrompt).contains("Go Foundations")
            }
        }

    @Test
    fun `given rust lesson - resolves rust course and tutor topic`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("rust-first-program"))
            val presenter = createPresenter(navigator, LessonScreen("rust-first-program"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(state.course.id).isEqualTo(RustCourseContent.COURSE_ID)
                state.eventSink(LessonScreen.Event.AskAi)

                val chatScreen = navigator.awaitNextScreen() as ChatScreen
                assertThat(chatScreen.topic).isEqualTo(CodingTopic.RUST)
                assertThat(chatScreen.showCourseBanner).isFalse()
                assertThat(chatScreen.initialPrompt).contains("Your First Rust Program")
                assertThat(chatScreen.initialPrompt).contains("Rust Foundations")
            }
        }

    @Test
    fun `given back event - pops navigator`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter = createPresenter(navigator, LessonScreen("kotlin-hello-world"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                state.eventSink(LessonScreen.Event.Back)

                navigator.awaitPop()
            }
        }

    @Test
    fun `given unknown lesson ID - emits not found state`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("unknown-lesson"))
            val presenter = createPresenter(navigator, LessonScreen("unknown-lesson"))

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state).isInstanceOf(LessonScreen.State.NotFound::class.java)
            }
        }

    @Test
    fun `given run snippet event with success - updates snippet execution state to success`() =
        runTest {
            fakePlaygroundRunner.resultToReturn = PlaygroundExecutionResult.Success("Hello from playground!\n")
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter = createPresenter(navigator, LessonScreen("kotlin-hello-world"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(state.snippetExecutionStates).isEmpty()

                state.eventSink(LessonScreen.Event.RunSnippet(0, "println!(\"hi\")", "rust"))

                val updatedState = expectMostRecentItem() as LessonScreen.State.Success
                val snippetState = updatedState.snippetExecutionStates[0]
                assertThat(snippetState).isInstanceOf(SnippetExecutionState.Success::class.java)
                val success = snippetState as SnippetExecutionState.Success
                assertThat(success.output).isEqualTo("Hello from playground!\n")
            }
        }

    @Test
    fun `given run snippet event with compilation error - updates state to compilation error`() =
        runTest {
            fakePlaygroundRunner.resultToReturn =
                PlaygroundExecutionResult.CompilationError("error[E0308]: mismatched types")
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter = createPresenter(navigator, LessonScreen("kotlin-hello-world"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                state.eventSink(LessonScreen.Event.RunSnippet(1, "bad code", "rust"))

                val updatedState = expectMostRecentItem() as LessonScreen.State.Success
                val snippetState = updatedState.snippetExecutionStates[1]
                assertThat(snippetState).isInstanceOf(SnippetExecutionState.CompilationError::class.java)
                val compError = snippetState as SnippetExecutionState.CompilationError
                assertThat(compError.diagnostic).contains("error[E0308]")
            }
        }

    @Test
    fun `given run snippet event with network error - updates state to error`() =
        runTest {
            fakePlaygroundRunner.resultToReturn =
                PlaygroundExecutionResult.NetworkError("Internet connection required")
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter = createPresenter(navigator, LessonScreen("kotlin-hello-world"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                state.eventSink(LessonScreen.Event.RunSnippet(0, "code", "rust"))

                val updatedState = expectMostRecentItem() as LessonScreen.State.Success
                val snippetState = updatedState.snippetExecutionStates[0]
                assertThat(snippetState).isInstanceOf(SnippetExecutionState.Error::class.java)
                val error = snippetState as SnippetExecutionState.Error
                assertThat(error.message).contains("Internet connection required")
            }
        }

    @Test
    fun `given dismiss snippet output event - clears snippet execution state`() =
        runTest {
            fakePlaygroundRunner.resultToReturn = PlaygroundExecutionResult.Success("Output")
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter = createPresenter(navigator, LessonScreen("kotlin-hello-world"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                state.eventSink(LessonScreen.Event.RunSnippet(0, "code", "rust"))

                val updatedState = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(updatedState.snippetExecutionStates[0]).isNotNull()

                updatedState.eventSink(LessonScreen.Event.DismissSnippetOutput(0))

                val finalState = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(finalState.snippetExecutionStates[0]).isNull()
            }
        }

    @Test
    fun `given initial state - emits isOnline as true`() =
        runTest {
            fakeNetworkMonitor.setOnline(true)
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter = createPresenter(navigator, LessonScreen("kotlin-hello-world"))

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(state.isOnline).isTrue()
            }
        }

    @Test
    fun `given network goes offline - emits state with isOnline false`() =
        runTest {
            fakeNetworkMonitor.setOnline(true)
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter = createPresenter(navigator, LessonScreen("kotlin-hello-world"))

            presenter.test {
                val initial = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(initial.isOnline).isTrue()

                fakeNetworkMonitor.setOnline(false)

                val updatedState = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(updatedState.isOnline).isFalse()
            }
        }
}
