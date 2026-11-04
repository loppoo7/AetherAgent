package com.aether.agent.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private sealed interface Block {
    data class Text(val lines: List<String>) : Block
    data class Code(val lang: String, val code: String) : Block
}

/** Render Markdown ringan: heading, list, daftar bernomor, **bold**, `inline code`, dan blok kode. */
@Composable
fun MarkdownText(content: String, modifier: Modifier = Modifier) {
    val blocks = remember(content) { parseBlocks(content) }
    SelectionContainer {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is Block.Code -> CodeBlockView(code = block.code, language = block.lang)
                    is Block.Text -> TextBlock(block.lines)
                }
            }
        }
    }
}

private fun parseBlocks(content: String): List<Block> {
    val blocks = mutableListOf<Block>()
    val text = mutableListOf<String>()
    val code = mutableListOf<String>()
    var lang = ""
    var inCode = false
    for (line in content.lines()) {
        if (line.trimStart().startsWith("```")) {
            if (inCode) {
                blocks += Block.Code(lang, code.joinToString("\n"))
                code.clear()
                inCode = false
            } else {
                if (text.isNotEmpty()) {
                    blocks += Block.Text(text.toList())
                    text.clear()
                }
                lang = line.trimStart().removePrefix("```").trim()
                inCode = true
            }
        } else if (inCode) {
            code += line
        } else {
            text += line
        }
    }
    if (inCode) blocks += Block.Code(lang, code.joinToString("\n"))
    else if (text.isNotEmpty()) blocks += Block.Text(text.toList())
    return blocks
}

@Composable
private fun TextBlock(lines: List<String>) {
    val numbered = Regex("^(\\d+)\\. (.*)")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { raw ->
            val line = raw.trimEnd()
            val match = numbered.find(line)
            when {
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                line.startsWith("### ") -> Text(
                    inlineMarkdown(line.removePrefix("### ")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                line.startsWith("## ") -> Text(
                    inlineMarkdown(line.removePrefix("## ")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                line.startsWith("# ") -> Text(
                    inlineMarkdown(line.removePrefix("# ")),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                line.startsWith("- ") || line.startsWith("* ") -> Row(verticalAlignment = Alignment.Top) {
                    Text("•", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(inlineMarkdown(line.drop(2)), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                match != null -> Row(verticalAlignment = Alignment.Top) {
                    Text("${match.groupValues[1]}.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(inlineMarkdown(match.groupValues[2]), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                else -> Text(inlineMarkdown(line), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private val inlineRegex = Regex("\\*\\*(.+?)\\*\\*|`([^`]+)`")

internal fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    inlineRegex.findAll(text).forEach { m ->
        append(text.substring(last, m.range.first))
        val bold = m.groups[1]
        if (bold != null) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
        } else {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x33888888))) {
                append(m.groupValues[2])
            }
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}
