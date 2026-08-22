package dev.hossain.codematex.data.local

import androidx.room.InvalidationTracker

@Suppress("DEPRECATION")
class FakeSessionDatabase(
    private val dao: SessionDao = FakeSessionDao(),
) : SessionDatabase() {
    override fun sessionDao(): SessionDao = dao

    override fun createInvalidationTracker(): InvalidationTracker =
        InvalidationTracker(
            this,
            "sessions",
            "messages",
        )

    override fun clearAllTables() {
        // No-op for tests.
    }
}
