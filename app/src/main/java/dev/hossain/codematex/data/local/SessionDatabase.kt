package dev.hossain.codematex.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
