package com.aichat.assistant.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.assistant.domain.model.ApiProviderSettings
import com.aichat.assistant.domain.model.ConnectionStatus
import com.aichat.assistant.domain.model.ConnectionTestResult
import com.aichat.assistant.domain.model.Conversation
import com.aichat.assistant.domain.repository.ConversationRepository
import com.aichat.assistant.domain.repository.SettingsRepository
import com.aichat.assistant.domain.usecase.TestConnectionUseCase
import com.aichat.assistant.utils.ConnectivityObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val greetingHour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
    val connectionStatus: ConnectionStatus = ConnectionStatus.NotConfigured,
    val activeModel: String = "",
    val conversations: List<Conversation> = emptyList(),
    val searchQuery: String = "",
    val isOffline: Boolean = false
)

class HomeViewModel(
    private val conversationRepository: ConversationRepository,
    private val settingsRepository: SettingsRepository,
    private val testConnectionUseCase: TestConnectionUseCase,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.NotConfigured)
    private var lastCheckedSettings: ApiProviderSettings? = null

    val uiState: StateFlow<HomeUiState> = combine(
        connectionStatus,
        settingsRepository.providerSettings,
        searchQuery.debounce(200).distinctUntilChanged().flatMapLatest { conversationRepository.observeConversations(it) },
        connectivityObserver.observe(),
        searchQuery
    ) { status, settings, conversations, online, query ->
        HomeUiState(
            connectionStatus = status,
            activeModel = settings.model,
            conversations = conversations,
            searchQuery = query,
            isOffline = !online
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            settingsRepository.providerSettings.collectLatest { settings ->
                if (!settings.isConfigured) {
                    connectionStatus.value = ConnectionStatus.NotConfigured
                    lastCheckedSettings = settings
                    return@collectLatest
                }
                // Re-check whenever the meaningful connection fields actually change, not on
                // every keystroke elsewhere in Settings.
                if (settings != lastCheckedSettings) {
                    lastCheckedSettings = settings
                    checkConnection(settings)
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun togglePin(conversationId: String) {
        viewModelScope.launch { conversationRepository.togglePin(conversationId) }
    }

    fun retryConnectionCheck() {
        lastCheckedSettings?.let { checkConnection(it) }
    }

    private fun checkConnection(settings: ApiProviderSettings) {
        connectionStatus.value = ConnectionStatus.Checking
        viewModelScope.launch {
            connectionStatus.value = when (val result = testConnectionUseCase(settings)) {
                is ConnectionTestResult.Success -> ConnectionStatus.Connected
                is ConnectionTestResult.Failure -> ConnectionStatus.Error(result.error)
            }
        }
    }
}
