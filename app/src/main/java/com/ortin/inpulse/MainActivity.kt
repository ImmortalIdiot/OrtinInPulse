package com.ortin.inpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.ortin.inpulse.ui.screen.GraphicsScreen
import com.ortin.inpulse.ui.screen.HeartScreen
import com.ortin.inpulse.ui.screen.HistoryScreen
import com.ortin.inpulse.ui.theme.OrtinInPulseTheme

class MainActivity : ComponentActivity() {
    private lateinit var navigationVM: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationVM = ViewModelProvider(
            owner = this,
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            OrtinInPulseTheme {
                val currentScreen = navigationVM.currentScreen.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen.value) {
                        MainViewModel.Screen.MAIN -> HeartScreen(navigationVM)
                        MainViewModel.Screen.HISTORY -> HistoryScreen(navigationVM)
                        MainViewModel.Screen.GRAPHICS -> GraphicsScreen(navigationVM)
                    }
                }
            }
        }
    }
}
