package dev.hossain.codematex.data.repository.course

import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.CourseProgress
import dev.hossain.codematex.data.model.LearningChapter
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import dev.hossain.codematex.data.model.LessonStatus
import kotlinx.coroutines.flow.Flow

interface LearningRepository {
    /** Observes all bundled learning courses available to the app. */
    fun getCourses(): Flow<List<LearningCourse>>

    /** Returns the course with [courseId], or `null` when it is not bundled. */
    suspend fun getCourse(courseId: String): LearningCourse?

    /** Returns the chapter with [chapterId], or `null` when it is not bundled. */
    suspend fun getChapter(chapterId: String): LearningChapter?

    /** Returns the lesson with [lessonId], or `null` when it is not bundled. */
    suspend fun getLesson(lessonId: String): LearningLesson?

    /** Returns the parent course containing [lessonId], or `null` if not found. */
    suspend fun getCourseForLesson(lessonId: String): LearningCourse?

    /** Returns the course associated with [topic], or `null` if none exists. */
    suspend fun getCourseForTopic(topic: CodingTopic): LearningCourse?

    /** Returns all coding topics that currently have a bundled guided course. */
    suspend fun getTopicsWithCourses(): Set<CodingTopic>

    /** Observes aggregate completion and resume information for [courseId]. */
    fun observeCourseProgress(courseId: String): Flow<CourseProgress>

    /** Observes the persisted status for [lessonId]. */
    fun observeLessonStatus(lessonId: String): Flow<LessonStatus>

    /** Marks [lessonId] as started without downgrading a completed lesson. */
    suspend fun markLessonStarted(lessonId: String)

    /** Marks [lessonId] as completed and persists the update locally. */
    suspend fun markLessonCompleted(lessonId: String)

    /** Removes all persisted lesson progress for [courseId]. */
    suspend fun resetCourseProgress(courseId: String)
}
