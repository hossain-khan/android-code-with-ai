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

    @Test
    fun `course shared keys have stable equality for same course id`() {
        val courseId = "kotlin-foundations"

        val cardKey1 = CourseCardSharedKey(courseId)
        val cardKey2 = CourseCardSharedKey(courseId)
        val badgeKey1 = CourseBadgeSharedKey(courseId)
        val badgeKey2 = CourseBadgeSharedKey(courseId)
        val titleKey1 = CourseTitleSharedKey(courseId)
        val titleKey2 = CourseTitleSharedKey(courseId)
        val progressKey1 = CourseProgressSharedKey(courseId)
        val progressKey2 = CourseProgressSharedKey(courseId)

        assertThat(cardKey1).isEqualTo(cardKey2)
        assertThat(badgeKey1).isEqualTo(badgeKey2)
        assertThat(titleKey1).isEqualTo(titleKey2)
        assertThat(progressKey1).isEqualTo(progressKey2)
    }

    @Test
    fun `different courses have distinct shared keys`() {
        val course1 = "kotlin-foundations"
        val course2 = "python-foundations"

        assertThat(CourseCardSharedKey(course1)).isNotEqualTo(CourseCardSharedKey(course2))
        assertThat(CourseBadgeSharedKey(course1)).isNotEqualTo(CourseBadgeSharedKey(course2))
        assertThat(CourseTitleSharedKey(course1)).isNotEqualTo(CourseTitleSharedKey(course2))
        assertThat(CourseProgressSharedKey(course1)).isNotEqualTo(CourseProgressSharedKey(course2))
    }

    @Test
    fun `lesson shared keys have stable equality for same lesson id`() {
        val lessonId = "kotlin-variables"

        val cardKey1 = LessonCardSharedKey(lessonId)
        val cardKey2 = LessonCardSharedKey(lessonId)
        val titleKey1 = LessonTitleSharedKey(lessonId)
        val titleKey2 = LessonTitleSharedKey(lessonId)

        assertThat(cardKey1).isEqualTo(cardKey2)
        assertThat(titleKey1).isEqualTo(titleKey2)
    }

    @Test
    fun `different lessons have distinct shared keys`() {
        val lesson1 = "kotlin-intro"
        val lesson2 = "kotlin-variables"

        assertThat(LessonCardSharedKey(lesson1)).isNotEqualTo(LessonCardSharedKey(lesson2))
        assertThat(LessonTitleSharedKey(lesson1)).isNotEqualTo(LessonTitleSharedKey(lesson2))
    }
}
