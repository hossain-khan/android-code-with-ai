package dev.hossain.codematex.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CodeBlockPreferencesTest {
    @Test
    fun `all CodeTheme entries resolve valid highlight themes`() {
        CodeTheme.entries.forEach { theme ->
            val (light, dark) = theme.resolveHighlightThemes()
            assertThat(light).isNotNull()
            assertThat(dark).isNotNull()
            assertThat(theme.displayName).isNotEmpty()
            assertThat(theme.description).isNotEmpty()
        }
    }

    @Test
    fun `all CodeBlockPreset entries have valid configurations`() {
        CodeBlockPreset.entries.forEach { preset ->
            assertThat(preset.displayName).isNotEmpty()
            assertThat(preset.description).isNotEmpty()
        }
    }

    @Test
    fun `all CodeFontSize entries have valid sizes`() {
        CodeFontSize.entries.forEach { size ->
            assertThat(size.displayName).isNotEmpty()
            assertThat(size.sizeSp).isGreaterThan(0f)
        }
    }

    @Test
    fun `CodeBlockSettings default values are expected`() {
        val settings = CodeBlockSettings()
        assertThat(settings.theme).isEqualTo(CodeTheme.TOMORROW)
        assertThat(settings.showLineNumbers).isFalse()
        assertThat(settings.showLanguageLabel).isTrue()
        assertThat(settings.showCopyButton).isTrue()
        assertThat(settings.preset).isEqualTo(CodeBlockPreset.COMPACT)
        assertThat(settings.fontSize).isEqualTo(CodeFontSize.MEDIUM)
    }
}
