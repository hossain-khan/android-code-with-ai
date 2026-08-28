package dev.hossain.codematex.data.local

import androidx.room.Entity
import dev.hossain.codematex.data.model.LessonStatus

@Entity(
    tableName = "lesson_progress",
    primaryKeys = ["lessonId"],
)
data class LessonProgressEntity(
    val lessonId: String,
    val courseId: String,
    val status: String,
    val lastOpenedAt: Long,
)

fun LessonProgressEntity.toLessonStatus(): LessonStatus = LessonStatus.entries.find { it.name == status } ?: LessonStatus.NOT_STARTED
