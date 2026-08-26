package dev.hossain.codematex.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for persisting chat sessions and messages in Room.
 */
@Dao
interface SessionDao {
    /**
     * Returns an observable [Flow] of all sessions ordered by most recent activity first.
     */
    @Query("SELECT * FROM sessions ORDER BY lastActiveAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    /**
     * Returns an observable [Flow] of the session matching [sessionId], or `null` if it does not exist.
     */
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionById(sessionId: String): Flow<SessionEntity?>

    /**
     * Retrieves all messages belonging to [sessionId] ordered sequentially by order index.
     */
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY orderIndex")
    suspend fun getMessages(sessionId: String): List<MessageEntity>

    /**
     * Inserts or updates [session].
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionEntity)

    /**
     * Inserts a list of [messages] into the database.
     */
    @Insert
    suspend fun insertMessages(messages: List<MessageEntity>)

    /**
     * Deletes the session row matching [sessionId].
     */
    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    /**
     * Deletes all messages associated with [sessionId].
     */
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
