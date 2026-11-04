package com.aether.agent.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aether.agent.domain.model.Message
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta

@Composable
fun ChatBubble(message: Message) {
    if (message.role == "user") {
        UserBubble(message.content)
    } else {
        AssistantMessage(message.content)
    }
}

/** Pesan user: bubble krem/abu lembut di kanan, sudut kanan bawah tajam. */
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

/** Pesan AI: tanpa bubble, rata kiri di atas latar, dengan avatar kecil di atas. */
@Composable
private fun AssistantMessage(text: String) {
    val palette = LocalAetherPalette.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Terracotta),
                contentAlignment = Alignment.Center,
            ) {
                Text("A", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
            Text("AetherAgent", style = MaterialTheme.typography.labelMedium, color = palette.muted)
        }
        MarkdownText(text)
    }
}
