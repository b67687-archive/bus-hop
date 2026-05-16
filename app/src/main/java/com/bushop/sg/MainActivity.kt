package com.bushop.sg

import android.os.Bundle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bushop.sg.data.api.RetrofitBusArrivalDataSource
import com.bushop.sg.data.local.BusStopIndex
import com.bushop.sg.data.local.BusStopStorage
import com.bushop.sg.data.repository.BusRepositoryImpl
import com.bushop.sg.domain.model.ColorSchemeOption
import com.bushop.sg.domain.model.ThemeMode
import com.bushop.sg.ui.screens.MainScreen
import com.bushop.sg.ui.screens.MainViewModel
import com.bushop.sg.ui.theme.BusHopTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val storage = BusStopStorage(applicationContext)
        val dataSource = RetrofitBusArrivalDataSource()
        val busStopIndex = BusStopIndex(applicationContext).also { idx ->
            lifecycleScope.launch(Dispatchers.IO) { idx.load() }
        }
        val repository = BusRepositoryImpl(storage, dataSource, busStopIndex)
        val viewModelFactory = MainViewModel.Factory(application, repository, busStopIndex)

        setContent {
            val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
            val themeMode by viewModel.themeModeFlow.collectAsState()
            val colorSchemeOption by viewModel.colorSchemeOptionFlow.collectAsState()
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Pause auto-refresh when app goes to background
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> viewModel.resumeAutoRefresh()
                        Lifecycle.Event.ON_STOP -> viewModel.pauseAutoRefresh()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            BusHopTheme(darkTheme = isDarkTheme, colorSchemeOption = colorSchemeOption) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
