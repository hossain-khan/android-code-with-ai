package dev.hossain.codematex.data.model

import androidx.compose.runtime.Immutable
import dev.hossain.highlight.engine.HighlightTheme
import kotlinx.serialization.Serializable

/**
 * Syntax highlighting color themes bundled with `compose-highlight`.
 */
@Serializable
enum class CodeTheme(
    val displayName: String,
    val description: String,
) {
    TOMORROW(
        displayName = "Tomorrow",
        description = "Balanced pastel syntax palette with comfortable contrast.",
    ),
    ATOM_ONE(
        displayName = "Atom One",
        description = "Vibrant syntax colors inspired by the iconic Atom editor.",
    ),
    GITHUB(
        displayName = "GitHub",
        description = "Crisp, familiar syntax palette matching GitHub's web interface.",
    ),
    DRACULA(
        displayName = "Dracula",
        description = "High-contrast dark palette with distinctive purple & pink accents.",
    ),
    ;

    /**
     * Resolves the pair of (Light HighlightTheme, Dark HighlightTheme) for this theme preset.
     */
    fun resolveHighlightThemes(): Pair<HighlightTheme, HighlightTheme> =
        when (this) {
            TOMORROW -> HighlightTheme.tomorrow() to HighlightTheme.tomorrowNight()
            ATOM_ONE -> HighlightTheme.atomOneLight() to HighlightTheme.atomOneDark()
            GITHUB -> HighlightTheme.githubLight() to HighlightTheme.githubDark()
            DRACULA -> HighlightTheme.alucardLight() to HighlightTheme.draculaDark()
        }
}

/**
 * Layout density presets for rendered code blocks.
 */
@Serializable
enum class CodeBlockPreset(
    val displayName: String,
    val description: String,
) {
    COMFORTABLE(
        displayName = "Comfortable",
        description = "Generous padding and relaxed spacing for comfortable reading.",
    ),
    COMPACT(
        displayName = "Compact",
        description = "Tighter margins and padding for viewing more code per screen.",
    ),
}

/**
 * Font size presets for code text within code blocks.
 */
@Serializable
enum class CodeFontSize(
    val displayName: String,
    val sizeSp: Float,
) {
    SMALL(
        displayName = "Small",
        sizeSp = 11.5f,
    ),
    MEDIUM(
        displayName = "Medium (Default)",
        sizeSp = 13.0f,
    ),
    LARGE(
        displayName = "Large",
        sizeSp = 15.5f,
    ),
}

/**
 * Complete immutable snapshot of user preferences for syntax-highlighted code blocks.
 */
@Immutable
@Serializable
data class CodeBlockSettings(
    val theme: CodeTheme = CodeTheme.TOMORROW,
    val showLineNumbers: Boolean = true,
    val showLanguageLabel: Boolean = true,
    val showCopyButton: Boolean = true,
    val preset: CodeBlockPreset = CodeBlockPreset.COMFORTABLE,
    val fontSize: CodeFontSize = CodeFontSize.MEDIUM,
)
