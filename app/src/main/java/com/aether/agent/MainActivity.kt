package com.aether.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.presentation.navigation.AppNavHost
import com.aether.agent.presentation.ui.theme.AetherTheme
import com.aether.agent.presentation.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app: AppViewModel = hiltViewModel()
            val themeMode by app.themeMode.collectAsState()
            AetherTheme(mode = themeMode) { AppNavHost() }
        }
    }
}
