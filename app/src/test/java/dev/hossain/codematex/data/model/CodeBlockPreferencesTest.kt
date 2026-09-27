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
    fun `all CodeTheme entries map to valid light and dark HighlightThemeDescriptors`() {
        CodeTheme.entries.forEach { theme ->
            val lightDesc = theme.lightDescriptor
            val darkDesc = theme.darkDescriptor

            assertThat(lightDesc.id).isEqualTo(theme.lightThemeId)
            assertThat(darkDesc.id).isEqualTo(theme.darkThemeId)

            assertThat(lightDesc.isLight).isTrue()
            assertThat(lightDesc.isDark).isFalse()

            assertThat(darkDesc.isDark).isTrue()
            assertThat(darkDesc.isLight).isFalse()

            // Verify referential caching
            val (light1, dark1) = theme.resolveHighlightThemes()
            val (light2, dark2) = theme.resolveHighlightThemes()
            assertThat(light1).isSameInstanceAs(light2)
            assertThat(dark1).isSameInstanceAs(dark2)
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
