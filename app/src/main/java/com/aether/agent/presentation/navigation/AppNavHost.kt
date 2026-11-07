package com.aether.agent.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aether.agent.presentation.ui.chat.ChatScreen
import com.aether.agent.presentation.ui.files.FileEditorScreen
import com.aether.agent.presentation.ui.files.FileManagerScreen
import com.aether.agent.presentation.ui.files.ServerListScreen
import com.aether.agent.presentation.ui.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "chat") {
        composable("chat") {
            ChatScreen(
                onSettings = { nav.navigate("settings") },
                onFileServer = { nav.navigate("servers") },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable("servers") {
            ServerListScreen(
                onBack = { nav.popBackStack() },
                onOpen = { server ->
                    nav.navigate("files/${Uri.encode(server.id)}/${Uri.encode(server.name)}")
                },
            )
        }
        composable("files/{server}/{name}") {
            FileManagerScreen(
                onBack = { nav.popBackStack() },
                onEdit = { serverId, path ->
                    nav.navigate("editor/${Uri.encode(serverId)}/${Uri.encode(path)}")
                },
            )
        }
        composable("editor/{server}/{path}") {
            FileEditorScreen(onBack = { nav.popBackStack() })
        }
    }
}
