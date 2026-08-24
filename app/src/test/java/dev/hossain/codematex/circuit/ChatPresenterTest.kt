package dev.hossain.codematex.circuit

import android.content.Context
import android.content.ContextWrapper
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.circuit.overlay.ModelConfigStore
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.TutorPersona
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ChatPresenter].
 *
 * These tests verify that the presenter correctly renders [ChatScreen.State] from a
 * [ChatStateHolder] and delegates user events back to the holder.
 */
class ChatPresenterTest {
    private val fakeContext: Context = ContextWrapper(null)

    private val configStore = ModelConfigStore(fakeContext)
    private val fakeSessionRepo = FakeChatSessionRepository()
    private val fakeChatInferenceOrchestrator = FakeChatInferenceOrchestrator()
    private val fakeSystemStatsMonitor = FakeSystemStatsMonitor()

    private fun createStateHolderFactory(
        screen: ChatScreen,
        modelRepository: FakeModelRepository,
    ): DefaultChatStateHolder.Factory =
        object : DefaultChatStateHolder.Factory {
            override fun create(screen: ChatScreen): DefaultChatStateHolder =
                DefaultChatStateHolder(
                    screen = screen,
                    modelRepository = modelRepository,
                    sessionRepository = fakeSessionRepo,
                    configStore = configStore,
                    chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                )
        }

    private fun createPresenter(
        navigator: FakeNavigator,
        modelRepository: FakeModelRepository,
        screen: ChatScreen = ChatScreen(CodingTopic.KOTLIN),
    ): ChatPresenter =
        ChatPresenter(
            navigator = navigator,
            screen = screen,
            stateHolderFactory = createStateHolderFactory(screen, modelRepository),
            configStore = configStore,
        )

    @Test
    fun `given no downloaded models - emits no model selected with false`() =
        runTest {
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    modelRepository =
                        FakeModelRepository(
                            availableModels = listOf(testModel(downloadStatus = DownloadStatus.NOT_DOWNLOADED)),
                            selectedModel = null,
                        ),
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                assertFalse(state.hasDownloadedModels)
                assertEquals(CodingTopic.KOTLIN, state.topic)
            }
        }

    @Test
    fun `given models downloaded - emits no model selected with true`() =
        runTest {
            val navigator = FakeNavigator(ChatScreen(CodingTopic.ANDROID))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    modelRepository =
                        FakeModelRepository(
                            availableModels = listOf(testModel(downloadStatus = DownloadStatus.DOWNLOADED)),
                            selectedModel = null,
                        ),
                    screen = ChatScreen(CodingTopic.ANDROID),
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                assertTrue(state.hasDownloadedModels)
                assertEquals(CodingTopic.ANDROID, state.topic)
            }
        }

    @Test
    fun `given open model picker event - navigates to model picker screen`() =
        runTest {
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    modelRepository = FakeModelRepository(),
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                state.eventSink(ChatScreen.Event.OpenModelPicker)
                assertEquals(ModelPickerScreen, navigator.awaitNextScreen())
            }
        }

    @Test
    fun `given model is selected - emits active state with model details and default persona`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    modelRepository =
                        FakeModelRepository(
                            availableModels = listOf(model),
                            selectedModel = model,
                        ),
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                assertFalse(state.isPreparing)
                assertFalse(state.isGenerating)
                assertEquals(CodingTopic.KOTLIN, state.topic)
                assertEquals(TutorPersona.SENIOR_ENGINEER, state.persona)
                assertEquals(model.displayName, state.modelName)
            }
        }

    @Test
    fun `given select persona event - updates active persona via state holder`() =
        runTest {
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    modelRepository =
                        FakeModelRepository(
                            availableModels = listOf(testModel(downloadStatus = DownloadStatus.DOWNLOADED)),
                            selectedModel = testModel(downloadStatus = DownloadStatus.DOWNLOADED),
                        ),
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                state.eventSink(ChatScreen.Event.SelectPersona(TutorPersona.BEGINNER_FRIENDLY))

                val updatedState = expectMostRecentItem() as ChatScreen.State.Active
                assertEquals(TutorPersona.BEGINNER_FRIENDLY, updatedState.persona)
            }
        }

    @Test
    fun `given send message event - delegates to state holder`() =
        runTest {
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    modelRepository =
                        FakeModelRepository(
                            availableModels = listOf(testModel(downloadStatus = DownloadStatus.DOWNLOADED)),
                            selectedModel = testModel(downloadStatus = DownloadStatus.DOWNLOADED),
                        ),
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                state.eventSink(ChatScreen.Event.SendMessage("Hello"))
                assertTrue(fakeChatInferenceOrchestrator.sendMessageInputs.contains("Hello"))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given stop generation event - delegates to state holder`() =
        runTest {
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    modelRepository =
                        FakeModelRepository(
                            availableModels = listOf(testModel(downloadStatus = DownloadStatus.DOWNLOADED)),
                            selectedModel = testModel(downloadStatus = DownloadStatus.DOWNLOADED),
                        ),
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                state.eventSink(ChatScreen.Event.StopGeneration)
                assertEquals(1, fakeChatInferenceOrchestrator.stopCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given reset session event - delegates to state holder`() =
        runTest {
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    modelRepository =
                        FakeModelRepository(
                            availableModels = listOf(testModel(downloadStatus = DownloadStatus.DOWNLOADED)),
                            selectedModel = testModel(downloadStatus = DownloadStatus.DOWNLOADED),
                        ),
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                state.eventSink(ChatScreen.Event.ResetSession)
                assertTrue(fakeChatInferenceOrchestrator.resetConversationTopics.contains(CodingTopic.KOTLIN))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given retry event - triggers model reinitialization`() =
        runTest {
            fakeChatInferenceOrchestrator.initializeResult = Result.failure(RuntimeException("init failed"))
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    modelRepository =
                        FakeModelRepository(
                            availableModels = listOf(testModel(downloadStatus = DownloadStatus.DOWNLOADED)),
                            selectedModel = testModel(downloadStatus = DownloadStatus.DOWNLOADED),
                        ),
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Error
                assertEquals("init failed", state.message)
                state.eventSink(ChatScreen.Event.Retry)
                // After retry, initialization is attempted again and fails with the same error.
                val retriedState = expectMostRecentItem() as ChatScreen.State.Error
                assertEquals("init failed", retriedState.message)
            }
        }

    @Test
    fun `given back event - navigates back`() =
        runTest {
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    modelRepository = FakeModelRepository(),
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                state.eventSink(ChatScreen.Event.Back)
                navigator.awaitPop()
            }
        }
}
