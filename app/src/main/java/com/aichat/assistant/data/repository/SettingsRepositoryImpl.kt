package com.aichat.assistant.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aichat.assistant.domain.model.ApiProviderSettings
import com.aichat.assistant.domain.model.ChatPreferences
import com.aichat.assistant.domain.model.ProviderType
import com.aichat.assistant.domain.model.ThemeMode
import com.aichat.assistant.domain.repository.SettingsRepository
import com.aichat.assistant.security.SecureKeyStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Splits storage by sensitivity: everything here except the API key is plain-preference data
 * (base URL, model name, temperature — nothing that needs Keystore protection). The key itself is
 * always delegated to [secureKeyStore] so there is exactly one code path that can read or write it.
 */
class SettingsRepositoryImpl(
    private val context: Context,
    private val secureKeyStore: SecureKeyStore
) : SettingsRepository {

    private object Keys {
        val PROVIDER_NAME = stringPreferencesKey("provider_name")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")

        val STREAMING = booleanPreferencesKey("streaming_enabled")
        val AUTO_SAVE = booleanPreferencesKey("auto_save_enabled")
        val MARKDOWN = booleanPreferencesKey("markdown_enabled")
        val SEND_WITH_ENTER = booleanPreferencesKey("send_with_enter")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
    }

    override val providerSettings: Flow<ApiProviderSettings> = context.settingsDataStore.data.map { prefs ->
        ApiProviderSettings(
            providerType = ProviderType.OPENAI_COMPATIBLE,
            providerName = prefs[Keys.PROVIDER_NAME] ?: "",
            baseUrl = prefs[Keys.BASE_URL] ?: "",
            apiKey = secureKeyStore.getApiKey(),
            model = prefs[Keys.MODEL] ?: "",
            temperature = prefs[Keys.TEMPERATURE] ?: 0.7f,
            maxTokens = prefs[Keys.MAX_TOKENS] ?: 1024,
            systemPrompt = prefs[Keys.SYSTEM_PROMPT] ?: ""
        )
    }

    override val chatPreferences: Flow<ChatPreferences> = context.settingsDataStore.data.map { prefs ->
        ChatPreferences(
            streamingEnabled = prefs[Keys.STREAMING] ?: true,
            autoSaveEnabled = prefs[Keys.AUTO_SAVE] ?: true,
            markdownEnabled = prefs[Keys.MARKDOWN] ?: true,
            sendWithEnter = prefs[Keys.SEND_WITH_ENTER] ?: false,
            themeMode = (prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name).let {
                runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
            },
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: true
        )
    }

    override suspend fun updateProviderSettings(settings: ApiProviderSettings) {
        // The key never touches DataStore — only the encrypted store sees it.
        secureKeyStore.setApiKey(settings.apiKey)
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.PROVIDER_NAME] = settings.providerName
            prefs[Keys.BASE_URL] = settings.baseUrl
            prefs[Keys.MODEL] = settings.model
            prefs[Keys.TEMPERATURE] = settings.temperature
            prefs[Keys.MAX_TOKENS] = settings.maxTokens
            prefs[Keys.SYSTEM_PROMPT] = settings.systemPrompt
        }
    }

    override suspend fun updateChatPreferences(preferences: ChatPreferences) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.STREAMING] = preferences.streamingEnabled
            prefs[Keys.AUTO_SAVE] = preferences.autoSaveEnabled
            prefs[Keys.MARKDOWN] = preferences.markdownEnabled
            prefs[Keys.SEND_WITH_ENTER] = preferences.sendWithEnter
            prefs[Keys.THEME_MODE] = preferences.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = preferences.dynamicColorEnabled
        }
    }

    override suspend fun clearApiKey() {
        secureKeyStore.clear()
    }
}
