package com.crischarles.bitlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android draws the app under the status and navigation bars.
        // safeDrawingPadding keeps the content clear of the bars.
        enableEdgeToEdge()
        setContent {
            EggTimerScreen()
        }
    }
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
