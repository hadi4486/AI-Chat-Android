package com.aichat.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichat.assistant.domain.model.ChatPreferences
import com.aichat.assistant.presentation.navigation.AppNavGraph
import com.aichat.assistant.presentation.theme.AiChatTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate()/setContent — this is what shows Theme.AiChat.Splash
        // for the brief window before our first Compose frame draws (see AndroidManifest + themes.xml).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as AiChatApplication).container

        setContent {
            val preferences by container.settingsRepository.chatPreferences
                .collectAsStateWithLifecycle(initialValue = ChatPreferences())

            AiChatTheme(
                themeMode = preferences.themeMode,
                dynamicColorEnabled = preferences.dynamicColorEnabled
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(container = container)
                }
            }
        }
    }
}
