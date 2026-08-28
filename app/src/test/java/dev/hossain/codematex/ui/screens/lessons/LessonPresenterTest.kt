package dev.hossain.codematex.ui.screens.lessons

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.FakeLearningRepository
import dev.hossain.codematex.data.repository.GoCourseContent
import dev.hossain.codematex.data.repository.KotlinCourseContent
import dev.hossain.codematex.data.repository.PythonCourseContent
import dev.hossain.codematex.data.repository.RustCourseContent
import dev.hossain.codematex.data.repository.TypeScriptCourseContent
import dev.hossain.codematex.ui.screens.chat.ChatScreen
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LessonPresenterTest {
    private val fakeLearningRepository = FakeLearningRepository()

    @Test
    fun `given valid lesson - emits success state with lesson and course`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter =
                LessonPresenter(
                    navigator = navigator,
                    screen = LessonScreen("kotlin-hello-world"),
                    learningRepository = fakeLearningRepository,
                )

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
            val presenter =
                LessonPresenter(
                    navigator = navigator,
                    screen = LessonScreen("kotlin-hello-world"),
                    learningRepository = fakeLearningRepository,
                )

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
            val presenter =
                LessonPresenter(
                    navigator = navigator,
                    screen = LessonScreen("kotlin-hello-world"),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                state.eventSink(LessonScreen.Event.NextLesson)

                assertThat(navigator.awaitNextScreen()).isEqualTo(LessonScreen("kotlin-variables"))
            }
        }

    @Test
    fun `given ask ai event - navigates to ChatScreen with saveToHistory false and initialPrompt`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter =
                LessonPresenter(
                    navigator = navigator,
                    screen = LessonScreen("kotlin-hello-world"),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                state.eventSink(LessonScreen.Event.AskAi)

                val chatScreen = navigator.awaitNextScreen() as ChatScreen
                assertThat(chatScreen.topic).isEqualTo(CodingTopic.KOTLIN)
                assertThat(chatScreen.saveToHistory).isFalse()
                assertThat(chatScreen.initialPrompt).contains("Hello, Kotlin")
                assertThat(chatScreen.initialPrompt).contains("Kotlin Foundations")
            }
        }

    @Test
    fun `given python lesson - resolves python course and tutor topic`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("python-hello-world"))
            val presenter =
                LessonPresenter(
                    navigator = navigator,
                    screen = LessonScreen("python-hello-world"),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(state.course.id).isEqualTo(PythonCourseContent.COURSE_ID)
                state.eventSink(LessonScreen.Event.AskAi)

                val chatScreen = navigator.awaitNextScreen() as ChatScreen
                assertThat(chatScreen.topic).isEqualTo(CodingTopic.PYTHON)
                assertThat(chatScreen.initialPrompt).contains("Hello, Python")
                assertThat(chatScreen.initialPrompt).contains("Python Foundations")
            }
        }

    @Test
    fun `given typescript lesson - resolves typescript course and tutor topic`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("typescript-first-program"))
            val presenter =
                LessonPresenter(
                    navigator = navigator,
                    screen = LessonScreen("typescript-first-program"),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(state.course.id).isEqualTo(TypeScriptCourseContent.COURSE_ID)
                state.eventSink(LessonScreen.Event.AskAi)

                val chatScreen = navigator.awaitNextScreen() as ChatScreen
                assertThat(chatScreen.topic).isEqualTo(CodingTopic.TYPESCRIPT)
                assertThat(chatScreen.initialPrompt).contains("Your First TypeScript Program")
                assertThat(chatScreen.initialPrompt).contains("TypeScript Foundations")
            }
        }

    @Test
    fun `given go lesson - resolves go course and tutor topic`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("go-first-program"))
            val presenter =
                LessonPresenter(
                    navigator = navigator,
                    screen = LessonScreen("go-first-program"),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(state.course.id).isEqualTo(GoCourseContent.COURSE_ID)
                state.eventSink(LessonScreen.Event.AskAi)

                val chatScreen = navigator.awaitNextScreen() as ChatScreen
                assertThat(chatScreen.topic).isEqualTo(CodingTopic.GO)
                assertThat(chatScreen.initialPrompt).contains("Your First Go Program")
                assertThat(chatScreen.initialPrompt).contains("Go Foundations")
            }
        }

    @Test
    fun `given rust lesson - resolves rust course and tutor topic`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("rust-first-program"))
            val presenter =
                LessonPresenter(
                    navigator = navigator,
                    screen = LessonScreen("rust-first-program"),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonScreen.State.Success
                assertThat(state.course.id).isEqualTo(RustCourseContent.COURSE_ID)
                state.eventSink(LessonScreen.Event.AskAi)

                val chatScreen = navigator.awaitNextScreen() as ChatScreen
                assertThat(chatScreen.topic).isEqualTo(CodingTopic.RUST)
                assertThat(chatScreen.initialPrompt).contains("Your First Rust Program")
                assertThat(chatScreen.initialPrompt).contains("Rust Foundations")
            }
        }

    @Test
    fun `given back event - pops navigator`() =
        runTest {
            val navigator = FakeNavigator(LessonScreen("kotlin-hello-world"))
            val presenter =
                LessonPresenter(
                    navigator = navigator,
                    screen = LessonScreen("kotlin-hello-world"),
                    learningRepository = fakeLearningRepository,
                )

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
            val presenter =
                LessonPresenter(
                    navigator = navigator,
                    screen = LessonScreen("unknown-lesson"),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state).isInstanceOf(LessonScreen.State.NotFound::class.java)
            }
        }
}
