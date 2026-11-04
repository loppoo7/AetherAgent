package com.aether.agent.presentation.navigation
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aether.agent.presentation.ui.chat.ChatScreen
import com.aether.agent.presentation.ui.settings.SettingsScreen
@Composable fun AppNavHost(){val nav=rememberNavController();NavHost(navController=nav,startDestination="chat"){composable("chat"){ChatScreen(onSettings={nav.navigate("settings")})};composable("settings"){SettingsScreen(onBack={nav.popBackStack()})}}}
