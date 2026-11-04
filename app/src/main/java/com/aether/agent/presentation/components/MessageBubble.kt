package com.aether.agent.presentation.components
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.agent.domain.model.Message
@Composable fun MessageBubble(message:Message){ Row(Modifier.fillMaxWidth(),horizontalArrangement=if(message.role=="user") Arrangement.End else Arrangement.Start){ Surface(color=if(message.role=="user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth(.88f)){ Column(Modifier.padding(14.dp)){Text(if(message.role=="user") "Kamu" else "Aether",style=MaterialTheme.typography.labelSmall);MarkdownText(message.content)} } } }
