package com.aether.agent.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.agent.presentation.ui.theme.AetherMono
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import kotlinx.coroutines.delay

private val keywordRegex = Regex(
    "\\b(fun|val|var|class|object|interface|import|package|return|if|else|when|for|while|suspend|override|private|public|const|def|function|let|async|await|from|true|false|null|self)\\b"
)
private val stringRegex = Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'")
private val commentRegex = Regex("//.*|#.*")

private val KeywordColor = Color(0xFF7DD3FC)
private val StringColor = Color(0xFF9FE870)
private val CommentColor = Color(0xFF6B7280)
private val CopyColor = Color(0xFFE9A38A)

/** Blok kode gelap: label bahasa di kiri atas, tombol Salin di kanan atas. */
@Composable
fun CodeBlockView(code: String, language: String) {
    val palette = LocalAetherPalette.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val highlighted = remember(code) { highlight(code) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500)
            copied = false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.codeBg),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(palette.codeHeader)
                .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                language.ifBlank { "code" }.lowercase(),
                style = MaterialTheme.typography.labelMedium,
                color = palette.muted,
                modifier = Modifier.weight(1f),
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AIcon(
                    icon = if (copied) AetherIcon.Check else AetherIcon.Copy,
                    tint = CopyColor,
                    size = 15.dp,
                    strokeWidth = 1.9.dp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (copied) "Tersalin" else "Salin",
                    style = MaterialTheme.typography.labelMedium,
                    color = CopyColor,
                )
            }
        }
        Text(
            highlighted,
            fontFamily = AetherMono,
            fontSize = 13.sp,
            color = palette.codeText,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
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
