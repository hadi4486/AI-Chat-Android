package com.aichat.assistant.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aichat.assistant.AppContainer
import com.aichat.assistant.presentation.chat.ChatScreen
import com.aichat.assistant.presentation.chat.ChatViewModel
import com.aichat.assistant.presentation.history.HistoryScreen
import com.aichat.assistant.presentation.history.HistoryViewModel
import com.aichat.assistant.presentation.home.HomeScreen
import com.aichat.assistant.presentation.home.HomeViewModel
import com.aichat.assistant.presentation.settings.SettingsScreen
import com.aichat.assistant.presentation.settings.SettingsViewModel
import com.aichat.assistant.presentation.splash.SplashScreen
import com.aichat.assistant.presentation.theme.Motion
import java.util.UUID

private object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val CHAT = "chat/{conversationId}"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    fun chat(conversationId: String) = "chat/$conversationId"
}

@Composable
fun AppNavGraph(container: AppContainer) {
    val navController: NavHostController = rememberNavController()
    val slideSpec = Motion.standardTween<Float>()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = { fadeIn(tween(Motion.STANDARD_MS)) + slideInHorizontally(slideSpec) { it / 6 } },
        exitTransition = { fadeOut(tween(Motion.STANDARD_MS)) },
        popEnterTransition = { fadeIn(tween(Motion.STANDARD_MS)) },
        popExitTransition = { fadeOut(tween(Motion.STANDARD_MS)) + slideOutHorizontally(slideSpec) { it / 6 } }
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(factory = appViewModelFactory(container))
            HomeScreen(
                viewModel = viewModel,
                onNewChat = { navController.navigate(Routes.chat(UUID.randomUUID().toString())) },
                onOpenConversation = { id -> navController.navigate(Routes.chat(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) }
            )
        }

        composable(Routes.CHAT) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty()
            val viewModel: ChatViewModel = viewModel(factory = chatViewModelFactory(container, conversationId))
            ChatScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(factory = appViewModelFactory(container))
            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.HISTORY) {
            val viewModel: HistoryViewModel = viewModel(factory = appViewModelFactory(container))
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenConversation = { id -> navController.navigate(Routes.chat(id)) }
            )
        }
    }
}
