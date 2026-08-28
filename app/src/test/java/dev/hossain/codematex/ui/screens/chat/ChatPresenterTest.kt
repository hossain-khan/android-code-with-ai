package dev.hossain.codematex.ui.screens.chat

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.ChatInferenceEvent
import dev.hossain.codematex.data.FakeChatInferenceOrchestrator
import dev.hossain.codematex.data.FakeSystemStatsMonitor
import dev.hossain.codematex.data.FakeTopicPromptProvider
import dev.hossain.codematex.data.TopicPromptProvider
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.hossain.codematex.data.repository.FakeChatSessionRepository
import dev.hossain.codematex.data.repository.FakeModelConfigStore
import dev.hossain.codematex.data.repository.FakeModelRepository
import dev.hossain.codematex.data.repository.FakeUserPreferencesStore
import dev.hossain.codematex.data.repository.ModelConfigStore
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.data.repository.UserPreferencesStore
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.system.SystemResourceStats
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [ChatPresenter].
 */
class ChatPresenterTest {
    private val configStore: ModelConfigStore = FakeModelConfigStore()
    private val fakeSessionRepo = FakeChatSessionRepository()
    private val fakeChatInferenceOrchestrator = FakeChatInferenceOrchestrator()
    private val fakeSystemStatsMonitor = FakeSystemStatsMonitor()
    private val fakeUserPreferencesStore = FakeUserPreferencesStore()
    private val fakeTopicPromptProvider = FakeTopicPromptProvider()

    private fun createPresenter(
        navigator: com.slack.circuit.runtime.Navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN)),
        screen: ChatScreen = ChatScreen(CodingTopic.KOTLIN),
        modelRepository: ModelRepository,
        sessionRepository: ChatSessionRepository = fakeSessionRepo,
        configStore: ModelConfigStore = this.configStore,
        userPreferencesStore: UserPreferencesStore = fakeUserPreferencesStore,
        chatInferenceOrchestrator: dev.hossain.codematex.data.ChatInferenceOrchestrator = fakeChatInferenceOrchestrator,
        systemStatsMonitor: dev.hossain.codematex.data.SystemStatsMonitor = fakeSystemStatsMonitor,
        topicPromptProvider: TopicPromptProvider = fakeTopicPromptProvider,
    ): ChatPresenter =
        ChatPresenter(
            navigator = navigator,
            screen = screen,
            modelRepository = modelRepository,
            sessionRepository = sessionRepository,
            configStore = configStore,
            userPreferencesStore = userPreferencesStore,
            chatInferenceOrchestrator = chatInferenceOrchestrator,
            systemStatsMonitor = systemStatsMonitor,
            topicPromptProvider = topicPromptProvider,
        )

    @Test
    fun `given no downloaded models - emits no model selected with false`() =
        runTest {
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(testModel(downloadStatus = DownloadStatus.NOT_DOWNLOADED)),
                    selectedModel = null,
                )
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                assertThat(state.hasDownloadedModels).isFalse()
                assertThat(state.topic).isEqualTo(CodingTopic.KOTLIN)
            }
        }

    @Test
    fun `given models downloaded - emits no model selected with true`() =
        runTest {
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(testModel(downloadStatus = DownloadStatus.DOWNLOADED)),
                    selectedModel = null,
                )
            val navigator = FakeNavigator(ChatScreen(CodingTopic.ANDROID))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.ANDROID),
                    modelRepository = fakeModelRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                assertThat(state.hasDownloadedModels).isTrue()
                assertThat(state.topic).isEqualTo(CodingTopic.ANDROID)
            }
        }

    @Test
    fun `given open model picker event - navigates to model picker screen`() =
        runTest {
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = emptyList(),
                    selectedModel = null,
                )
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                state.eventSink(ChatScreen.Event.OpenModelPicker)
                assertThat(navigator.awaitNextScreen()).isEqualTo(ModelPickerScreen)
            }
        }

    @Test
    fun `given model is selected - emits active state with model details and default persona`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(state.isPreparing).isFalse()
                assertThat(state.isGenerating).isFalse()
                assertThat(state.topic).isEqualTo(CodingTopic.KOTLIN)
                assertThat(state.persona).isEqualTo(TutorPersona.SENIOR_ENGINEER)
            }
        }

    @Test
    fun `given stored persona in user preferences - initializes active state with stored persona`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val customPreferencesStore = FakeUserPreferencesStore(initialSelectedPersona = TutorPersona.INTERVIEW_COACH)
            val navigator = FakeNavigator(ChatScreen(CodingTopic.PYTHON))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.PYTHON),
                    modelRepository = fakeModelRepo,
                    userPreferencesStore = customPreferencesStore,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(state.topic).isEqualTo(CodingTopic.PYTHON)
                assertThat(state.persona).isEqualTo(TutorPersona.INTERVIEW_COACH)
            }
        }

    @Test
    fun `given select persona event - updates active persona, resets engine, and persists to user preferences`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val preferencesStore = FakeUserPreferencesStore(initialSelectedPersona = TutorPersona.SENIOR_ENGINEER)
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    userPreferencesStore = preferencesStore,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                state.eventSink(ChatScreen.Event.SelectPersona(TutorPersona.BEGINNER_FRIENDLY))

                val updatedState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(updatedState.persona).isEqualTo(TutorPersona.BEGINNER_FRIENDLY)
                assertThat(preferencesStore.getSelectedPersona()).isEqualTo(TutorPersona.BEGINNER_FRIENDLY)
                assertThat(fakeChatInferenceOrchestrator.resetConversationPersonas).contains(TutorPersona.BEGINNER_FRIENDLY)
            }
        }

    @Test
    fun `given system stats monitor emits metrics - emits active state with systemResourceStats`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val customStatsMonitor = FakeSystemStatsMonitor()
            customStatsMonitor.resourceStatsToEmit =
                listOf(
                    SystemResourceStats(
                        cpuPercent = 45f,
                        ramUsedGb = 3.5f,
                        ramTotalGb = 8.0f,
                    ),
                )

            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            fakeOrchestrator.messageEvents = listOf(ChatInferenceEvent.Token("Response"))
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))

            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    chatInferenceOrchestrator = fakeOrchestrator,
                    systemStatsMonitor = customStatsMonitor,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                state.eventSink(ChatScreen.Event.SendMessage("Explain ViewModel"))

                val generatingState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(generatingState.systemResourceStats?.cpuPercent ?: 0f).isWithin(0.01f).of(45f)
                assertThat(generatingState.systemResourceStats?.ramUsedGb ?: 0f).isWithin(0.01f).of(3.5f)
                assertThat(generatingState.systemResourceStats?.ramTotalGb ?: 0f).isWithin(0.01f).of(8.0f)
            }
        }

    @Test
    fun `given resumed session - reset clears messages and sessionId and next message creates new session`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val resumedSessionId = "resumed-session-123"
            val existingMessages = listOf(ChatMessage.User("Prior question"), ChatMessage.Agent("Prior answer"))
            val sessionRepo = FakeChatSessionRepository(messages = existingMessages)

            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            fakeOrchestrator.initializeResult = Result.success(existingMessages)
            fakeOrchestrator.messageEvents = listOf(ChatInferenceEvent.Token("Fresh answer"), ChatInferenceEvent.Done)

            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN, sessionId = resumedSessionId))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN, sessionId = resumedSessionId),
                    modelRepository = fakeModelRepo,
                    sessionRepository = sessionRepo,
                    chatInferenceOrchestrator = fakeOrchestrator,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(initialState.messages).hasSize(2)

                // Reset session
                initialState.eventSink(ChatScreen.Event.ResetSession)
                val resetState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(resetState.messages).isEmpty()

                // Next message should create a new conversation (passing sessionId = null to saveSession)
                resetState.eventSink(ChatScreen.Event.SendMessage("New turn"))
                expectMostRecentItem() // generation & save completes

                val lastSaved = sessionRepo.savedSessions.lastOrNull()
                assertThat(lastSaved).isNotNull()
                assertThat(lastSaved?.third).isNull()
            }
        }

    @Test
    fun `given persona switch with existing messages - calls switchPersona with history and updates persona`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val existingMessages = listOf(ChatMessage.User("Existing question"), ChatMessage.Agent("Existing answer"))
            val sessionRepo = FakeChatSessionRepository(messages = existingMessages)

            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            fakeOrchestrator.initializeResult = Result.success(existingMessages)

            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    sessionRepository = sessionRepo,
                    chatInferenceOrchestrator = fakeOrchestrator,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ChatScreen.State.Active
                initialState.eventSink(ChatScreen.Event.SelectPersona(TutorPersona.INTERVIEW_COACH))

                val updatedState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(updatedState.persona).isEqualTo(TutorPersona.INTERVIEW_COACH)

                val switchCall = fakeOrchestrator.switchPersonaCalls.lastOrNull()
                assertThat(switchCall).isNotNull()
                assertThat(switchCall?.persona).isEqualTo(TutorPersona.INTERVIEW_COACH)
                assertThat(switchCall?.messages?.any { it is ChatMessage.System }).isTrue()
            }
        }

    @Test
    fun `given persistence failure after inference done - preserves assistant response and exposes saveErrorMessage`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val sessionRepo = FakeChatSessionRepository()
            sessionRepo.saveException = java.io.IOException("Disk full")

            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            fakeOrchestrator.messageEvents = listOf(ChatInferenceEvent.Token("Generated code answer"), ChatInferenceEvent.Done)

            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    sessionRepository = sessionRepo,
                    chatInferenceOrchestrator = fakeOrchestrator,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ChatScreen.State.Active
                initialState.eventSink(ChatScreen.Event.SendMessage("Write quicksort"))

                val postDoneState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(postDoneState.isGenerating).isFalse()
                assertThat(postDoneState.saveErrorMessage).isEqualTo("Disk full")

                // Verify the generated assistant response was NOT deleted or replaced with an Error message
                val lastMessage = postDoneState.messages.lastOrNull()
                assertThat(lastMessage).isInstanceOf(ChatMessage.Agent::class.java)
                assertThat((lastMessage as ChatMessage.Agent).content).isEqualTo("Generated code answer")
                assertThat(lastMessage.isStreaming).isFalse()
            }
        }

    @Test
    fun `given save error - retry save persists conversation and clears error`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val sessionRepo = FakeChatSessionRepository()
            sessionRepo.saveException = java.io.IOException("Temporary DB lock")

            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            fakeOrchestrator.messageEvents = listOf(ChatInferenceEvent.Token("Response"), ChatInferenceEvent.Done)

            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    sessionRepository = sessionRepo,
                    chatInferenceOrchestrator = fakeOrchestrator,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ChatScreen.State.Active
                initialState.eventSink(ChatScreen.Event.SendMessage("Hello"))

                val failedState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(failedState.saveErrorMessage).isEqualTo("Temporary DB lock")

                // Database lock clears
                sessionRepo.saveException = null

                failedState.eventSink(ChatScreen.Event.RetrySave)
                val recoveredState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(recoveredState.saveErrorMessage).isNull()
                assertThat(sessionRepo.savedSessions).hasSize(1)
            }
        }

    @Test
    fun `given active generation - stop generation transitions streaming message to terminal state`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            fakeOrchestrator.messageEvents = listOf(ChatInferenceEvent.Token("Partial output"))

            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    chatInferenceOrchestrator = fakeOrchestrator,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ChatScreen.State.Active
                initialState.eventSink(ChatScreen.Event.SendMessage("Long question"))

                val streamingState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(streamingState.isGenerating).isTrue()

                streamingState.eventSink(ChatScreen.Event.StopGeneration)
                val stoppedState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(stoppedState.isGenerating).isFalse()
                val lastMessage = stoppedState.messages.lastOrNull() as? ChatMessage.Agent
                assertThat(lastMessage).isNotNull()
                assertThat(lastMessage?.isStreaming).isFalse()
                assertThat(fakeOrchestrator.stopCalls).isEqualTo(1)
            }
        }

    @Test
    fun `given SelectPersona event - updates persona, persists to store, and orchestrates persona switch`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            val userPrefsStore = FakeUserPreferencesStore(initialSelectedPersona = TutorPersona.SENIOR_ENGINEER)

            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    userPreferencesStore = userPrefsStore,
                    chatInferenceOrchestrator = fakeOrchestrator,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(initialState.persona).isEqualTo(TutorPersona.SENIOR_ENGINEER)

                initialState.eventSink(ChatScreen.Event.SelectPersona(TutorPersona.INTERVIEW_COACH))

                val updatedState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(updatedState.persona).isEqualTo(TutorPersona.INTERVIEW_COACH)
                assertThat(userPrefsStore.getSelectedPersona()).isEqualTo(TutorPersona.INTERVIEW_COACH)
            }
        }

    @Test
    fun `given model config update in store - configInfo is dynamically updated in active state`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val localConfigStore = FakeModelConfigStore()

            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    configStore = localConfigStore,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(initialState.configInfo).isEqualTo("Temp: 0.7, Top-K: 40, Top-P: 1.0")

                localConfigStore.setConfig(model.id, ModelConfig(temperature = 0.2f, topK = 10, topP = 0.8f, maxTokens = 512))

                val updatedState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(updatedState.configInfo).isEqualTo("Temp: 0.2, Top-K: 10, Top-P: 0.8")
            }
        }

    @Test
    fun `given active model with context window - emits contextStats with calculated used tokens`() =
        runTest {
            val model =
                testModel(
                    id = "litert-community/gemma-4-E2B-it-litert-lm",
                    downloadStatus = DownloadStatus.DOWNLOADED,
                ).copy(contextWindow = 8192)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val presenter =
                createPresenter(
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(state.contextStats).isNotNull()
                assertThat(state.contextStats?.maxTokens).isEqualTo(8192)
                assertThat(state.contextStats?.usedTokens ?: 0).isGreaterThan(0)
            }
        }

    @Test
    fun `given ephemeral session with saveToHistory false - does not save conversation to session repository`() =
        runTest {
            val model = testModel(downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val fakeSessionRepo = FakeChatSessionRepository()
            fakeChatInferenceOrchestrator.messageEvents =
                listOf(
                    ChatInferenceEvent.Token("Lesson response"),
                    ChatInferenceEvent.Done,
                )
            val presenter =
                createPresenter(
                    screen = ChatScreen(CodingTopic.KOTLIN, saveToHistory = false),
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                )

            presenter.test {
                val active = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(active.saveToHistory).isFalse()
                active.eventSink(ChatScreen.Event.SendMessage("Explain this lesson"))

                // Wait for generation to complete
                val completedState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(completedState.messages).isNotEmpty()
                assertThat(fakeSessionRepo.savedSessions).isEmpty()
            }
        }

    @Test
    fun `given screen with initialPrompt - automatically triggers send message`() =
        runTest {
            val model = testModel(downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val fakeSessionRepo = FakeChatSessionRepository()
            fakeChatInferenceOrchestrator.messageEvents =
                listOf(
                    ChatInferenceEvent.Token("Auto-response"),
                    ChatInferenceEvent.Done,
                )
            val presenter =
                createPresenter(
                    screen = ChatScreen(CodingTopic.KOTLIN, saveToHistory = false, initialPrompt = "Initial question"),
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                )

            presenter.test {
                // Should auto-send and produce messages
                val finalState = expectMostRecentItem() as ChatScreen.State.Active
                assertThat(finalState.messages.any { it is ChatMessage.User && it.content == "Initial question" }).isTrue()
            }
        }
}
