package dev.hossain.codematex.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SessionEntity::class, MessageEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

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
    }
}
