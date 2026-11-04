package com.alexray.cookr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alexray.cookr.ui.navigation.AppNavigation
import com.alexray.cookr.ui.theme.CookrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CookrTheme {
                AppNavigation()
            }
        }
    }
}
