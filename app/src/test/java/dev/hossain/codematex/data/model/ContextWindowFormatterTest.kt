package dev.hossain.codematex.data.model

import dev.hossain.codematex.runtime.LlmEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContextWindowFormatterTest {
    private fun createModelWithContext(contextWindow: Int): AiModel =
        AiModel(
            id = "test-model",
            name = "test-model",
            displayName = "Test Model",
            downloadUrl = "https://example.com/model",
            sizeBytes = 1000L,
            localPath = null,
            downloadStatus = DownloadStatus.NOT_DOWNLOADED,
            preferredBackend = LlmEngine.Backend.GPU,
            contextWindow = contextWindow,
        )

    @Test
    fun `returns null for zero or negative context window`() {
        assertNull(createModelWithContext(0).formattedContextWindow)
        assertNull(createModelWithContext(-100).formattedContextWindow)
    }

    @Test
    fun `formats common binary context window sizes correctly`() {
        assertEquals("4k Context", createModelWithContext(4096).formattedContextWindow)
        assertEquals("8k Context", createModelWithContext(8192).formattedContextWindow)
        assertEquals("16k Context", createModelWithContext(16384).formattedContextWindow)
        assertEquals("32k Context", createModelWithContext(32768).formattedContextWindow)
        assertEquals("64k Context", createModelWithContext(65536).formattedContextWindow)
        assertEquals("128k Context", createModelWithContext(131072).formattedContextWindow)
    }

    @Test
    fun `formats decimal context window sizes correctly`() {
        assertEquals("128k Context", createModelWithContext(128000).formattedContextWindow)
        assertEquals("32k Context", createModelWithContext(32000).formattedContextWindow)
        assertEquals("8k Context", createModelWithContext(8000).formattedContextWindow)
    }
}
