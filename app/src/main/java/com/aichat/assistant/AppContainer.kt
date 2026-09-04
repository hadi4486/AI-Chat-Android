package com.aichat.assistant

import android.content.Context
import com.aichat.assistant.data.api.ChatApiClient
import com.aichat.assistant.data.database.AppDatabase
import com.aichat.assistant.data.repository.ChatRepositoryImpl
import com.aichat.assistant.data.repository.ConversationRepositoryImpl
import com.aichat.assistant.data.repository.SettingsRepositoryImpl
import com.aichat.assistant.domain.repository.ChatRepository
import com.aichat.assistant.domain.repository.ConversationRepository
import com.aichat.assistant.domain.repository.SettingsRepository
import com.aichat.assistant.domain.usecase.ExportConversationMarkdownUseCase
import com.aichat.assistant.domain.usecase.FetchModelsUseCase
import com.aichat.assistant.domain.usecase.SendMessageUseCase
import com.aichat.assistant.domain.usecase.TestConnectionUseCase
import com.aichat.assistant.security.SecureKeyStore
import com.aichat.assistant.utils.ConnectivityObserver

/**
 * Wires the whole dependency graph by hand. Everything is a `by lazy` singleton scoped to the
 * process, created once in [AiChatApplication] and handed to ViewModels via
 * [com.aichat.assistant.presentation.navigation.ViewModelFactory].
 */
class AppContainer(private val appContext: Context) {

    private val database by lazy { AppDatabase.getInstance(appContext) }
    private val secureKeyStore by lazy { SecureKeyStore(appContext) }
    private val apiClient by lazy { ChatApiClient(debugLogging = BuildConfig.DEBUG) }
    val connectivityObserver by lazy { ConnectivityObserver(appContext) }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(appContext, secureKeyStore)
    }

    val conversationRepository: ConversationRepository by lazy {
        ConversationRepositoryImpl(database.conversationDao(), database.messageDao())
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepositoryImpl(apiClient, connectivityObserver)
    }

    val sendMessageUseCase by lazy { SendMessageUseCase(chatRepository) }
    val testConnectionUseCase by lazy { TestConnectionUseCase(chatRepository) }
    val fetchModelsUseCase by lazy { FetchModelsUseCase(chatRepository) }
    val exportConversationMarkdownUseCase by lazy { ExportConversationMarkdownUseCase(conversationRepository) }
}
