package com.suno.android.sunointerview.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.suno.android.sunointerview.ui.nav.MainNavigation
import com.suno.android.sunointerview.ui.theme.SunoInterviewTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SunoInterviewTheme {
                MainNavigation(intent = intent)
            }
        }
    }
}
