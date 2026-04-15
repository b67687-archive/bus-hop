package com.bushop.sg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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
        val viewModelFactory = MainViewModel.Factory(repository)

        setContent {
            BusHopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}