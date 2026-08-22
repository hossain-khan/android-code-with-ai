@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package dev.hossain.codematex.runtime

import dev.hossain.codematex.circuit.overlay.ModelConfig
import dev.hossain.codematex.data.model.ChatMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
                modelPath = "/dev/null",
                backend = LlmEngine.Backend.GPU,
            )

            assertEquals(0, factory.createSessionRequests.size)
            assertNull(engine.getActiveBackend())
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

            assertEquals(1, factory.createSessionRequests.size)
            with(factory.createSessionRequests.single()) {
                assertEquals("/data/model.bin", modelPath)
                assertEquals(LlmEngine.Backend.GPU, preferredBackend)
                assertEquals("You are a helpful assistant", systemInstruction)
                assertEquals(128, config.maxTokens)
            }
            assertEquals(LlmEngine.Backend.GPU, engine.getActiveBackend())
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

            assertEquals(1, factory.createSessionRequests.size)
            assertEquals(1, fakeEngine.createdConversations.size)
            val systemContent =
                fakeEngine.createdConversations
                    .single()
                    .config
                    ?.systemInstruction
                    ?.contents
                    ?.first()
                    as? com.google.ai.edge.litertlm.Content.Text
            assertEquals("Second prompt", systemContent?.text)
        }

    @Test
    fun `runInference returns stub response when engine is not initialized`() =
        runEngineTest {
            val emittedTokens = mutableListOf<Pair<String, Boolean>>()

            engine.runInference("Hello") { partial, done ->
                emittedTokens.add(partial to done)
            }

            assertEquals(1, emittedTokens.size)
            assertEquals(true, emittedTokens.single().second)
            assertTrue(emittedTokens.single().first.contains("stub response"))
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
            assertEquals("Hello", message.input)
            message.callback.onMessage(textMessage("Hello"))
            message.callback.onMessage(textMessage(" world"))
            message.callback.onDone()
            job.join()

            assertEquals(
                listOf("Hello" to false, " world" to false, "" to true),
                emittedTokens,
            )
        }

    @Test
    fun `runInference falls back to CPU when inference fails on hardware backend`() =
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

            val emittedTokens = mutableListOf<Pair<String, Boolean>>()
            val job =
                launch {
                    engine.runInference("Hello") { partial, done ->
                        emittedTokens.add(partial to done)
                    }
                }

            val gpuMessage = gpuConversation.sentMessages.single()
            gpuMessage.callback.onError(RuntimeException("GPU failed"))

            val cpuMessage = cpuConversation.sentMessages.single()
            cpuMessage.callback.onMessage(textMessage("OK"))
            cpuMessage.callback.onDone()
            job.join()

            assertEquals(2, factory.createSessionRequests.size)
            assertEquals(LlmEngine.Backend.CPU, factory.createSessionRequests[1].preferredBackend)
            assertEquals(listOf("OK" to false, "" to true), emittedTokens)
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

            assertNotNull(caughtError)
            assertEquals(error.message, caughtError?.message)
            assertEquals(1, factory.createSessionRequests.size)
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

            assertTrue(fakeConversation.cancelled)
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

            assertTrue(fakeConversation.closed)
            assertEquals(1, fakeEngine.createdConversations.size)
            val newConversation = fakeEngine.createdConversations.single()
            val newSystemContent =
                newConversation.config
                    ?.systemInstruction
                    ?.contents
                    ?.first()
                    as? com.google.ai.edge.litertlm.Content.Text
            assertEquals("New system prompt", newSystemContent?.text)
            assertEquals(0.5, newConversation.config?.samplerConfig?.temperature ?: 0.0, 0.001)
            assertEquals(20, newConversation.config?.samplerConfig?.topK)
            assertEquals(0.9, newConversation.config?.samplerConfig?.topP ?: 0.0, 0.001)
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
            assertTrue(message.input.contains("Hello"))
            assertTrue(message.input.contains("Hi there"))
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
                .onError(RuntimeException("GPU failed"))
            assertEquals(1, cpuConversation.sentMessages.size)
            cpuConversation.sentMessages
                .single()
                .callback
                .onDone()
            job.join()

            assertEquals(2, factory.createSessionRequests.size)
            assertEquals(LlmEngine.Backend.CPU, factory.createSessionRequests[1].preferredBackend)
        }

    @Test
    fun `restoreHistory does nothing when engine is not initialized`() =
        runEngineTest {
            engine.restoreHistory(listOf(ChatMessage.User("Hello")))

            assertEquals(0, factory.createSessionRequests.size)
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

            assertTrue(fakeConversation.closed)
            assertTrue(fakeEngine.closed)
            assertNull(engine.getActiveBackend())
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
