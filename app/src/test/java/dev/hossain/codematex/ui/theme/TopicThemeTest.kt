package dev.hossain.codematex.ui.theme

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.CodingTopic
import org.junit.Test

class TopicThemeTest {
    @Test
    fun `all selectable topics provide visual info with non-empty starter prompts`() {
        for (topic in CodingTopic.selectableEntries) {
            val visualInfo = topic.visualInfo
            assertThat(visualInfo.tagline).isNotEmpty()
            assertThat(visualInfo.iconGlyph).isNotEmpty()
            assertThat(visualInfo.starterPrompts).isNotEmpty()
            assertThat(visualInfo.starterPrompts.size).isAtLeast(3)
        }
    }

    @Test
    fun `android topic contains updated compose compiler stability starter prompt`() {
        val visualInfo = CodingTopic.ANDROID.visualInfo
        assertThat(visualInfo.starterPrompts).contains(
            "Explain Compose Compiler Stability: @Stable, @Immutable, and Strong Skipping",
        )
        assertThat(visualInfo.starterPrompts).doesNotContain(
            "What are the benefits of Circuit UDF architecture over MVI/MVVM?",
        )
    }
}
