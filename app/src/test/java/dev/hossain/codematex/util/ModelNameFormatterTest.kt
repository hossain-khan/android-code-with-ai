package dev.hossain.codematex.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelNameFormatterTest {
    @Test
    fun `formats gemma-4-E2B-it-litert-lm to Gemma-4-E2B`() {
        assertEquals("Gemma-4-E2B", formatShortModelName("gemma-4-E2B-it-litert-lm"))
    }

    @Test
    fun `formats gemma-4-E4B-it-litert-lm to Gemma-4-E4B`() {
        assertEquals("Gemma-4-E4B", formatShortModelName("gemma-4-E4B-it-litert-lm"))
    }

    @Test
    fun `preserves dev stub model display name`() {
        assertEquals("Dev Stub Model", formatShortModelName("Dev Stub Model"))
    }

    @Test
    fun `handles blank string gracefully`() {
        assertEquals("", formatShortModelName(""))
    }
}
