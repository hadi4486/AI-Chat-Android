package com.aichat.assistant.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.assistant.domain.model.AppError
import com.aichat.assistant.domain.model.ChatPreferences
import com.aichat.assistant.domain.model.ThemeMode
import com.aichat.assistant.domain.repository.Outcome
import com.aichat.assistant.domain.repository.SettingsRepository
import com.aichat.assistant.domain.usecase.FetchModelsUseCase
import com.aichat.assistant.domain.usecase.TestConnectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ConnectionTestUiState {
    data object Idle : ConnectionTestUiState
    data object Running : ConnectionTestUiState
    data object Success : ConnectionTestUiState
    data class Failure(val error: AppError) : ConnectionTestUiState
}

data class SettingsUiState(
    val providerName: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val systemPrompt: String = "",

    val streamingEnabled: Boolean = true,
    val autoSaveEnabled: Boolean = true,
    val markdownEnabled: Boolean = true,
    val sendWithEnter: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,

    val apiKeyVisible: Boolean = false,
    val availableModels: List<String> = emptyList(),
    val isRefreshingModels: Boolean = false,
    val connectionTest: ConnectionTestUiState = ConnectionTestUiState.Idle,
    val isSaved: Boolean = false,
    val isLoaded: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val fetchModelsUseCase: FetchModelsUseCase,
    private val testConnectionUseCase: TestConnectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.providerSettings.first()
            val preferences = settingsRepository.chatPreferences.first()
            _uiState.value = _uiState.value.copy(
                providerName = settings.providerName,
                baseUrl = settings.baseUrl,
                apiKey = settings.apiKey,
                model = settings.model,
                temperature = settings.temperature,
                maxTokens = settings.maxTokens,
                systemPrompt = settings.systemPrompt,
                streamingEnabled = preferences.streamingEnabled,
                autoSaveEnabled = preferences.autoSaveEnabled,
                markdownEnabled = preferences.markdownEnabled,
                sendWithEnter = preferences.sendWithEnter,
                themeMode = preferences.themeMode,
                dynamicColorEnabled = preferences.dynamicColorEnabled,
                isLoaded = true
            )
        }
    }

    fun onProviderNameChange(value: String) = update { it.copy(providerName = value, isSaved = false) }
    fun onBaseUrlChange(value: String) = update { it.copy(baseUrl = value, isSaved = false) }
    fun onApiKeyChange(value: String) = update { it.copy(apiKey = value, isSaved = false) }
    fun onModelChange(value: String) = update { it.copy(model = value, isSaved = false) }
    fun onTemperatureChange(value: Float) = update { it.copy(temperature = value, isSaved = false) }
    fun onMaxTokensChange(value: Int) = update { it.copy(maxTokens = value, isSaved = false) }
    fun onSystemPromptChange(value: String) = update { it.copy(systemPrompt = value, isSaved = false) }
    fun toggleApiKeyVisibility() = update { it.copy(apiKeyVisible = !it.apiKeyVisible) }

    fun onStreamingChange(value: Boolean) = updateAndPersistPreferences { it.copy(streamingEnabled = value) }
    fun onAutoSaveChange(value: Boolean) = updateAndPersistPreferences { it.copy(autoSaveEnabled = value) }
    fun onMarkdownChange(value: Boolean) = updateAndPersistPreferences { it.copy(markdownEnabled = value) }
    fun onSendWithEnterChange(value: Boolean) = updateAndPersistPreferences { it.copy(sendWithEnter = value) }
    fun onThemeModeChange(value: ThemeMode) = updateAndPersistPreferences { it.copy(themeMode = value) }
    fun onDynamicColorChange(value: Boolean) = updateAndPersistPreferences { it.copy(dynamicColorEnabled = value) }

    private fun update(block: (SettingsUiState) -> SettingsUiState) {
        _uiState.value = block(_uiState.value)
    }

    /** Appearance/Chat toggles apply immediately — only provider credentials need an explicit Save. */
    private fun updateAndPersistPreferences(block: (SettingsUiState) -> SettingsUiState) {
        val next = block(_uiState.value)
        _uiState.value = next
        viewModelScope.launch {
            settingsRepository.updateChatPreferences(
                ChatPreferences(
                    streamingEnabled = next.streamingEnabled,
                    autoSaveEnabled = next.autoSaveEnabled,
                    markdownEnabled = next.markdownEnabled,
                    sendWithEnter = next.sendWithEnter,
                    themeMode = next.themeMode,
                    dynamicColorEnabled = next.dynamicColorEnabled
                )
            )
        }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            settingsRepository.updateProviderSettings(state.toProviderSettings())
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            settingsRepository.clearApiKey()
            update { it.copy(apiKey = "") }
        }
    }

    fun refreshModels() {
        val state = _uiState.value
        if (state.baseUrl.isBlank() || state.apiKey.isBlank()) return
        update { it.copy(isRefreshingModels = true) }
        viewModelScope.launch {
            when (val result = fetchModelsUseCase(state.toProviderSettings())) {
                is Outcome.Ok -> update { it.copy(availableModels = result.value, isRefreshingModels = false) }
                is Outcome.Err -> update { it.copy(availableModels = emptyList(), isRefreshingModels = false) }
            }
        }
    }

    fun testConnection() {
        val state = _uiState.value
        update { it.copy(connectionTest = ConnectionTestUiState.Running) }
        viewModelScope.launch {
            when (val result = testConnectionUseCase(state.toProviderSettings())) {
                is com.aichat.assistant.domain.model.ConnectionTestResult.Success -> {
                    update { it.copy(connectionTest = ConnectionTestUiState.Success) }
                    if (result.availableModels.isNotEmpty()) {
                        update { it.copy(availableModels = result.availableModels) }
                    }
                }
                is com.aichat.assistant.domain.model.ConnectionTestResult.Failure -> {
                    update { it.copy(connectionTest = ConnectionTestUiState.Failure(result.error)) }
                }
            }
        }
    }

    private fun SettingsUiState.toProviderSettings() = com.aichat.assistant.domain.model.ApiProviderSettings(
        providerName = providerName,
        baseUrl = baseUrl.trim(),
        apiKey = apiKey.trim(),
        model = model.trim(),
        temperature = temperature,
        maxTokens = maxTokens,
        systemPrompt = systemPrompt
    )
}
