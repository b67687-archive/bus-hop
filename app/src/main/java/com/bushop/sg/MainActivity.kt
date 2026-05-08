package com.bushop.sg

import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bushop.sg.data.local.BusStopIndex
import com.bushop.sg.data.local.BusStopStorage
import com.bushop.sg.data.repository.BusRepository
import com.bushop.sg.ui.screens.MainScreen
import com.bushop.sg.ui.screens.MainViewModel
import com.bushop.sg.ui.theme.BusHopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val storage = BusStopStorage(applicationContext)
        val repository = BusRepository(storage)
        val busStopIndex = BusStopIndex(applicationContext)
        val viewModelFactory = MainViewModel.Factory(repository, busStopIndex)

        setContent {
            val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
            Crossfade(
                targetState = viewModel.isDarkMode,
                animationSpec = tween(durationMillis = 400)
            ) { isDark ->
                BusHopTheme(darkTheme = isDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}