package dev.hossain.codematex.runtime

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ToolManager
import dev.hossain.codematex.circuit.overlay.ModelConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LlmEngineImplTest {
    private lateinit var factory: FakeLlmEngineFactory
    private lateinit var engine: LlmEngineImpl

    @Before
    fun setUp() {
        factory = FakeLlmEngineFactory()
        engine = LlmEngineImpl(factory)
    }

    @Test
    fun `initialize skips loading for dev null model path`() =
        runTest {
            engine.initialize(
                modelPath = "/dev/null",
                backend = LlmEngine.Backend.GPU,
            )

            assertEquals(0, factory.createSessionRequests.size)
            assertNull(engine.getActiveBackend())
        }

    @Test
    fun `initialize creates session through factory and tracks active backend`() =
        runTest {
            val fakeEngine = createFakeEngine()
            val fakeConversation = Conversation(0, ToolManager(), false)
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
    fun `runInference returns stub response when engine is not initialized`() =
        runTest {
            val emittedTokens = mutableListOf<Pair<String, Boolean>>()

            engine.runInference("Hello") { partial, done ->
                emittedTokens.add(partial to done)
            }

            assertEquals(1, emittedTokens.size)
            assertEquals(true, emittedTokens.single().second)
            assert(emittedTokens.single().first.contains("stub response"))
        }

    private fun createFakeEngine(): Engine =
        Engine(
            EngineConfig(
                modelPath = "test",
                backend = Backend.CPU(),
                maxNumTokens = 100,
            ),
        )
}
