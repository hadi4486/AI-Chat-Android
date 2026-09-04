package com.aichat.assistant.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    // Pinned conversations always float to the top, most-recently-active first within each group.
    @Query(
        """
        SELECT * FROM conversations
        WHERE title LIKE '%' || :query || '%' OR last_message_preview LIKE '%' || :query || '%'
        ORDER BY is_pinned DESC, updated_at DESC
        """
    )
    fun observeConversations(query: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Query("UPDATE conversations SET is_pinned = NOT is_pinned WHERE id = :id")
    suspend fun togglePin(id: String)

    @Query("UPDATE conversations SET title = :title, updated_at = :updatedAt WHERE id = :id")
    suspend fun rename(id: String, title: String, updatedAt: Long)

    @Query(
        "UPDATE conversations SET last_message_preview = :preview, updated_at = :updatedAt WHERE id = :id"
    )
    suspend fun touch(id: String, preview: String, updatedAt: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY created_at ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Update
    suspend fun update(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM messages WHERE conversation_id = :conversationId")
    suspend fun clearForConversation(conversationId: String)

    @Delete
    suspend fun deleteEntity(message: MessageEntity)
}
