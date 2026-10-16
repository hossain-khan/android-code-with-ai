package dev.hossain.codematex.runtime

import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.MessageCallback
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit tests for [DefaultInferenceEngine] and [DefaultInferenceConversation].
 *
 * LiteRT-LM [com.google.ai.edge.litertlm.Engine] and [com.google.ai.edge.litertlm.Conversation] are final native JNI
 * classes that load shared libraries (liblitertlm_jni.so) and cannot be instantiated or mocked in a host JVM unit test
 * environment. The internal constructors provide test seams verifying state transition, fast-fail checks, and idempotency.
 */
class InferenceEngineTest {
    @Test
    fun `DefaultInferenceConversation forwards sendMessageAsync when open`() {
        var sentInput: String? = null
        var callbackReceived: MessageCallback? = null
        val conversation =
            DefaultInferenceConversation(
                sendAction = { input, cb ->
                    sentInput = input
                    callbackReceived = cb
                },
                cancelAction = {},
                closeAction = {},
            )

        val callback =
            object : MessageCallback {
                override fun onMessage(message: com.google.ai.edge.litertlm.Message) {}

                override fun onDone() {}

                override fun onError(throwable: Throwable) {}
            }
        conversation.sendMessageAsync("Hello", callback)

        assertThat(sentInput).isEqualTo("Hello")
        assertThat(callbackReceived).isSameInstanceAs(callback)
    }

    @Test
    fun `DefaultInferenceConversation throws on sendMessageAsync when closed`() {
        var sendInvoked = false
        val conversation =
            DefaultInferenceConversation(
                sendAction = { _, _ -> sendInvoked = true },
                cancelAction = {},
                closeAction = {},
            )

        conversation.close()

        val exception =
            assertThrows(IllegalStateException::class.java) {
                conversation.sendMessageAsync(
                    "Hello",
                    object : MessageCallback {
                        override fun onMessage(message: com.google.ai.edge.litertlm.Message) {}

                        override fun onDone() {}

                        override fun onError(throwable: Throwable) {}
                    },
                )
            }

        assertThat(exception).hasMessageThat().isEqualTo("Cannot send message on a closed conversation.")
        assertThat(sendInvoked).isFalse()
    }

    @Test
    fun `DefaultInferenceConversation forwards cancelProcess when open`() {
        var cancelInvoked = false
        val conversation =
            DefaultInferenceConversation(
                sendAction = { _, _ -> },
                cancelAction = { cancelInvoked = true },
                closeAction = {},
            )

        conversation.cancelProcess()
        assertThat(cancelInvoked).isTrue()
    }

    @Test
    fun `DefaultInferenceConversation ignores cancelProcess when closed`() {
        var cancelInvoked = false
        val conversation =
            DefaultInferenceConversation(
                sendAction = { _, _ -> },
                cancelAction = { cancelInvoked = true },
                closeAction = {},
            )

        conversation.close()
        conversation.cancelProcess()

        assertThat(cancelInvoked).isFalse()
    }

    @Test
    fun `DefaultInferenceConversation close is idempotent`() {
        var closeCount = 0
        val conversation =
            DefaultInferenceConversation(
                sendAction = { _, _ -> },
                cancelAction = {},
                closeAction = { closeCount++ },
            )

        conversation.close()
        conversation.close()

        assertThat(closeCount).isEqualTo(1)
    }

    @Test
    fun `DefaultInferenceEngine forwards initialize and createConversation when open`() {
        var initInvoked = false
        var createdConfig: ConversationConfig? = null
        val fakeConversation = FakeInferenceConversation()

        val engine =
            DefaultInferenceEngine(
                initAction = { initInvoked = true },
                createConversationAction = { config ->
                    createdConfig = config
                    fakeConversation
                },
                closeAction = {},
            )

        engine.initialize()
        assertThat(initInvoked).isTrue()

        val config = ConversationConfig()
        val result = engine.createConversation(config)
        assertThat(result).isSameInstanceAs(fakeConversation)
        assertThat(createdConfig).isSameInstanceAs(config)
    }

    @Test
    fun `DefaultInferenceEngine throws on initialize when closed`() {
        var initInvoked = false
        val engine =
            DefaultInferenceEngine(
                initAction = { initInvoked = true },
                createConversationAction = { FakeInferenceConversation() },
                closeAction = {},
            )

        engine.close()

        val exception =
            assertThrows(IllegalStateException::class.java) {
                engine.initialize()
            }

        assertThat(exception).hasMessageThat().isEqualTo("Cannot initialize a closed engine.")
        assertThat(initInvoked).isFalse()
    }

    @Test
    fun `DefaultInferenceEngine throws on createConversation when closed`() {
        var createInvoked = false
        val engine =
            DefaultInferenceEngine(
                initAction = {},
                createConversationAction = {
                    createInvoked = true
                    FakeInferenceConversation()
                },
                closeAction = {},
            )

        engine.close()

        val exception =
            assertThrows(IllegalStateException::class.java) {
                engine.createConversation(ConversationConfig())
            }

        assertThat(exception).hasMessageThat().isEqualTo("Cannot create conversation from a closed engine.")
        assertThat(createInvoked).isFalse()
    }

    @Test
    fun `DefaultInferenceEngine close is idempotent`() {
        var closeCount = 0
        val engine =
            DefaultInferenceEngine(
                initAction = {},
                createConversationAction = { FakeInferenceConversation() },
                closeAction = { closeCount++ },
            )

        engine.close()
        engine.close()

        assertThat(closeCount).isEqualTo(1)
    }

    @Test
    fun `DefaultInferenceConversation cancelProcess and close are strictly serialized`() {
        val cancelStarted = java.util.concurrent.CountDownLatch(1)
        val canCompleteCancel = java.util.concurrent.CountDownLatch(1)
        var closeExecuted = false
        var cancelExecuted = false

        val conversation =
            DefaultInferenceConversation(
                sendAction = { _, _ -> },
                cancelAction = {
                    cancelStarted.countDown()
                    canCompleteCancel.await(2, java.util.concurrent.TimeUnit.SECONDS)
                    cancelExecuted = true
                },
                closeAction = {
                    closeExecuted = true
                },
            )

        val cancelThread = Thread { conversation.cancelProcess() }
        cancelThread.start()

        assertThat(cancelStarted.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue()

        val closeThread = Thread { conversation.close() }
        closeThread.start()

        Thread.sleep(50)
        assertThat(closeExecuted).isFalse()

        canCompleteCancel.countDown()
        cancelThread.join(2000)
        closeThread.join(2000)

        assertThat(cancelExecuted).isTrue()
        assertThat(closeExecuted).isTrue()
    }

    @Test
    fun `DefaultInferenceConversation cancelProcess never runs after close starts`() {
        val closeStarted = java.util.concurrent.CountDownLatch(1)
        val canCompleteClose = java.util.concurrent.CountDownLatch(1)
        var cancelExecuted = false
        var closeExecuted = false

        val conversation =
            DefaultInferenceConversation(
                sendAction = { _, _ -> },
                cancelAction = {
                    cancelExecuted = true
                },
                closeAction = {
                    closeStarted.countDown()
                    canCompleteClose.await(2, java.util.concurrent.TimeUnit.SECONDS)
                    closeExecuted = true
                },
            )

        val closeThread = Thread { conversation.close() }
        closeThread.start()

        assertThat(closeStarted.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue()

        val cancelThread = Thread { conversation.cancelProcess() }
        cancelThread.start()

        canCompleteClose.countDown()
        closeThread.join(2000)
        cancelThread.join(2000)

        assertThat(closeExecuted).isTrue()
        assertThat(cancelExecuted).isFalse()
    }
}
