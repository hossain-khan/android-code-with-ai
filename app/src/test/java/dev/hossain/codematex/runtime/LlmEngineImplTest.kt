@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package dev.hossain.codematex.runtime

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ModelConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class LlmEngineImplTest {
    private lateinit var factory: FakeLlmEngineFactory
    private lateinit var engine: LlmEngineImpl

    @Before
    fun setUp() {
        factory = FakeLlmEngineFactory()
        engine = LlmEngineImpl(factory, UnconfinedTestDispatcher())
    }

    private fun runEngineTest(block: suspend TestScope.() -> Unit) = runTest(UnconfinedTestDispatcher()) { block() }

    @Test
    fun `initialize skips loading for dev null model path`() =
        runEngineTest {
            engine.initialize(
                modelPath = DEV_STUB_MODEL_PATH,
                backend = LlmEngine.Backend.GPU,
            )

            assertThat(factory.createSessionRequests).isEmpty()
            assertThat(engine.getActiveBackend()).isNull()
        }

    @Test
    fun `initialize creates session through factory and tracks active backend`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )

            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
                systemInstruction = "You are a helpful assistant",
                config = ModelConfig(maxTokens = 128),
            )

            assertThat(factory.createSessionRequests).hasSize(1)
            with(factory.createSessionRequests.single()) {
                assertThat(modelPath).isEqualTo("/data/model.bin")
                assertThat(preferredBackend).isEqualTo(LlmEngine.Backend.GPU)
                assertThat(systemInstruction).isEqualTo("You are a helpful assistant")
                assertThat(config.maxTokens).isEqualTo(128)
            }
            assertThat(engine.getActiveBackend()).isEqualTo(LlmEngine.Backend.GPU)
        }

    @Test
    fun `initialize reuses in-memory engine when same model path is used`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )

            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
                systemInstruction = "First prompt",
                config = ModelConfig(maxTokens = 128),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
                systemInstruction = "Second prompt",
                config = ModelConfig(maxTokens = 256),
            )

            assertThat(factory.createSessionRequests).hasSize(1)
            assertThat(fakeEngine.createdConversations).hasSize(1)
            val systemContent =
                fakeEngine.createdConversations
                    .single()
                    .config
                    ?.systemInstruction
                    ?.contents
                    ?.first()
                    as? com.google.ai.edge.litertlm.Content.Text
            assertThat(systemContent?.text).isEqualTo("Second prompt")
        }

    @Test(expected = IllegalStateException::class)
    fun `runInference throws IllegalStateException when engine is not initialized`() =
        runEngineTest {
            engine.runInference("Hello") { _, _ -> }
        }

    @Test
    fun `runInference streams tokens from conversation`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val emittedTokens = mutableListOf<Pair<String, Boolean>>()
            val job =
                launch {
                    engine.runInference("Hello") { partial, done ->
                        emittedTokens.add(partial to done)
                    }
                }

            val message = fakeConversation.sentMessages.single()
            assertThat(message.input).isEqualTo("Hello")
            message.callback.onMessage(textMessage("Hello"))
            message.callback.onMessage(textMessage(" world"))
            message.callback.onDone()
            job.join()

            assertThat(emittedTokens)
                .containsExactly(
                    "Hello" to false,
                    " world" to false,
                    "" to true,
                ).inOrder()
        }

    @Test
    fun `runInference recreates CPU session and throws BackendFailureException on hardware failure`() =
        runEngineTest {
            val gpuEngine = FakeInferenceEngine()
            val gpuConversation = FakeInferenceConversation()
            val cpuEngine = FakeInferenceEngine()
            val cpuConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = gpuEngine,
                    conversation = gpuConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            factory.addSession(
                factory.createFakeSession(
                    engine = cpuEngine,
                    conversation = cpuConversation,
                    backend = LlmEngine.Backend.CPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val job =
                launch {
                    try {
                        engine.runInference("Hello") { _, _ -> }
                    } catch (e: BackendFailureException) {
                        assertThat(e.failedBackend).isEqualTo(LlmEngine.Backend.GPU)
                    }
                }

            val gpuMessage = gpuConversation.sentMessages.single()
            gpuMessage.callback.onError(
                com.google.ai.edge.litertlm
                    .LiteRtLmJniException("GPU failed"),
            )
            job.join()

            assertThat(factory.createSessionRequests).hasSize(2)
            assertThat(factory.createSessionRequests[1].preferredBackend).isEqualTo(LlmEngine.Backend.CPU)
            assertThat(gpuEngine.closed).isTrue()
            assertThat(gpuConversation.closed).isTrue()
            assertThat(engine.getActiveBackend()).isEqualTo(LlmEngine.Backend.CPU)

            // A second runInference call now uses the CPU session.
            val emittedTokens = mutableListOf<Pair<String, Boolean>>()
            val cpuJob =
                launch {
                    engine.runInference("Hello") { partial, done ->
                        emittedTokens.add(partial to done)
                    }
                }
            val cpuMessage = cpuConversation.sentMessages.single()
            cpuMessage.callback.onMessage(textMessage("OK"))
            cpuMessage.callback.onDone()
            cpuJob.join()

            assertThat(emittedTokens).containsExactly("OK" to false, "" to true).inOrder()
        }

    @Test
    fun `runInference recreates GPU session after NPU failure`() =
        runEngineTest {
            val npuEngine = FakeInferenceEngine()
            val npuConversation = FakeInferenceConversation()
            val gpuEngine = FakeInferenceEngine()
            val gpuConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = npuEngine,
                    conversation = npuConversation,
                    backend = LlmEngine.Backend.NPU,
                ),
            )
            factory.addSession(
                factory.createFakeSession(
                    engine = gpuEngine,
                    conversation = gpuConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.NPU,
            )

            val job =
                launch {
                    try {
                        engine.runInference("Hello") { _, _ -> }
                    } catch (e: BackendFailureException) {
                        assertThat(e.failedBackend).isEqualTo(LlmEngine.Backend.NPU)
                    }
                }

            npuConversation.sentMessages
                .single()
                .callback
                .onError(
                    com.google.ai.edge.litertlm
                        .LiteRtLmJniException("NPU failed"),
                )
            job.join()

            assertThat(factory.fallbackSessionRequests).containsExactly(LlmEngine.Backend.NPU)
            assertThat(factory.createSessionRequests[1].preferredBackend).isEqualTo(LlmEngine.Backend.GPU)
            assertThat(engine.getActiveBackend()).isEqualTo(LlmEngine.Backend.GPU)
            assertThat(npuEngine.closed).isTrue()
            assertThat(npuConversation.closed).isTrue()
        }

    @Test
    fun `runInference throws BackendFailureException and recreates CPU session on backend failure`() =
        runEngineTest {
            val gpuEngine = FakeInferenceEngine()
            val gpuConversation = FakeInferenceConversation()
            val cpuEngine = FakeInferenceEngine()
            val cpuConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = gpuEngine,
                    conversation = gpuConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            factory.addSession(
                factory.createFakeSession(
                    engine = cpuEngine,
                    conversation = cpuConversation,
                    backend = LlmEngine.Backend.CPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val job =
                launch {
                    try {
                        engine.runInference("Hello") { _, _ -> }
                    } catch (e: BackendFailureException) {
                        assertThat(e.failedBackend).isEqualTo(LlmEngine.Backend.GPU)
                    }
                }

            gpuConversation.sentMessages
                .single()
                .callback
                .onError(
                    com.google.ai.edge.litertlm
                        .LiteRtLmJniException("GPU failed"),
                )
            job.join()

            assertThat(gpuEngine.closed).isTrue()
            assertThat(gpuConversation.closed).isTrue()
            assertThat(engine.getActiveBackend()).isEqualTo(LlmEngine.Backend.CPU)
        }

    @Test
    fun `runInference does not retry when CPU backend fails`() =
        runEngineTest {
            val cpuEngine = FakeInferenceEngine()
            val cpuConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = cpuEngine,
                    conversation = cpuConversation,
                    backend = LlmEngine.Backend.CPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.CPU,
            )

            val job =
                launch {
                    try {
                        engine.runInference("Hello") { _, _ -> }
                    } catch (e: BackendFailureException) {
                        assertThat(e.failedBackend).isEqualTo(LlmEngine.Backend.CPU)
                    }
                }

            cpuConversation.sentMessages
                .single()
                .callback
                .onError(
                    com.google.ai.edge.litertlm
                        .LiteRtLmJniException("CPU failed"),
                )
            job.join()

            assertThat(factory.createSessionRequests).hasSize(1)
        }

    @Test
    fun `runInference does not fall back on non-backend errors`() =
        runEngineTest {
            val gpuEngine = FakeInferenceEngine()
            val gpuConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = gpuEngine,
                    conversation = gpuConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val job =
                launch {
                    try {
                        engine.runInference("Hello") { _, _ -> }
                    } catch (e: RuntimeException) {
                        assertThat(e.message).isEqualTo("Programming error")
                    }
                }

            gpuConversation.sentMessages
                .single()
                .callback
                .onError(RuntimeException("Programming error"))
            job.join()

            assertThat(factory.createSessionRequests).hasSize(1)
            assertThat(engine.getActiveBackend()).isEqualTo(LlmEngine.Backend.GPU)
        }

    @Test
    fun `runInference does not fall back to CPU when inference is cancelled`() =
        runEngineTest {
            val gpuEngine = FakeInferenceEngine()
            val gpuConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = gpuEngine,
                    conversation = gpuConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val emittedTokens = mutableListOf<Pair<String, Boolean>>()
            val job =
                launch {
                    engine.runInference("Hello") { partial, done ->
                        emittedTokens.add(partial to done)
                    }
                }

            val gpuMessage = gpuConversation.sentMessages.single()
            gpuMessage.callback.onMessage(textMessage("Hello"))
            gpuMessage.callback.onError(java.util.concurrent.CancellationException("Task cancelled"))
            job.join()

            assertThat(factory.createSessionRequests).hasSize(1)
            assertThat(engine.getActiveBackend()).isEqualTo(LlmEngine.Backend.GPU)
            assertThat(emittedTokens).containsExactly("Hello" to false, "" to true).inOrder()
        }

    @Test
    fun `runInference rethrows when inference fails on CPU`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.CPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.CPU,
            )

            val error = RuntimeException("CPU failed")
            var caughtError: Throwable? = null
            val job =
                launch {
                    try {
                        engine.runInference("Hello") { _, _ -> }
                    } catch (e: Throwable) {
                        caughtError = e
                    }
                }

            fakeConversation.sentMessages
                .single()
                .callback
                .onError(error)
            job.join()

            assertThat(caughtError).isNotNull()
            assertThat(caughtError?.message).isEqualTo(error.message)
            assertThat(factory.createSessionRequests).hasSize(1)
        }

    @Test
    fun `stop cancels conversation`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            engine.stop()

            assertThat(fakeConversation.cancelled).isTrue()
        }

    @Test
    fun `resetConversation closes old conversation and creates new one`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            engine.resetConversation(
                systemInstruction = "New system prompt",
                config = ModelConfig(temperature = 0.5f, topK = 20, topP = 0.9f),
            )

            assertThat(fakeConversation.closed).isTrue()
            assertThat(fakeEngine.createdConversations).hasSize(1)
            val newConversation = fakeEngine.createdConversations.single()
            val newSystemContent =
                newConversation.config
                    ?.systemInstruction
                    ?.contents
                    ?.first()
                    as? com.google.ai.edge.litertlm.Content.Text
            assertThat(newSystemContent?.text).isEqualTo("New system prompt")
            assertThat(newConversation.config?.samplerConfig?.temperature ?: 0.0).isWithin(0.001).of(0.5)
            assertThat(newConversation.config?.samplerConfig?.topK).isEqualTo(20)
            assertThat(newConversation.config?.samplerConfig?.topP ?: 0.0).isWithin(0.001).of(0.9)
        }

    @Test
    fun `restoreHistory seeds context through conversation`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val messages =
                listOf(
                    ChatMessage.User("Hello"),
                    ChatMessage.Agent("Hi there"),
                )
            val job =
                launch {
                    engine.restoreHistory(messages)
                }

            val message = fakeConversation.sentMessages.single()
            assertThat(message.input).contains("Hello")
            assertThat(message.input).contains("Hi there")
            message.callback.onDone()
            job.join()
        }

    @Test
    fun `restoreHistory preserves chronological order of alternating user and agent turns`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val messages =
                listOf(
                    ChatMessage.User("User1"),
                    ChatMessage.Agent("Agent1"),
                    ChatMessage.User("User2"),
                    ChatMessage.Agent("Agent2"),
                    ChatMessage.System("ignored"),
                    ChatMessage.Error("ignored"),
                )
            val job =
                launch {
                    engine.restoreHistory(messages)
                }

            val message = fakeConversation.sentMessages.single()
            val user1Index = message.input.indexOf("User: User1")
            val agent1Index = message.input.indexOf("Assistant: Agent1")
            val user2Index = message.input.indexOf("User: User2")
            val agent2Index = message.input.indexOf("Assistant: Agent2")

            assertThat(user1Index).isAtLeast(0)
            assertThat(agent1Index).isAtLeast(0)
            assertThat(user2Index).isAtLeast(0)
            assertThat(agent2Index).isAtLeast(0)
            assertThat(user1Index).isLessThan(agent1Index)
            assertThat(agent1Index).isLessThan(user2Index)
            assertThat(user2Index).isLessThan(agent2Index)
            assertThat(message.input).doesNotContain("System")
            assertThat(message.input).doesNotContain("Error")

            message.callback.onDone()
            job.join()
        }

    @Test
    fun `restoreHistory falls back to CPU when seeding fails on hardware backend`() =
        runEngineTest {
            val gpuEngine = FakeInferenceEngine()
            val gpuConversation = FakeInferenceConversation()
            val cpuEngine = FakeInferenceEngine()
            val cpuConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = gpuEngine,
                    conversation = gpuConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            factory.addSession(
                factory.createFakeSession(
                    engine = cpuEngine,
                    conversation = cpuConversation,
                    backend = LlmEngine.Backend.CPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val messages = listOf(ChatMessage.User("Hello"))
            val job =
                launch {
                    engine.restoreHistory(messages)
                }

            gpuConversation.sentMessages
                .single()
                .callback
                .onError(
                    com.google.ai.edge.litertlm
                        .LiteRtLmJniException("GPU failed"),
                )
            assertThat(cpuConversation.sentMessages).hasSize(1)
            cpuConversation.sentMessages
                .single()
                .callback
                .onDone()
            job.join()

            assertThat(factory.createSessionRequests).hasSize(2)
            assertThat(factory.createSessionRequests[1].preferredBackend).isEqualTo(LlmEngine.Backend.CPU)
            assertThat(gpuEngine.closed).isTrue()
            assertThat(gpuConversation.closed).isTrue()
        }

    @Test
    fun `restoreHistory cancellation cancels native processing without fallback`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.NPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.NPU,
            )

            val job =
                launch {
                    engine.restoreHistory(listOf(ChatMessage.User("Hello")))
                }

            val callback = fakeConversation.sentMessages.single().callback
            job.cancel()
            job.join()

            // Native code can still report a late terminal callback while unwinding.
            callback.onDone()

            assertThat(job.isCancelled).isTrue()
            assertThat(fakeConversation.cancelled).isTrue()
            assertThat(factory.fallbackSessionRequests).isEmpty()
            assertThat(factory.createSessionRequests).hasSize(1)
            assertThat(engine.getActiveBackend()).isEqualTo(LlmEngine.Backend.NPU)
        }

    @Test
    fun `restoreHistory propagates non-backend errors without fallback`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.NPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.NPU,
            )

            val expectedError = IllegalStateException("Conversation state is invalid")
            var caughtError: Throwable? = null
            val job =
                launch {
                    try {
                        engine.restoreHistory(listOf(ChatMessage.User("Hello")))
                    } catch (throwable: Throwable) {
                        caughtError = throwable
                    }
                }

            fakeConversation.sentMessages
                .single()
                .callback
                .onError(expectedError)
            job.join()

            assertThat(caughtError).isInstanceOf(IllegalStateException::class.java)
            assertThat(caughtError?.message).isEqualTo(expectedError.message)
            assertThat(factory.fallbackSessionRequests).isEmpty()
            assertThat(factory.createSessionRequests).hasSize(1)
            assertThat(engine.getActiveBackend()).isEqualTo(LlmEngine.Backend.NPU)
        }

    @Test
    fun `restoreHistory propagates CPU backend failure without retry`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.CPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.CPU,
            )

            var caughtError: Throwable? = null
            val job =
                launch {
                    try {
                        engine.restoreHistory(listOf(ChatMessage.User("Hello")))
                    } catch (throwable: Throwable) {
                        caughtError = throwable
                    }
                }

            fakeConversation.sentMessages
                .single()
                .callback
                .onError(
                    com.google.ai.edge.litertlm
                        .LiteRtLmJniException("CPU failed"),
                )
            job.join()

            assertThat(caughtError).isInstanceOf(BackendFailureException::class.java)
            assertThat((caughtError as BackendFailureException).failedBackend).isEqualTo(LlmEngine.Backend.CPU)
            assertThat(factory.fallbackSessionRequests).isEmpty()
            assertThat(factory.createSessionRequests).hasSize(1)
        }

    @Test
    fun `restoreHistory does nothing when engine is not initialized`() =
        runEngineTest {
            engine.restoreHistory(listOf(ChatMessage.User("Hello")))

            assertThat(factory.createSessionRequests).isEmpty()
        }

    @Test
    fun `cleanup closes engine and conversation`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            engine.cleanup()

            assertThat(fakeConversation.closed).isTrue()
            assertThat(fakeEngine.closed).isTrue()
            assertThat(engine.getActiveBackend()).isNull()
        }

    @Test
    fun `runInference ignores duplicate onDone callbacks`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val emittedTokens = mutableListOf<Pair<String, Boolean>>()
            val job =
                launch {
                    engine.runInference("Hello") { partial, done ->
                        emittedTokens.add(partial to done)
                    }
                }

            val message = fakeConversation.sentMessages.single()
            message.callback.onDone()
            message.callback.onDone()
            message.callback.onDone()
            job.join()

            assertThat(emittedTokens).containsExactly("" to true)
        }

    @Test
    fun `runInference completes when terminal consumer throws`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            var completed = false
            var terminalCalls = 0
            val job =
                launch {
                    engine.runInference("Hello") { _, done ->
                        if (done) {
                            terminalCalls++
                            throw AssertionError("Terminal consumer failed")
                        }
                    }
                    completed = true
                }

            fakeConversation.sentMessages
                .single()
                .callback
                .onDone()
            job.join()

            assertThat(completed).isTrue()
            assertThat(terminalCalls).isEqualTo(1)
        }

    @Test
    fun `runInference swallows onToken consumer throwable and still completes`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val emittedTokens = mutableListOf<Pair<String, Boolean>>()
            val job =
                launch {
                    engine.runInference("Hello") { partial, done ->
                        if (partial == "boom") throw AssertionError("Token consumer failed")
                        emittedTokens.add(partial to done)
                    }
                }

            val message = fakeConversation.sentMessages.single()
            message.callback.onMessage(textMessage("before"))
            message.callback.onMessage(textMessage("boom"))
            message.callback.onMessage(textMessage("after"))
            message.callback.onDone()
            job.join()

            assertThat(emittedTokens)
                .containsExactly(
                    "before" to false,
                    "after" to false,
                    "" to true,
                ).inOrder()
        }

    @Test
    fun `runInference is safe when cancellation races with late terminal callback`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val job =
                launch {
                    engine.runInference("Hello") { _, _ -> }
                }

            val message = fakeConversation.sentMessages.single()
            job.cancel()
            job.join()

            // After cancellation, a late native callback should not resume a completed continuation.
            message.callback.onDone()
            message.callback.onError(RuntimeException("Late error"))

            assertThat(fakeConversation.cancelled).isTrue()
        }

    @Test
    fun `runInference closes failed hardware session when CPU fallback creation fails`() =
        runEngineTest {
            val gpuEngine = FakeInferenceEngine()
            val gpuConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = gpuEngine,
                    conversation = gpuConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            // No CPU session configured, so fallback creation will throw.
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val job =
                launch {
                    try {
                        engine.runInference("Hello") { _, _ -> }
                    } catch (e: IllegalStateException) {
                        // Expected: CPU fallback factory has no session.
                    }
                }

            gpuConversation.sentMessages
                .single()
                .callback
                .onError(
                    com.google.ai.edge.litertlm
                        .LiteRtLmJniException("GPU failed"),
                )
            job.join()

            assertThat(gpuEngine.closed).isTrue()
            assertThat(gpuConversation.closed).isTrue()
            assertThat(engine.getActiveBackend()).isNull()
        }

    @Test
    fun `runInferenceIsolated uses a separate conversation from active chat`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val emittedTokens = mutableListOf<Pair<String, Boolean>>()
            val job =
                launch {
                    engine.runInferenceIsolated("Summary prompt") { partial, done ->
                        emittedTokens.add(partial to done)
                    }
                }

            // The active chat conversation should not receive the isolated prompt.
            assertThat(fakeConversation.sentMessages).isEmpty()

            // A new conversation should have been created from the same engine.
            assertThat(fakeEngine.createdConversations).hasSize(1)
            val isolatedConversation = fakeEngine.createdConversations.single()
            assertThat(isolatedConversation.sentMessages).hasSize(1)
            assertThat(isolatedConversation.sentMessages.single().input).isEqualTo("Summary prompt")

            isolatedConversation.sentMessages
                .single()
                .callback
                .onMessage(textMessage("Short"))
            isolatedConversation.sentMessages
                .single()
                .callback
                .onDone()
            job.join()

            assertThat(emittedTokens).containsExactly("Short" to false, "" to true).inOrder()
        }

    @Test
    fun `runInferenceIsolated closes isolated conversation on completion`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val job =
                launch {
                    engine.runInferenceIsolated("Summary prompt") { _, _ -> }
                }

            val isolatedConversation = fakeEngine.createdConversations.single()
            isolatedConversation.sentMessages
                .single()
                .callback
                .onDone()
            job.join()

            assertThat(isolatedConversation.closed).isTrue()
            assertThat(fakeConversation.closed).isFalse()
        }

    @Test
    fun `runInferenceIsolated closes isolated conversation on error`() =
        runEngineTest {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            factory.addSession(
                factory.createFakeSession(
                    engine = fakeEngine,
                    conversation = fakeConversation,
                    backend = LlmEngine.Backend.GPU,
                ),
            )
            engine.initialize(
                modelPath = "/data/model.bin",
                backend = LlmEngine.Backend.GPU,
            )

            val job =
                launch {
                    try {
                        engine.runInferenceIsolated("Summary prompt") { _, _ -> }
                    } catch (e: RuntimeException) {
                        // Expected.
                    }
                }

            val isolatedConversation = fakeEngine.createdConversations.single()
            isolatedConversation.sentMessages
                .single()
                .callback
                .onError(RuntimeException("Summary failed"))
            job.join()

            assertThat(isolatedConversation.closed).isTrue()
            assertThat(fakeConversation.closed).isFalse()
        }

    private fun textMessage(text: String): com.google.ai.edge.litertlm.Message {
        val content =
            com.google.ai.edge.litertlm.Content
                .Text(text)
        val contents =
            com.google.ai.edge.litertlm.Contents
                .of(content)
        return com.google.ai.edge.litertlm.Message
            .model(contents)
    }
}
