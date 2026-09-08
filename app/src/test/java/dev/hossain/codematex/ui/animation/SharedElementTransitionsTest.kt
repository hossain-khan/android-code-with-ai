package dev.hossain.codematex.ui.animation

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.CodingTopic
import org.junit.Test

class SharedElementTransitionsTest {
    @Test
    fun `topic shared keys have stable equality for same topic`() {
        val topic = CodingTopic.KOTLIN
        val glyphKey1 = TopicGlyphSharedKey(topic.stableId)
        val glyphKey2 = TopicGlyphSharedKey(topic.stableId)
        val titleKey1 = TopicTitleSharedKey(topic.stableId)
        val titleKey2 = TopicTitleSharedKey(topic.stableId)
        val cardKey1 = TopicCardSharedKey(topic.stableId)
        val cardKey2 = TopicCardSharedKey(topic.stableId)

        assertThat(glyphKey1).isEqualTo(glyphKey2)
        assertThat(titleKey1).isEqualTo(titleKey2)
        assertThat(cardKey1).isEqualTo(cardKey2)
    }

    @Test
    fun `different topics have distinct shared keys`() {
        val kotlin = CodingTopic.KOTLIN
        val android = CodingTopic.ANDROID

        assertThat(TopicGlyphSharedKey(kotlin.stableId)).isNotEqualTo(TopicGlyphSharedKey(android.stableId))
        assertThat(TopicTitleSharedKey(kotlin.stableId)).isNotEqualTo(TopicTitleSharedKey(android.stableId))
        assertThat(TopicCardSharedKey(kotlin.stableId)).isNotEqualTo(TopicCardSharedKey(android.stableId))
    }

    @Test
    fun `all selectable topics produce non-empty unique shared keys`() {
        val topics = CodingTopic.selectableEntries

        val glyphKeys = topics.map { TopicGlyphSharedKey(it.stableId) }.toSet()
        val titleKeys = topics.map { TopicTitleSharedKey(it.stableId) }.toSet()
        val cardKeys = topics.map { TopicCardSharedKey(it.stableId) }.toSet()

        assertThat(glyphKeys).hasSize(topics.size)
        assertThat(titleKeys).hasSize(topics.size)
        assertThat(cardKeys).hasSize(topics.size)
    }
}
