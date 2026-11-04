package com.aether.agent.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val keywordRegex = Regex(
    "\\b(fun|val|var|class|object|interface|import|package|return|if|else|when|for|while|suspend|override|private|public|const|def|function|let|async|await|from|true|false|null|self)\\b"
)
private val stringRegex = Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'")
private val commentRegex = Regex("//.*|#.*")

private val KeywordColor = Color(0xFF7DD3FC)
private val StringColor = Color(0xFF39FF14)
private val CommentColor = Color(0xFF6B7280)
private val CodeText = Color(0xFFE5E7EB)

/** Kotak kode dengan highlight sederhana dan tombol salin sekali sentuh. */
@Composable
fun CodeBlockView(code: String, language: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val highlighted = remember(code) { highlight(code) }

    Surface(
        color = Color(0xFF111214),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    language.ifBlank { "kode" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(code))
                    copied = true
                }) {
                    Text(if (copied) "Tersalin" else "Salin", color = Color(0xFF39FF14))
                }
            }
            Text(
                highlighted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = CodeText,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            )
        }
    }
}

private fun highlight(code: String): AnnotatedString = buildAnnotatedString {
    append(code)
    fun paint(regex: Regex, color: Color) {
        regex.findAll(code).forEach { addStyle(SpanStyle(color = color), it.range.first, it.range.last + 1) }
    }
    paint(keywordRegex, KeywordColor)
    paint(stringRegex, StringColor)
    paint(commentRegex, CommentColor)
}
