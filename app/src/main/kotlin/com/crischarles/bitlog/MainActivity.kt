package com.crischarles.bitlog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            AppNavHost()
        }
    }

    override fun onResume() {
        super.onResume()
        // App is now open in foreground: hide floating notch overlay
        EggTimerManager.setAppForeground(this, true)
    }

    override fun onStop() {
        super.onStop()
        // App is now in background / not open: activate notch overlay if timer is active
        EggTimerManager.setAppForeground(this, false)
    }
}

@Composable
fun AppNavHost() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Timer) }

    when (currentScreen) {
        Screen.Timer -> EggTimerScreen(onNavigateToRecipes = { currentScreen = Screen.Recipes })
        Screen.Recipes -> EggRecipesScreen(onBack = { currentScreen = Screen.Timer })
    }
}

enum class Screen {
    Timer,
    Recipes,
}

@Composable
fun Greeting(name: String) {
    LiquidText(text = "Hello, $name!")
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Greeting("Bitlog")
}
