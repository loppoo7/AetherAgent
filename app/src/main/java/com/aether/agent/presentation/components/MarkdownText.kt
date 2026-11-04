package com.aether.agent.presentation.components
import androidx.compose.foundation.text.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
@Composable fun MarkdownText(content:String,modifier:Modifier=Modifier){ SelectionContainer { Text(content,modifier=modifier) } }
