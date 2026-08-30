package dev.hossain.codematex.ui.screens.settings.code

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.CodeBlockPreset
import dev.hossain.codematex.data.model.CodeBlockSettings
import dev.hossain.codematex.data.model.CodeFontSize
import dev.hossain.codematex.data.model.CodeTheme
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Screen for customizing syntax-highlighted code block themes, line numbers, top bar headers, and font size.
 */
@Parcelize
data object CodeBlockSettingsScreen : ParcelableScreen {
    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        val eventSink: (Event) -> Unit

        data class Content(
            val settings: CodeBlockSettings = CodeBlockSettings(),
            val previewCode: String = SAMPLE_KOTLIN_CODE,
            override val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data class ThemeSelected(
            val theme: CodeTheme,
        ) : Event

        data class LineNumbersToggled(
            val enabled: Boolean,
        ) : Event

        data class LanguageLabelToggled(
            val enabled: Boolean,
        ) : Event

        data class CopyButtonToggled(
            val enabled: Boolean,
        ) : Event

        data class PresetSelected(
            val preset: CodeBlockPreset,
        ) : Event

        data class FontSizeSelected(
            val fontSize: CodeFontSize,
        ) : Event

        data object BackClicked : Event
    }

    @kotlinx.parcelize.IgnoredOnParcel
    const val SAMPLE_KOTLIN_CODE: String = """// Coroutine-driven token streaming
suspend fun streamTokens(prompt: String): Flow<String> = flow {
    val session = llmEngine.createSession()
    session.generateResponse(prompt).collect { token ->
        emit(token.text)
    }
}"""
}
