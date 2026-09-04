package com.aichat.assistant.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aichat.assistant.R
import com.aichat.assistant.domain.model.Message
import com.aichat.assistant.domain.model.MessageStatus
import com.aichat.assistant.domain.model.Sender

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    markdownEnabled: Boolean,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onConfirmEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == Sender.USER
    var menuExpanded by remember { mutableStateOf(false) }

    val bubbleColor = when {
        message.status == MessageStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        message.status == MessageStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 18.dp
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Surface(
                color = bubbleColor,
                contentColor = contentColor,
                shape = shape,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .combinedClickable(onClick = {}, onLongClick = { menuExpanded = true })
            ) {
                if (isEditing) {
                    EditableMessageContent(
                        initialText = message.content,
                        onConfirm = onConfirmEdit,
                        onCancel = onCancelEdit
                    )
                } else {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (message.status == MessageStatus.STREAMING && message.content.isEmpty()) {
                            TypingIndicator(color = contentColor)
                        } else if (markdownEnabled) {
                            MarkdownText(markdown = message.content, color = contentColor)
                        } else {
                            Text(text = message.content, color = contentColor)
                        }
                    }
                }
            }

            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_copy)) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = { menuExpanded = false; onCopy() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_share)) },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    onClick = { menuExpanded = false; onShare() }
                )
                if (isUser) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_edit)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onStartEdit() }
                    )
                } else if (message.status == MessageStatus.ERROR) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_retry)) },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        onClick = { menuExpanded = false; onRetry() }
                    )
                } else if (message.status == MessageStatus.COMPLETE) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_regenerate)) },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        onClick = { menuExpanded = false; onRegenerate() }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_delete)) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = { menuExpanded = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun EditableMessageContent(
    initialText: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    Column(modifier = Modifier.padding(10.dp)) {
        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.widthIn(min = 220.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.action_save)) }
        }
    }
}

/** Three softly pulsing dots — used both inside a streaming bubble and, larger, under the input bar. */
@Composable
fun TypingIndicator(color: Color = MaterialTheme.colorScheme.onSurfaceVariant, dotSize: androidx.compose.ui.unit.Dp = 6.dp) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            val delayMillis = index * 150
            val scale by transition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delayMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scale)
                    .alpha(0.4f + 0.6f * scale)
                    .background(color, shape = CircleShape)
            )
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isBusy: Boolean,
    sendWithEnter: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 160.dp),
                placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 6,
                keyboardOptions = if (sendWithEnter) {
                    KeyboardOptions(imeAction = ImeAction.Send)
                } else {
                    KeyboardOptions.Default
                },
                keyboardActions = KeyboardActions(
                    onSend = { if (text.isNotBlank() && !isBusy) onSend() }
                )
            )

            val canSend = text.isNotBlank() && !isBusy
            FilledIconButton(
                onClick = { if (isBusy) onStop() else if (canSend) onSend() },
                enabled = isBusy || canSend,
                modifier = Modifier.size(48.dp)
            ) {
                AnimatedContent(targetState = isBusy, label = "send-button") { busy ->
                    if (busy) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.chat_stop_generating)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.content_desc_send)
                        )
                    }
                }
            }
        }
    }
}
