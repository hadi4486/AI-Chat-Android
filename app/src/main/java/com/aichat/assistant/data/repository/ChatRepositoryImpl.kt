package com.aichat.assistant.data.repository

import com.aichat.assistant.data.api.ChatApiClient
import com.aichat.assistant.domain.model.AppError
import com.aichat.assistant.domain.model.ApiProviderSettings
import com.aichat.assistant.domain.model.ConnectionTestResult
import com.aichat.assistant.domain.model.Message
import com.aichat.assistant.domain.repository.ChatRepository
import com.aichat.assistant.domain.repository.Outcome
import com.aichat.assistant.utils.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ChatRepositoryImpl(
    private val apiClient: ChatApiClient,
    private val connectivityObserver: ConnectivityObserver
) : ChatRepository {

    override fun sendMessage(
        settings: ApiProviderSettings,
        history: List<Message>,
        userMessage: String,
        streaming: Boolean
    ): Flow<Outcome<String>> = flow {
        if (!connectivityObserver.isCurrentlyOnline()) {
            emit(Outcome.Err(AppError.NoInternet()))
            return@flow
        }
        if (streaming) {
            emitAll(apiClient.streamChatCompletion(settings, history, userMessage))
        } else {
            emit(apiClient.sendChatCompletion(settings, history, userMessage))
        }
    }

    override suspend fun testConnection(settings: ApiProviderSettings): ConnectionTestResult {
        if (!connectivityObserver.isCurrentlyOnline()) {
            return ConnectionTestResult.Failure(AppError.NoInternet())
        }
        return apiClient.testConnection(settings)
    }

    override suspend fun fetchAvailableModels(settings: ApiProviderSettings): Outcome<List<String>> {
        if (!connectivityObserver.isCurrentlyOnline()) {
            return Outcome.Err(AppError.NoInternet())
        }
        return apiClient.fetchModels(settings)
    }
}
