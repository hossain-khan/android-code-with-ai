package dev.hossain.codematex.data.model

import androidx.compose.runtime.Immutable
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.engine.HighlightThemeDescriptor
import kotlinx.serialization.Serializable

/**
 * Syntax highlighting color themes bundled with `compose-highlight`.
 */
@Serializable
enum class CodeTheme(
    val displayName: String,
    val description: String,
    val lightThemeId: String,
    val darkThemeId: String,
) {
    TOMORROW(
        displayName = "Tomorrow",
        description = "Balanced pastel syntax palette with comfortable contrast.",
        lightThemeId = "tomorrow",
        darkThemeId = "tomorrow-night",
    ),
    ATOM_ONE(
        displayName = "Atom One",
        description = "Vibrant syntax colors inspired by the iconic Atom editor.",
        lightThemeId = "atom-one-light",
        darkThemeId = "atom-one-dark",
    ),
    GITHUB(
        displayName = "GitHub",
        description = "Crisp, familiar syntax palette matching GitHub's web interface.",
        lightThemeId = "github",
        darkThemeId = "github-dark",
    ),
    DRACULA(
        displayName = "Dracula",
        description = "High-contrast dark palette with distinctive purple & pink accents.",
        lightThemeId = "alucard",
        darkThemeId = "dracula",
    ),
    ;

    /**
     * The [HighlightThemeDescriptor] for the light theme variant.
     */
    val lightDescriptor: HighlightThemeDescriptor
        get() =
            requireNotNull(HighlightTheme.findBundledById(lightThemeId)) {
                "Bundled light theme not found for ID: $lightThemeId"
            }

    /**
     * The [HighlightThemeDescriptor] for the dark theme variant.
     */
    val darkDescriptor: HighlightThemeDescriptor
        get() =
            requireNotNull(HighlightTheme.findBundledById(darkThemeId)) {
                "Bundled dark theme not found for ID: $darkThemeId"
            }

    /**
     * Resolves the pair of (Light HighlightTheme, Dark HighlightTheme) for this theme preset.
     * Uses cached singletons from the descriptors to avoid re-allocating themes.
     */
    fun resolveHighlightThemes(): Pair<HighlightTheme, HighlightTheme> = lightDescriptor.theme to darkDescriptor.theme
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
        displayName = "Medium",
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
    val showLineNumbers: Boolean = false,
    val showLanguageLabel: Boolean = true,
    val showCopyButton: Boolean = true,
    val preset: CodeBlockPreset = CodeBlockPreset.COMPACT,
    val fontSize: CodeFontSize = CodeFontSize.MEDIUM,
)
