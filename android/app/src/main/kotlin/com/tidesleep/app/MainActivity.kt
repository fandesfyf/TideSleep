package com.tidesleep.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidesleep.app.ui.navigation.TideSleepNavHost
import com.tidesleep.app.ui.theme.TideSleepTheme
import com.tidesleep.app.viewmodel.TideSleepViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TideSleepTheme {
                val viewModel: TideSleepViewModel = viewModel()
                TideSleepNavHost(viewModel = viewModel)
            }
        }
    }
}
