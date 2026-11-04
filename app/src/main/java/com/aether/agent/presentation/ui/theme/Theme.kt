package com.aether.agent.presentation.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
@Composable fun AetherTheme(content:@Composable ()->Unit){val colors=if(isSystemInDarkTheme()) darkColorScheme(primary=PurpleLight,background=DarkBackground) else lightColorScheme(primary=Purple);MaterialTheme(colorScheme=colors,typography=AetherTypography,content=content)}
