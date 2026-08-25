package dev.hossain.codematex.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY lastActiveAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionById(sessionId: String): Flow<SessionEntity?>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY orderIndex")
    suspend fun getMessages(sessionId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionEntity)

    @Insert
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessages(sessionId: String)

    /**
     * Atomically replaces the session and its messages. Any failure during deletion, upsert, or
     * insertion rolls back the entire transaction, leaving the previous session state intact.
     */
    @Transaction
    suspend fun replaceSession(
        session: SessionEntity,
        messages: List<MessageEntity>,
    ) {
        deleteMessages(session.id)
        upsertSession(session)
        insertMessages(messages)
    }
}
