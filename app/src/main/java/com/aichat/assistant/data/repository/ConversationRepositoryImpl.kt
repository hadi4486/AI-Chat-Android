package com.aichat.assistant.data.repository

import com.aichat.assistant.data.database.ConversationDao
import com.aichat.assistant.data.database.ConversationEntity
import com.aichat.assistant.data.database.MessageDao
import com.aichat.assistant.data.database.MessageEntity
import com.aichat.assistant.domain.model.Conversation
import com.aichat.assistant.domain.model.Message
import com.aichat.assistant.domain.model.MessageStatus
import com.aichat.assistant.domain.model.Sender
import com.aichat.assistant.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private fun ConversationEntity.toDomain() = Conversation(
    id = id,
    title = title,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastMessagePreview = lastMessagePreview,
    modelUsed = modelUsed
)

private fun Conversation.toEntity() = ConversationEntity(
    id = id,
    title = title,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastMessagePreview = lastMessagePreview,
    modelUsed = modelUsed
)

private fun MessageEntity.toDomain() = Message(
    id = id,
    conversationId = conversationId,
    sender = runCatching { Sender.valueOf(sender) }.getOrDefault(Sender.USER),
    content = content,
    status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.COMPLETE),
    createdAt = createdAt,
    error = null // Persisted messages only remember that they failed (errorLabel), not the typed cause.
)

private fun Message.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    sender = sender.name,
    content = content,
    status = status.name,
    createdAt = createdAt,
    errorLabel = error?.let { it::class.simpleName }
)

class ConversationRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) : ConversationRepository {

    override fun observeConversations(query: String): Flow<List<Conversation>> =
        conversationDao.observeConversations(query).map { list -> list.map { it.toDomain() } }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        messageDao.observeMessages(conversationId).map { list -> list.map { it.toDomain() } }

    override suspend fun getConversation(conversationId: String): Conversation? =
        conversationDao.getById(conversationId)?.toDomain()

    override suspend fun createConversation(id: String, title: String, modelUsed: String): Conversation {
        val now = System.currentTimeMillis()
        val conversation = Conversation(
            id = id,
            title = title,
            createdAt = now,
            updatedAt = now,
            modelUsed = modelUsed
        )
        conversationDao.upsert(conversation.toEntity())
        return conversation
    }

    override suspend fun saveMessage(message: Message) {
        messageDao.upsert(message.toEntity())
        conversationDao.touch(
            id = message.conversationId,
            preview = message.content.take(PREVIEW_LENGTH),
            updatedAt = message.createdAt
        )
    }

    override suspend fun updateMessage(message: Message) {
        messageDao.upsert(message.toEntity())
    }

    override suspend fun deleteMessage(messageId: String) {
        messageDao.delete(messageId)
    }

    override suspend fun renameConversation(conversationId: String, title: String) {
        conversationDao.rename(conversationId, title, System.currentTimeMillis())
    }

    override suspend fun togglePin(conversationId: String) {
        conversationDao.togglePin(conversationId)
    }

    override suspend fun clearConversation(conversationId: String) {
        messageDao.clearForConversation(conversationId)
    }

    override suspend fun deleteConversation(conversationId: String) {
        conversationDao.delete(conversationId)
    }

    companion object {
        private const val PREVIEW_LENGTH = 120
    }
}
