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
            val previewCode: String = SAMPLE_CODE,
            val expandedPreviewCode: String = EXPANDED_SAMPLE_CODE,
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
    const val SAMPLE_CODE: String = """interface TokenStream<T> {
  id: string;
  readonly latencyMs: number;
  tokens: AsyncIterable<T>;
}

// Stream on-device LLM completions
async function generateResponse(prompt: string): Promise<TokenStream<string>> {
  const session = await createSession({ temperature: 0.7 });
  return session.streamTokens(`Query: ${"$"}{prompt}`);
}"""

    @kotlinx.parcelize.IgnoredOnParcel
    const val EXPANDED_SAMPLE_CODE: String = """interface TokenStream<T> {
  id: string;
  readonly latencyMs: number;
  tokens: AsyncIterable<T>;
}

interface GenerationOptions {
  temperature?: number;
  topK?: number;
  maxTokens?: number;
  stopSequences?: string[];
}

/**
 * Streams on-device LLM completions using LiteRT-LM runtime.
 * Provides real-time token yield with zero cloud telemetry.
 */
async function generateResponse(
  prompt: string,
  options: GenerationOptions = { temperature: 0.7 }
): Promise<TokenStream<string>> {
  const session = await createSession({
    temperature: options.temperature ?? 0.7,
    topK: options.topK ?? 40,
  });

  console.log(`[LiteRT] Initialized conversation for prompt: ${"$"}{prompt}`);
  return session.streamTokens(`Query: ${"$"}{prompt}`);
}"""
}
