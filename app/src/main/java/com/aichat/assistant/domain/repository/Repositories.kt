package com.aichat.assistant.domain.repository

import com.aichat.assistant.domain.model.AppError
import com.aichat.assistant.domain.model.ApiProviderSettings
import com.aichat.assistant.domain.model.ChatPreferences
import com.aichat.assistant.domain.model.ConnectionTestResult
import com.aichat.assistant.domain.model.Conversation
import com.aichat.assistant.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * A minimal Kotlin Result-alike so use cases can return typed failures ([AppError]) instead of
 * throwing. Kept local to the domain layer so it has zero framework dependencies.
 */
sealed interface Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>
    data class Err(val error: AppError) : Outcome<Nothing>
}

interface SettingsRepository {
    val providerSettings: Flow<ApiProviderSettings>
    val chatPreferences: Flow<ChatPreferences>

    suspend fun updateProviderSettings(settings: ApiProviderSettings)
    suspend fun updateChatPreferences(preferences: ChatPreferences)
    suspend fun clearApiKey()
}

interface ChatRepository {
    /**
     * Streams incremental text deltas as they arrive (empty list of one delta per SSE chunk),
     * or emits a single final chunk when the provider/settings don't support streaming.
     * Every emission is an [Outcome] so a mid-stream failure (rate limit, disconnect) surfaces
     * through the same channel the UI is already collecting.
     */
    fun sendMessage(
        settings: ApiProviderSettings,
        history: List<Message>,
        userMessage: String,
        streaming: Boolean
    ): Flow<Outcome<String>>

    suspend fun testConnection(settings: ApiProviderSettings): ConnectionTestResult

    suspend fun fetchAvailableModels(settings: ApiProviderSettings): Outcome<List<String>>
}

interface ConversationRepository {
    fun observeConversations(query: String = ""): Flow<List<Conversation>>
    fun observeMessages(conversationId: String): Flow<List<Message>>
    suspend fun getConversation(conversationId: String): Conversation?

    suspend fun createConversation(id: String, title: String, modelUsed: String): Conversation
    suspend fun saveMessage(message: Message)
    suspend fun updateMessage(message: Message)
    suspend fun deleteMessage(messageId: String)
    suspend fun renameConversation(conversationId: String, title: String)
    suspend fun togglePin(conversationId: String)
    suspend fun clearConversation(conversationId: String)
    suspend fun deleteConversation(conversationId: String)
}
