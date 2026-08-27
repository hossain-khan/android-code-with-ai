package dev.hossain.codematex.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelNameFormatterTest {
    @Test
    fun `formats gemma-4-E2B-it-litert-lm to Gemma 4-E2B IT`() {
        assertThat(formatShortModelName("gemma-4-E2B-it-litert-lm")).isEqualTo("Gemma 4-E2B IT")
    }

    @Test
    fun `formats gemma-4-E4B-it-litert-lm to Gemma 4-E4B IT`() {
        assertThat(formatShortModelName("gemma-4-E4B-it-litert-lm")).isEqualTo("Gemma 4-E4B IT")
    }

    @Test
    fun `formats gemma-2b-it-litert-lm to Gemma 2B IT`() {
        assertThat(formatShortModelName("gemma-2b-it-litert-lm")).isEqualTo("Gemma 2B IT")
    }

    @Test
    fun `preserves dev stub model display name`() {
        assertThat(formatShortModelName("Dev Stub Model")).isEqualTo("Dev Stub Model")
    }

    @Test
    fun `handles blank string gracefully`() {
        assertThat(formatShortModelName("")).isEmpty()
    }

    @Test
    fun `removes only -litert-lm suffix when -it is not present`() {
        assertThat(formatShortModelName("gemma-4-e2b-litert-lm")).isEqualTo("Gemma 4-e2b")
    }

    @Test
    fun `removes only -it suffix when -litert-lm is not present`() {
        assertThat(formatShortModelName("gemma-4-E2B-it")).isEqualTo("Gemma 4-E2B IT")
    }

    @Test
    fun `does not modify string without known suffixes`() {
        assertThat(formatShortModelName("Custom-Model")).isEqualTo("Custom-Model")
    }
}
