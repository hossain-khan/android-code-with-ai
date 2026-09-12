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
