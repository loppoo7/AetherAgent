package com.aether.agent.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.agent.presentation.ui.theme.AetherMono

@Composable
fun DiffViewer(diff: String, modifier: Modifier = Modifier) {
    val addedBg = Color(0x2222C55E)
    val removedBg = Color(0x22EF4444)
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
            if (diff.isBlank()) {
                Text("(tidak ada perubahan teks)", modifier = Modifier.padding(10.dp), fontSize = 12.sp)
            } else {
                diff.lineSequence().forEach { line ->
                    val added = line.startsWith("+") && !line.startsWith("+++")
                    val removed = line.startsWith("-") && !line.startsWith("---")
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (added) addedBg else if (removed) removedBg else Color.Transparent)
                            .padding(horizontal = 9.dp, vertical = 2.dp),
                    ) {
                        Text(
                            line,
                            fontFamily = AetherMono,
                            fontSize = 11.sp,
                            color = when {
                                added -> Color(0xFF16A34A)
                                removed -> Color(0xFFDC2626)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}
