package dev.hossain.codematex.data.model

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.runtime.LlmEngine
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
        assertThat(createModelWithContext(0).formattedContextWindow).isNull()
        assertThat(createModelWithContext(-100).formattedContextWindow).isNull()
    }

    @Test
    fun `formats common binary context window sizes correctly`() {
        assertThat(createModelWithContext(4096).formattedContextWindow).isEqualTo("4k Context")
        assertThat(createModelWithContext(8192).formattedContextWindow).isEqualTo("8k Context")
        assertThat(createModelWithContext(16384).formattedContextWindow).isEqualTo("16k Context")
        assertThat(createModelWithContext(32768).formattedContextWindow).isEqualTo("32k Context")
        assertThat(createModelWithContext(65536).formattedContextWindow).isEqualTo("64k Context")
        assertThat(createModelWithContext(131072).formattedContextWindow).isEqualTo("128k Context")
    }

    @Test
    fun `formats decimal context window sizes correctly`() {
        assertThat(createModelWithContext(128000).formattedContextWindow).isEqualTo("128k Context")
        assertThat(createModelWithContext(32000).formattedContextWindow).isEqualTo("32k Context")
        assertThat(createModelWithContext(8000).formattedContextWindow).isEqualTo("8k Context")
    }
}
