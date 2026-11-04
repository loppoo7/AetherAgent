package com.aether.agent.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.agent.domain.model.Message

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.role == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.97f),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    if (isUser) "Kamu" else "Aether",
                    style = MaterialTheme.typography.labelSmall,
                )
                if (isUser) {
                    Text(message.content, style = MaterialTheme.typography.bodyMedium)
                } else {
                    MarkdownText(message.content)
                }
            }
        }
    }
}
