package com.aether.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aether.agent.presentation.navigation.AppNavHost
import com.aether.agent.presentation.ui.theme.AetherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { AetherTheme { AppNavHost() } } }
}
