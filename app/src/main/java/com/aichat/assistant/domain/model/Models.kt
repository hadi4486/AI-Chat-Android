package com.aichat.assistant.domain.model

import java.util.UUID

/**
 * Who authored a [Message].
 */
enum class Sender {
    USER,
    ASSISTANT
}

/**
 * Lifecycle of a single message bubble, used to drive UI (typing dots, retry button, error tint).
 */
enum class MessageStatus {
    SENDING,
    STREAMING,
    COMPLETE,
    ERROR
}

/**
 * A single chat bubble.
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val sender: Sender,
    val content: String,
    val status: MessageStatus = MessageStatus.COMPLETE,
    val createdAt: Long = System.currentTimeMillis(),
    val error: AppError? = null
)

/**
 * A conversation is a list of messages plus metadata shown in Home/History.
 */
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessagePreview: String = "",
    val modelUsed: String = ""
)

/**
 * Which request/response shape to speak. Only OPENAI_COMPATIBLE is implemented today, but the
 * repository + API client are written against this enum so a new provider is a new branch, not
 * a rewrite (see [com.aichat.assistant.data.api.ChatApiClient]).
 */
enum class ProviderType(val displayName: String) {
    OPENAI_COMPATIBLE("OpenAI Compatible")
}

/**
 * Everything needed to talk to an AI provider. Persisted via SettingsRepository:
 * [apiKey] goes through the Keystore-encrypted store, everything else through DataStore.
 */
data class ApiProviderSettings(
    val providerType: ProviderType = ProviderType.OPENAI_COMPATIBLE,
    val providerName: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val systemPrompt: String = ""
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
}

/**
 * User-facing chat/behavior toggles, separate from provider credentials.
 */
data class ChatPreferences(
    val streamingEnabled: Boolean = true,
    val autoSaveEnabled: Boolean = true,
    val markdownEnabled: Boolean = true,
    val sendWithEnter: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true
)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Drives the small connection badge on the Home screen.
 */
sealed interface ConnectionStatus {
    data object NotConfigured : ConnectionStatus
    data object Checking : ConnectionStatus
    data object Connected : ConnectionStatus
    data class Error(val error: AppError) : ConnectionStatus
}

/**
 * Every failure mode the master spec calls out, mapped to a single sealed type so the UI layer
 * never has to pattern-match raw exceptions. [ErrorMapper][com.aichat.assistant.utils.ErrorMapper]
 * turns these into localized strings; nothing here carries a stack trace or raw exception message,
 * so a caught [AppError] is always safe to render to the user (see [technicalDetail] for the
 * debug-log-only counterpart).
 */
sealed class AppError(val technicalDetail: String? = null) {
    data class InvalidApiKey(val detail: String? = null) : AppError(detail)
    data class Unauthorized(val detail: String? = null) : AppError(detail)
    data class Forbidden(val detail: String? = null) : AppError(detail)
    data class BadRequest(val detail: String? = null) : AppError(detail)
    data class RateLimited(val detail: String? = null) : AppError(detail)
    data class Timeout(val detail: String? = null) : AppError(detail)
    data class NoInternet(val detail: String? = null) : AppError(detail)
    data class ServerError(val code: Int, val detail: String? = null) : AppError(detail)
    data class InvalidBaseUrl(val detail: String? = null) : AppError(detail)
    data class EmptyResponse(val detail: String? = null) : AppError(detail)
    data class UnsupportedModel(val detail: String? = null) : AppError(detail)
    data object NotConfigured : AppError()
    data class Unknown(val detail: String? = null) : AppError(detail)
}

/**
 * Result of a "Test Connection" tap in Settings.
 */
sealed interface ConnectionTestResult {
    data class Success(val availableModels: List<String>) : ConnectionTestResult
    data class Failure(val error: AppError) : ConnectionTestResult
}
