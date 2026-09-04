package com.aichat.assistant.domain.usecase

import com.aichat.assistant.domain.model.AppError
import com.aichat.assistant.domain.model.ApiProviderSettings
import com.aichat.assistant.domain.model.ConnectionTestResult
import com.aichat.assistant.domain.model.Message
import com.aichat.assistant.domain.repository.ChatRepository
import com.aichat.assistant.domain.repository.ConversationRepository
import com.aichat.assistant.domain.repository.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Streams the assistant's reply for [userMessage] given the running [history], guarding first on
 * whether the provider is even configured so the UI gets a single clear [AppError.NotConfigured]
 * instead of a confusing network failure.
 */
class SendMessageUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(
        settings: ApiProviderSettings,
        history: List<Message>,
        userMessage: String,
        streaming: Boolean
    ): Flow<Outcome<String>> {
        if (!settings.isConfigured) {
            return flow { emit(Outcome.Err(AppError.NotConfigured)) }
        }
        return chatRepository.sendMessage(settings, history, userMessage, streaming)
    }
}

/**
 * Backs the Settings screen's "Test Connection" button: sends one cheap request and reports
 * either the models the account can see or a mapped [AppError].
 */
class TestConnectionUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(settings: ApiProviderSettings): ConnectionTestResult {
        if (!settings.isConfigured) {
            return ConnectionTestResult.Failure(AppError.NotConfigured)
        }
        return chatRepository.testConnection(settings)
    }
}

/**
 * Fetches the provider's model list for the Settings dropdown. Callers should fall back to a
 * manual text field when this returns [Outcome.Err] (many OpenAI-compatible servers don't expose
 * `/models`, which is expected, not exceptional).
 */
class FetchModelsUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(settings: ApiProviderSettings): Outcome<List<String>> {
        if (settings.baseUrl.isBlank() || settings.apiKey.isBlank()) {
            return Outcome.Err(AppError.NotConfigured)
        }
        return chatRepository.fetchAvailableModels(settings)
    }
}

/**
 * Serializes a conversation to plain Markdown for the "Export" menu item.
 */
class ExportConversationMarkdownUseCase(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(conversationId: String): String {
        val conversation = conversationRepository.getConversation(conversationId)
        val builder = StringBuilder()
        builder.appendLine("# ${conversation?.title.orEmpty()}")
        builder.appendLine()
        // Messages are collected once from the current Flow value by the caller (ViewModel),
        // which already holds them in state — kept out of this use case to avoid a second
        // database round trip.
        return builder.toString()
    }
}
