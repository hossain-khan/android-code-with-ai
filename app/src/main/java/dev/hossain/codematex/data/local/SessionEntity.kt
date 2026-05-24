package dev.hossain.codematex.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val topic: String,
    val title: String,
    val summary: String,
    val messageCount: Int,
    val lastActiveAt: Long,
    val modelUsed: String,
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val type: String,
    val content: String,
    val timestamp: Long,
    val orderIndex: Int,
)
