import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
package com.aichat.assistant.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.aichat.assistant.R
import kotlinx.coroutines.delay

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class CodeBlock(val language: String?, val code: String) : MdBlock()
    data class ListBlock(val items: List<String>, val ordered: Boolean) : MdBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MdBlock()
}

private val headingRegex = Regex("^#{1,6}\\s+.*")
private val orderedItemRegex = Regex("^\\d+\\.\\s+.*")
private val tableSeparatorRegex = Regex("^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")

private fun isUnorderedItem(line: String) =
    line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ")

private fun splitTableRow(line: String): List<String> =
    line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

/**
 * Parses a (possibly incomplete — this runs on every partial chunk while streaming) Markdown
 * string into block-level pieces. An unterminated code fence at end-of-input is treated as "code
 * block so far", not an error, so a reply mid-stream still renders sensibly.
 */
private fun parseMarkdown(source: String): List<MdBlock> {
    val lines = source.lines()
    val blocks = mutableListOf<MdBlock>()
    var i = 0
    while (i < lines.size) {
        val raw = lines[i]
        val trimmed = raw.trimStart()
        when {
            raw.isBlank() -> i++

            trimmed.startsWith("```") -> {
                val language = trimmed.removePrefix("```").trim().ifBlank { null }
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    codeLines.add(lines[i]); i++
                }
                if (i < lines.size) i++ // consume closing fence, if we found one
                blocks.add(MdBlock.CodeBlock(language, codeLines.joinToString("\n")))
            }

            headingRegex.matches(trimmed) -> {
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                blocks.add(MdBlock.Heading(level, trimmed.drop(level).trim()))
                i++
            }

            trimmed.startsWith("|") && i + 1 < lines.size && tableSeparatorRegex.matches(lines[i + 1]) -> {
                val headers = splitTableRow(trimmed)
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trimStart().startsWith("|")) {
                    rows.add(splitTableRow(lines[i])); i++
                }
                blocks.add(MdBlock.Table(headers, rows))
            }

            isUnorderedItem(trimmed) -> {
                val items = mutableListOf<String>()
                while (i < lines.size && isUnorderedItem(lines[i].trimStart())) {
                    items.add(lines[i].trimStart().drop(2).trim()); i++
                }
                blocks.add(MdBlock.ListBlock(items, ordered = false))
            }

            orderedItemRegex.matches(trimmed) -> {
                val items = mutableListOf<String>()
                while (i < lines.size && orderedItemRegex.matches(lines[i].trimStart())) {
                    items.add(lines[i].trimStart().replaceFirst(Regex("^\\d+\\.\\s+"), "")); i++
                }
                blocks.add(MdBlock.ListBlock(items, ordered = true))
            }

            else -> {
                val paragraphLines = mutableListOf<String>()
                while (
                    i < lines.size && lines[i].isNotBlank() &&
                    !lines[i].trimStart().startsWith("```") &&
                    !headingRegex.matches(lines[i].trimStart()) &&
                    !isUnorderedItem(lines[i].trimStart()) &&
                    !orderedItemRegex.matches(lines[i].trimStart())
                ) {
                    paragraphLines.add(lines[i]); i++
                }
                blocks.add(MdBlock.Paragraph(paragraphLines.joinToString(" ")))
            }
        }
    }
    return blocks
}

private val inlinePattern = Regex(
    "\\*\\*(.+?)\\*\\*|__(.+?)__|\\*(.+?)\\*|_(.+?)_|`(.+?)`|\\[(.+?)]\\((.+?)\\)"
)

@Composable
private fun inlineMarkdown(text: String): AnnotatedString {
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val linkColor = MaterialTheme.colorScheme.secondary
    return remember(text, codeBackground, linkColor) {
        buildAnnotatedString {
            var lastIndex = 0
            for (match in inlinePattern.findAll(text)) {
                if (match.range.first > lastIndex) {
                    append(text.substring(lastIndex, match.range.first))
                }
                val groups = match.groupValues
                when {
                    groups[1].isNotEmpty() || groups[2].isNotEmpty() -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(groups[1].ifEmpty { groups[2] })
                        }
                    }
                    groups[3].isNotEmpty() || groups[4].isNotEmpty() -> {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(groups[3].ifEmpty { groups[4] })
                        }
                    }
                    groups[5].isNotEmpty() -> {
                        withStyle(
                            SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
                        ) { append(groups[5]) }
                    }
                    groups[6].isNotEmpty() && groups[7].isNotEmpty() -> {
                        withLink(
                            LinkAnnotation.Url(
                                groups[7],
                                TextLinkStyles(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                            )
                        ) { append(groups[6]) }
                    }
                }
                lastIndex = match.range.last + 1
            }
            if (lastIndex < text.length) append(text.substring(lastIndex))
        }
    }
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = LocalContentColor.current
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> {
                    val headingStyle = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    Text(text = inlineMarkdown(block.text), style = headingStyle, color = color)
                }
                is MdBlock.Paragraph -> Text(text = inlineMarkdown(block.text), style = style, color = color)
                is MdBlock.CodeBlock -> CodeBlockView(block.language, block.code)
                is MdBlock.ListBlock -> ListBlockView(block.items, block.ordered, style, color)
                is MdBlock.Table -> TableBlockView(block.headers, block.rows, style)
            }
        }
    }
}

@Composable
private fun ListBlockView(items: List<String>, ordered: Boolean, style: TextStyle, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { index, item ->
            Row {
                Text(
                    text = if (ordered) "${index + 1}." else "•",
                    style = style,
                    color = color,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(text = inlineMarkdown(item), style = style, color = color)
            }
        }
    }
}

@Composable
private fun TableBlockView(headers: List<String>, rows: List<List<String>>, style: TextStyle) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                headers.forEach { cell ->
                    Text(
                        text = cell,
                        style = style,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }
            rows.forEach { row ->
                Row(modifier = Modifier.padding(top = 6.dp)) {
                    row.forEach { cell ->
                        Text(text = cell, style = style, modifier = Modifier.padding(end = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockView(language: String?, code: String) {
    val clipboardManager = LocalClipboardManager.current
    var justCopied by rememberSaveable(code) { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 4.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language?.uppercase() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(code))
                    justCopied = true
                }) {
                    Icon(
                        imageVector = if (justCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.content_desc_copy_code),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(1500)
            justCopied = false
        }
    }
}
