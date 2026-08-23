package dev.hossain.codematex.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelNameFormatterTest {
    @Test
    fun `formats gemma-4-E2B-it-litert-lm to Gemma 4-E2B IT`() {
        assertEquals("Gemma 4-E2B IT", formatShortModelName("gemma-4-E2B-it-litert-lm"))
    }

    @Test
    fun `formats gemma-4-E4B-it-litert-lm to Gemma 4-E4B IT`() {
        assertEquals("Gemma 4-E4B IT", formatShortModelName("gemma-4-E4B-it-litert-lm"))
    }

    @Test
    fun `formats gemma-2b-it-litert-lm to Gemma 2B IT`() {
        assertEquals("Gemma 2B IT", formatShortModelName("gemma-2b-it-litert-lm"))
    }

    @Test
    fun `preserves dev stub model display name`() {
        assertEquals("Dev Stub Model", formatShortModelName("Dev Stub Model"))
    }

    @Test
    fun `handles blank string gracefully`() {
        assertEquals("", formatShortModelName(""))
    }

    @Test
    fun `removes only -litert-lm suffix when -it is not present`() {
        assertEquals("Gemma 4-e2b", formatShortModelName("gemma-4-e2b-litert-lm"))
    }

    @Test
    fun `removes only -it suffix when -litert-lm is not present`() {
        assertEquals("Gemma 4-E2B IT", formatShortModelName("gemma-4-E2B-it"))
    }

    @Test
    fun `does not modify string without known suffixes`() {
        assertEquals("Custom-Model", formatShortModelName("Custom-Model"))
    }
}
