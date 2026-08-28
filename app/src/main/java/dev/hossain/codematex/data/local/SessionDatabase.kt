package dev.hossain.codematex.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SessionEntity::class, MessageEntity::class, LessonProgressEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun lessonProgressDao(): LessonProgressDao

    companion object {
        /**
         * Migration from version 1 to 2: adds the [MessageEntity.messageId] column
         * so that [dev.hossain.codematex.data.model.ChatMessage.id] can be restored
         * when loading messages from Room. Existing messages receive an empty
         * default value and are assigned fresh IDs at load time.
         */
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE messages ADD COLUMN messageId TEXT NOT NULL DEFAULT ''")
                }
            }

        /**
         * Migration from version 2 to 3: replaces brittle enum names and raw message type strings
         * with stable IDs. Any unrecognized values are mapped to the `unknown` stable ID so that
         * corrupt or legacy rows do not crash the sessions list.
         */
        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        UPDATE sessions SET topic = CASE topic
                            WHEN 'KOTLIN' THEN 'kotlin'
                            WHEN 'PYTHON' THEN 'python'
                            WHEN 'JAVASCRIPT' THEN 'javascript'
                            WHEN 'RUST' THEN 'rust'
                            WHEN 'GO' THEN 'go'
                            WHEN 'SWIFT' THEN 'swift'
                            WHEN 'ALGORITHMS' THEN 'algorithms'
                            WHEN 'SYSTEM_DESIGN' THEN 'system-design'
                            WHEN 'ANDROID' THEN 'android'
                            WHEN 'WEB' THEN 'web'
                            ELSE 'unknown'
                        END
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        UPDATE messages SET type = CASE type
                            WHEN 'user' THEN 'user'
                            WHEN 'agent' THEN 'agent'
                            WHEN 'error' THEN 'error'
                            WHEN 'system' THEN 'system'
                            ELSE 'unknown'
                        END
                        """.trimIndent(),
                    )
                }
            }

        /**
         * Migration from version 3 to 4: adds local Guided Lessons progress.
         */
        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS lesson_progress (
                            lessonId TEXT NOT NULL,
                            courseId TEXT NOT NULL,
                            status TEXT NOT NULL,
                            lastOpenedAt INTEGER NOT NULL,
                            PRIMARY KEY(lessonId)
                        )
                        """.trimIndent(),
                    )
                }
            }
    }
}
