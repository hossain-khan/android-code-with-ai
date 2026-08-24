package dev.hossain.codematex.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.circuit.overlay.ModelConfigStore
import dev.hossain.codematex.data.model.DownloadStatus
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

/**
 * Presenter for [ChatScreen].
 *
 * This class is intentionally thin: it creates a retained [ChatStateHolder], wires
 * lifecycle effects, maps user events to holder actions, and renders [ChatScreen.State]
 * from the holder's observable properties.
 */
@AssistedInject
class ChatPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: ChatScreen,
    private val stateHolderFactory: DefaultChatStateHolder.Factory,
    private val configStore: ModelConfigStore,
) : Presenter<ChatScreen.State> {
    @Composable
    override fun present(): ChatScreen.State {
        val scope = rememberCoroutineScope()
        val stateHolder = rememberRetained { stateHolderFactory.create(screen) }
        stateHolder.attachScope(scope)

        LaunchedEffect(Unit) {
            stateHolder.loadAvailableModels()
        }

        LaunchedEffect(screen.sessionId) {
            stateHolder.loadSessionMessages()
        }

        LaunchedEffect(
            stateHolder.activeModel?.id,
            stateHolder.activeModel?.localPath,
            stateHolder.initTrigger,
            stateHolder.persona,
        ) {
            stateHolder.initializeModel()
        }

        LaunchedEffect(stateHolder.isGenerating) {
            stateHolder.monitorSystemStats()
        }

        val eventSink: (ChatScreen.Event) -> Unit = { event ->
            when (event) {
                is ChatScreen.Event.SendMessage -> stateHolder.sendMessage(event.text)
                is ChatScreen.Event.SelectPersona -> stateHolder.selectPersona(event.persona)
                ChatScreen.Event.StopGeneration -> stateHolder.stopGeneration()
                ChatScreen.Event.ResetSession -> stateHolder.resetSession()
                ChatScreen.Event.Retry -> stateHolder.retry()
                is ChatScreen.Event.CopyMessage -> stateHolder.copyMessage(event.content)
                ChatScreen.Event.OpenModelPicker -> navigator.goTo(ModelPickerScreen)
                ChatScreen.Event.Back -> navigator.pop()
            }
        }

        return when {
            stateHolder.errorMessage != null -> {
                ChatScreen.State.Error(stateHolder.errorMessage!!, screen.topic, eventSink)
            }

            stateHolder.activeModel == null -> {
                val hasDownloadedModels =
                    stateHolder.availableModels.any { it.downloadStatus == DownloadStatus.DOWNLOADED }
                ChatScreen.State.NoModelSelected(
                    hasDownloadedModels = hasDownloadedModels,
                    topic = screen.topic,
                    eventSink = eventSink,
                )
            }

            else -> {
                val model =
                    stateHolder.activeModel
                        ?: return@present ChatScreen.State.Error(
                            "No model available",
                            screen.topic,
                            eventSink,
                        )
                val sizeMb = model.sizeBytes / 1_000_000
                val sizeText = "$sizeMb MB"
                val memoryText = "Requires ${model.minDeviceMemoryInGb}GB RAM"
                val config = configStore.config
                val configText = "Temp: ${config.temperature}, Top-K: ${config.topK}, Top-P: ${config.topP}"

                ChatScreen.State.Active(
                    messages = stateHolder.messages,
                    isGenerating = stateHolder.isGenerating,
                    isPreparing = stateHolder.isPreparing,
                    modelName = model.displayName,
                    persona = stateHolder.persona,
                    activeBackend = stateHolder.activeBackend,
                    modelSize = sizeText,
                    modelMemory = memoryText,
                    configInfo = configText,
                    throughputInfo = stateHolder.throughputInfo,
                    systemStatsInfo = stateHolder.systemStatsInfo,
                    topic = screen.topic,
                    eventSink = eventSink,
                )
            }
        }
    }

    @CircuitInject(ChatScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: ChatScreen,
        ): ChatPresenter
    }
}
