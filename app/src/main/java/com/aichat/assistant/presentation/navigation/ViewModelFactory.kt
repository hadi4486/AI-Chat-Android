package com.aichat.assistant.presentation.navigation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aichat.assistant.AppContainer
import com.aichat.assistant.presentation.chat.ChatViewModel
import com.aichat.assistant.presentation.history.HistoryViewModel
import com.aichat.assistant.presentation.home.HomeViewModel
import com.aichat.assistant.presentation.settings.SettingsViewModel

/** Screens that only need the container (no per-navigation-entry argument). */
fun appViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        HomeViewModel(
            conversationRepository = container.conversationRepository,
            settingsRepository = container.settingsRepository,
            testConnectionUseCase = container.testConnectionUseCase,
            connectivityObserver = container.connectivityObserver
        )
    }
    initializer {
        SettingsViewModel(
            settingsRepository = container.settingsRepository,
            fetchModelsUseCase = container.fetchModelsUseCase,
            testConnectionUseCase = container.testConnectionUseCase
        )
    }
    initializer {
        HistoryViewModel(conversationRepository = container.conversationRepository)
    }
}

/** Chat needs the conversation id from the nav route, so it gets its own small factory. */
fun chatViewModelFactory(container: AppContainer, conversationId: String): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            ChatViewModel(
                conversationId = conversationId,
                conversationRepository = container.conversationRepository,
                settingsRepository = container.settingsRepository,
                sendMessageUseCase = container.sendMessageUseCase
            )
        }
    }
