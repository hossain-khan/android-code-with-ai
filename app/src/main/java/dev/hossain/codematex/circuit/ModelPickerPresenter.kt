package dev.hossain.codematex.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.repository.ModelRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import com.slack.circuit.codegen.annotations.CircuitInject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@AssistedInject
class ModelPickerPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: ModelPickerScreen,
    private val modelRepository: ModelRepository,
) : Presenter<ModelPickerScreen.State> {

    @Composable
    override fun present(): ModelPickerScreen.State {
        var models by rememberRetained { mutableStateOf<List<AiModel>>(emptyList()) }
        var isLoading by rememberRetained { mutableStateOf(true) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            modelRepository.getAvailableModels()
                .catch { isLoading = false }
                .collect { list ->
                    models = list
                    isLoading = false
                }
        }

        val eventSink: (ModelPickerScreen.Event) -> Unit = { event ->
            when (event) {
                is ModelPickerScreen.Event.Download -> {
                    // TODO: Trigger download via WorkManager
                }
                is ModelPickerScreen.Event.CancelDownload -> {}
                is ModelPickerScreen.Event.Delete -> {}
                is ModelPickerScreen.Event.Select -> {
                    scope.launch {
                        modelRepository.selectModel(event.model)
                        navigator.pop()
                    }
                }
            }
        }

        return if (isLoading) {
            ModelPickerScreen.State.Loading
        } else {
            ModelPickerScreen.State.Success(
                models = models,
                eventSink = eventSink,
            )
        }
    }

    @CircuitInject(ModelPickerScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: ModelPickerScreen,
        ): ModelPickerPresenter
    }
}
