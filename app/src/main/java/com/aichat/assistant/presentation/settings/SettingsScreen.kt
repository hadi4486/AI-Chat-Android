package com.aichat.assistant.presentation.settings

import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichat.assistant.BuildConfig
import com.aichat.assistant.R
import com.aichat.assistant.domain.model.ThemeMode
import com.aichat.assistant.presentation.components.ErrorInlineBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearKeyConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(stringResource(R.string.settings_section_provider))
            OutlinedTextField(
                value = state.providerName,
                onValueChange = viewModel::onProviderNameChange,
                label = { Text(stringResource(R.string.settings_provider_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = viewModel::onBaseUrlChange,
                label = { Text(stringResource(R.string.settings_base_url)) },
                placeholder = { Text("https://api.example.com/v1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                label = { Text(stringResource(R.string.settings_api_key)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (state.apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleApiKeyVisibility) {
                        Icon(
                            imageVector = if (state.apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(
                                if (state.apiKeyVisible) R.string.settings_hide_key else R.string.settings_show_key
                            )
                        )
                    }
                }
            )

            ModelDropdown(
                selectedModel = state.model,
                availableModels = state.availableModels,
                isRefreshing = state.isRefreshingModels,
                onModelChange = viewModel::onModelChange,
                onRefresh = viewModel::refreshModels
            )
            Text(
                text = stringResource(R.string.settings_model_manual_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_section_generation))
            Text("${stringResource(R.string.settings_temperature)}: ${"%.1f".format(state.temperature)}")
            Slider(
                value = state.temperature,
                onValueChange = viewModel::onTemperatureChange,
                valueRange = 0f..2f
            )
            OutlinedTextField(
                value = state.maxTokens.toString(),
                onValueChange = { text -> text.toIntOrNull()?.let(viewModel::onMaxTokensChange) },
                label = { Text(stringResource(R.string.settings_max_tokens)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = state.systemPrompt,
                onValueChange = viewModel::onSystemPromptChange,
                label = { Text(stringResource(R.string.settings_system_prompt)) },
                placeholder = { Text(stringResource(R.string.settings_system_prompt_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_section_appearance))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                    ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                    ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system)
                )
                options.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = state.themeMode == mode,
                        onClick = { viewModel.onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) { Text(label) }
                }
            }
            ToggleRow(
                label = stringResource(R.string.settings_dynamic_theme),
                checked = state.dynamicColorEnabled,
                onCheckedChange = viewModel::onDynamicColorChange
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_section_chat))
            ToggleRow(stringResource(R.string.settings_streaming), state.streamingEnabled, viewModel::onStreamingChange)
            ToggleRow(stringResource(R.string.settings_auto_save), state.autoSaveEnabled, viewModel::onAutoSaveChange)
            ToggleRow(stringResource(R.string.settings_markdown), state.markdownEnabled, viewModel::onMarkdownChange)
            ToggleRow(stringResource(R.string.settings_send_with_enter), state.sendWithEnter, viewModel::onSendWithEnterChange)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_section_connection))
            OutlinedButton(onClick = viewModel::testConnection, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_test_connection))
            }
            when (val test = state.connectionTest) {
                is ConnectionTestUiState.Running -> Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.settings_test_connection_running),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                is ConnectionTestUiState.Success -> Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.settings_test_connection_success),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                is ConnectionTestUiState.Failure -> ErrorInlineBanner(error = test.error)
                ConnectionTestUiState.Idle -> {}
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_section_security))
            Text(
                text = stringResource(R.string.settings_secure_storage_note),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.settings_client_side_warning),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { showClearKeyConfirm = true }) {
                Text(stringResource(R.string.settings_clear_api_key), color = MaterialTheme.colorScheme.error)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_section_about))
            AboutRow(stringResource(R.string.settings_about_version), BuildConfig.VERSION_NAME)
            AboutRow(stringResource(R.string.settings_about_libraries), "Jetpack Compose, Room, DataStore, OkHttp, kotlinx.serialization")

            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isSaved) stringResource(R.string.settings_saved) else stringResource(R.string.action_save))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showClearKeyConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearKeyConfirm = false },
            title = { Text(stringResource(R.string.settings_clear_api_key)) },
            text = { Text(stringResource(R.string.settings_clear_api_key_confirm)) },
            confirmButton = {
                TextButton(onClick = { showClearKeyConfirm = false; viewModel.clearApiKey() }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearKeyConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(
    selectedModel: String,
    availableModels: List<String>,
    isRefreshing: Boolean,
    onModelChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded && availableModels.isNotEmpty(), onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedModel,
            onValueChange = onModelChange,
            label = { Text(stringResource(R.string.settings_model)) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true,
            trailingIcon = {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.settings_model_refresh))
                    }
                }
            }
        )
        DropdownMenu(
            expanded = expanded && availableModels.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            availableModels.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = { onModelChange(model); expanded = false }
                )
            }
        }
    }
}
