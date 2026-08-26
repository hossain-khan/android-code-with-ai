@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package dev.hossain.codematex.runtime

import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ModelConfig
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
                modelPath = DEV_STUB_MODEL_PATH,
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
                        assertEquals(LlmEngine.Backend.GPU, e.failedBackend)
                    }
                }

            val gpuMessage = gpuConversation.sentMessages.single()
            gpuMessage.callback.onError(
                com.google.ai.edge.litertlm
                    .LiteRtLmJniException("GPU failed"),
            )
            job.join()

            assertEquals(2, factory.createSessionRequests.size)
            assertEquals(LlmEngine.Backend.CPU, factory.createSessionRequests[1].preferredBackend)
            assertTrue("GPU engine should be closed during fallback", gpuEngine.closed)
            assertTrue("GPU conversation should be closed during fallback", gpuConversation.closed)
            assertEquals(LlmEngine.Backend.CPU, engine.getActiveBackend())

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

            assertEquals(listOf("OK" to false, "" to true), emittedTokens)
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
                        assertEquals(LlmEngine.Backend.GPU, e.failedBackend)
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

            assertTrue("GPU engine should be closed", gpuEngine.closed)
            assertTrue("GPU conversation should be closed", gpuConversation.closed)
            assertEquals(LlmEngine.Backend.CPU, engine.getActiveBackend())
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
                        assertEquals(LlmEngine.Backend.CPU, e.failedBackend)
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

            assertEquals(1, factory.createSessionRequests.size)
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
                        assertEquals("Programming error", e.message)
                    }
                }

            gpuConversation.sentMessages
                .single()
                .callback
                .onError(RuntimeException("Programming error"))
            job.join()

            assertEquals(1, factory.createSessionRequests.size)
            assertEquals(LlmEngine.Backend.GPU, engine.getActiveBackend())
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

            assertEquals(1, factory.createSessionRequests.size)
            assertEquals(LlmEngine.Backend.GPU, engine.getActiveBackend())
            assertEquals(listOf("Hello" to false, "" to true), emittedTokens)
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

            assertTrue("Expected prompt to contain all turns", user1Index >= 0 && agent1Index >= 0 && user2Index >= 0 && agent2Index >= 0)
            assertTrue("User1 should come before Agent1", user1Index < agent1Index)
            assertTrue("Agent1 should come before User2", agent1Index < user2Index)
            assertTrue("User2 should come before Agent2", user2Index < agent2Index)
            assertEquals(-1, message.input.indexOf("System"))
            assertEquals(-1, message.input.indexOf("Error"))

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
            assertEquals(1, cpuConversation.sentMessages.size)
            cpuConversation.sentMessages
                .single()
                .callback
                .onDone()
            job.join()

            assertEquals(2, factory.createSessionRequests.size)
            assertEquals(LlmEngine.Backend.CPU, factory.createSessionRequests[1].preferredBackend)
            assertTrue("GPU engine should be closed during history fallback", gpuEngine.closed)
            assertTrue("GPU conversation should be closed during history fallback", gpuConversation.closed)
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

            assertEquals(listOf("" to true), emittedTokens)
        }

    @Test
    fun `runInference swallows onToken consumer exception and still completes`() =
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
                        if (partial == "boom") throw RuntimeException("Token consumer failed")
                        emittedTokens.add(partial to done)
                    }
                }

            val message = fakeConversation.sentMessages.single()
            message.callback.onMessage(textMessage("before"))
            message.callback.onMessage(textMessage("boom"))
            message.callback.onMessage(textMessage("after"))
            message.callback.onDone()
            job.join()

            assertEquals(listOf("before" to false, "after" to false, "" to true), emittedTokens)
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

            assertTrue(fakeConversation.cancelled)
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

            assertTrue("GPU engine should be closed when fallback fails", gpuEngine.closed)
            assertTrue("GPU conversation should be closed when fallback fails", gpuConversation.closed)
            assertNull(engine.getActiveBackend())
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
            assertEquals(0, fakeConversation.sentMessages.size)

            // A new conversation should have been created from the same engine.
            assertEquals(1, fakeEngine.createdConversations.size)
            val isolatedConversation = fakeEngine.createdConversations.single()
            assertEquals(1, isolatedConversation.sentMessages.size)
            assertEquals("Summary prompt", isolatedConversation.sentMessages.single().input)

            isolatedConversation.sentMessages
                .single()
                .callback
                .onMessage(textMessage("Short"))
            isolatedConversation.sentMessages
                .single()
                .callback
                .onDone()
            job.join()

            assertEquals(listOf("Short" to false, "" to true), emittedTokens)
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

            assertTrue("Isolated conversation should be closed", isolatedConversation.closed)
            assertEquals("Active chat conversation should not be closed", false, fakeConversation.closed)
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

            assertTrue("Isolated conversation should be closed on error", isolatedConversation.closed)
            assertEquals("Active chat conversation should not be closed", false, fakeConversation.closed)
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
