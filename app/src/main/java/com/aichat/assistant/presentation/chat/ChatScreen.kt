package com.aichat.assistant.presentation.chat

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichat.assistant.R
import com.aichat.assistant.presentation.components.ChatInputBar
import com.aichat.assistant.presentation.components.EmptyState
import com.aichat.assistant.presentation.components.MessageBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.conversationTitle.ifBlank { stringResource(R.string.chat_new_conversation_title) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (state.modelName.isNotBlank()) {
                            Text(text = state.modelName, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.content_desc_more))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_menu_pin)) },
                            leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                            onClick = { menuExpanded = false; viewModel.togglePin() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_menu_export_markdown)) },
                            onClick = { menuExpanded = false; shareText(viewModel.buildMarkdownExport()) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_menu_export_json)) },
                            onClick = { menuExpanded = false; shareText(viewModel.buildJsonExport()) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_clear_conversation)) },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                            onClick = { menuExpanded = false; showClearConfirm = true }
                        )
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                text = state.inputText,
                onTextChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopGenerating,
                isBusy = state.isBusy,
                sendWithEnter = state.sendWithEnter
            )
        }
    ) { padding ->
        if (state.messages.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.chat_empty_title),
                subtitle = stringResource(R.string.chat_empty_subtitle),
                modifier = Modifier.padding(padding).fillMaxSize()
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        markdownEnabled = state.markdownEnabled,
                        isEditing = editingMessageId == message.id,
                        onStartEdit = { editingMessageId = message.id },
                        onConfirmEdit = { newText ->
                            editingMessageId = null
                            viewModel.editMessage(message.id, newText)
                        },
                        onCancelEdit = { editingMessageId = null },
                        onCopy = { clipboardManager.setText(AnnotatedString(message.content)) },
                        onShare = { shareText(message.content) },
                        onDelete = { viewModel.deleteMessage(message.id) },
                        onRetry = { viewModel.retry(message.id) },
                        onRegenerate = { viewModel.regenerate(message.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.chat_clear_conversation)) },
            text = { Text(stringResource(R.string.chat_clear_conversation_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearConversation()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
