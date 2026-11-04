package com.aether.agent.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.aether.agent.domain.model.Message
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import kotlinx.coroutines.delay

@Composable
fun ChatBubble(message: Message) {
    if (message.role == "user") {
        UserBubble(message.content)
    } else {
        AssistantMessage(message.content)
    }
}

/** Pesan user: bubble lembut di kanan, sudut kanan bawah tajam. */
@Composable
private fun UserBubble(text: String) {
    val palette = LocalAetherPalette.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = palette.userBubble,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 6.dp),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            SelectionContainer {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

/** Pesan AI: tanpa bubble, rata kiri, bintang kecil di atas dan tombol salin di bawah. */
@Composable
private fun AssistantMessage(text: String) {
    val palette = LocalAetherPalette.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500)
            copied = false
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AIcon(AetherIcon.Sparkle, tint = Terracotta, size = 16.dp)
        MarkdownText(text)
        IconAction(
            icon = if (copied) AetherIcon.Check else AetherIcon.Copy,
            description = "Salin jawaban",
            onClick = {
                clipboard.setText(AnnotatedString(text))
                copied = true
            },
            tint = palette.muted,
            boxSize = 36.dp,
            iconSize = 18.dp,
        )
    }
}
