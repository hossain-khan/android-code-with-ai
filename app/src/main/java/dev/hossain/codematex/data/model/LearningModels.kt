package dev.hossain.codematex.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class LearningCourse(
    val id: String,
    val language: String,
    val title: String,
    val description: String,
    val version: Int,
    val chapters: List<LearningChapter>,
) {
    val lessonCount: Int
        get() = chapters.sumOf { it.lessons.size }

    val topic: CodingTopic
        get() =
            CodingTopic.entries.find {
                it.displayName.equals(language, ignoreCase = true) ||
                    it.stableId.equals(language, ignoreCase = true) ||
                    id.startsWith(it.stableId, ignoreCase = true)
            } ?: CodingTopic.KOTLIN
}

@Immutable
data class LearningChapter(
    val id: String,
    val courseId: String,
    val order: Int,
    val title: String,
    val description: String,
    val lessons: List<LearningLesson>,
)

@Immutable
data class LearningLesson(
    val id: String,
    val chapterId: String,
    val order: Int,
    val title: String,
    val summary: String,
    val estimatedMinutes: Int,
    val blocks: List<LessonBlock>,
)

sealed interface LessonBlock {
    data class Markdown(
        val content: String,
    ) : LessonBlock

    data class Code(
        val language: String,
        val code: String,
    ) : LessonBlock

    data class Quiz(
        val question: String,
        val options: List<String>,
        val answerIndex: Int,
        val explanation: String,
    ) : LessonBlock
}

enum class LessonStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
}

@Immutable
data class LessonProgress(
    val lessonId: String,
    val courseId: String,
    val status: LessonStatus,
    val lastOpenedAt: Long,
)

@Immutable
data class CourseProgress(
    val courseId: String,
    val completedLessons: Int,
    val totalLessons: Int,
    val currentLessonId: String?,
) {
    val completionPercent: Int
        get() = if (totalLessons == 0) 0 else (completedLessons * 100 / totalLessons)
}
