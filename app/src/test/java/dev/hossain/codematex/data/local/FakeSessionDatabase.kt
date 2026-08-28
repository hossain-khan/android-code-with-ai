package dev.hossain.codematex.data.local

import androidx.room.InvalidationTracker

@Suppress("DEPRECATION")
class FakeSessionDatabase(
    private val dao: SessionDao = FakeSessionDao(),
    private val progressDao: LessonProgressDao = FakeLessonProgressDao(),
) : SessionDatabase() {
    override fun sessionDao(): SessionDao = dao

    override fun lessonProgressDao(): LessonProgressDao = progressDao

    override fun createInvalidationTracker(): InvalidationTracker =
        InvalidationTracker(
            this,
            "sessions",
            "messages",
            "lesson_progress",
        )

    override fun clearAllTables() {
        // No-op for tests.
    }
}

private class FakeLessonProgressDao : LessonProgressDao {
    private val values = mutableMapOf<String, LessonProgressEntity>()

    override fun observeCourseProgress(courseId: String): kotlinx.coroutines.flow.Flow<List<LessonProgressEntity>> =
        kotlinx.coroutines.flow.flowOf(values.values.filter { it.courseId == courseId })

    override fun observeLessonProgress(lessonId: String): kotlinx.coroutines.flow.Flow<LessonProgressEntity?> =
        kotlinx.coroutines.flow.flowOf(values[lessonId])

    override suspend fun getLessonProgress(lessonId: String): LessonProgressEntity? = values[lessonId]

    override suspend fun upsert(progress: LessonProgressEntity) {
        values[progress.lessonId] = progress
    }

    override suspend fun deleteCourseProgress(courseId: String) {
        values.entries.removeIf { it.value.courseId == courseId }
    }
}
