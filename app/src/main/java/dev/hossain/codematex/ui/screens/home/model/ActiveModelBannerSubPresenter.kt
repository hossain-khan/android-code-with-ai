package dev.hossain.codematex.ui.screens.home.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubPresenterFactory
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.runtime.LlmEngine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

class ActiveModelBannerSubPresenter(
    private val modelRepository: ModelRepository,
    private val llmEngine: LlmEngine,
) : SubPresenter<ActiveModelBannerOuterEvent, ActiveModelBannerSubState> {
    @Composable
    override fun present(outerEventSink: (ActiveModelBannerOuterEvent) -> Unit): ActiveModelBannerSubState {
        var selectedModel by rememberRetained { mutableStateOf(modelRepository.getSelectedModel()) }

        LaunchedEffect(Unit) {
            modelRepository.getAvailableModels().collect { models ->
                selectedModel =
                    modelRepository.getSelectedModel()
                        ?: models.firstOrNull { it.isSelected }
                        ?: models.firstOrNull { it.downloadStatus == DownloadStatus.DOWNLOADED }
            }
        }

        val hasDownloadedModel = selectedModel != null
        val selectedModelName = selectedModel?.displayName
        val isModelInMemory = llmEngine.isInitialized()
        val memoryBackend = llmEngine.getActiveBackend()?.name

        return ActiveModelBannerSubState(
            hasDownloadedModel = hasDownloadedModel,
            selectedModelName = selectedModelName,
            isModelInMemory = isModelInMemory,
            memoryBackend = memoryBackend,
            eventSink = { event ->
                when (event) {
                    ActiveModelBannerUiEvent.ManageModels -> {
                        outerEventSink(ActiveModelBannerOuterEvent.NavigateToModelPicker)
                    }
                }
            },
        )
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class ActiveModelBannerSubPresenterFactory(
    private val modelRepository: ModelRepository,
    private val llmEngine: LlmEngine,
) : SubPresenterFactory {
    override fun create(screen: SubScreen<*>): SubPresenter<*, *>? =
        when (screen) {
            is ActiveModelBannerSubScreen -> {
                ActiveModelBannerSubPresenter(
                    modelRepository = modelRepository,
                    llmEngine = llmEngine,
                )
            }

            else -> {
                null
            }
        }
}
