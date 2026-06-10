package com.bushop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bushop.data.api.RetrofitBusArrivalDataSource
import com.bushop.data.local.BusStopIndex
import com.bushop.data.local.BusStopStorage
import com.bushop.data.repository.BusRepositoryImpl
import com.bushop.domain.model.ColorSchemeOption
import com.bushop.domain.model.ThemeMode
import com.bushop.ui.screens.MainScreen
import com.bushop.ui.screens.MainViewModel
import com.bushop.ui.theme.BusHopTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (BuildConfig.DEBUG) {
            com.bushop.data.api.ApiClient
                .enableDebugLogging()
        }
        val storage = BusStopStorage(applicationContext)
        val dataSource = RetrofitBusArrivalDataSource()
        val busStopIndex =
            BusStopIndex(applicationContext).also { idx ->
                lifecycleScope.launch { idx.load() }
            }
        val repository = BusRepositoryImpl(storage, dataSource, busStopIndex)
        val viewModelFactory = MainViewModel.Factory(application, repository, busStopIndex)

        setContent {
            val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
            val themeMode by viewModel.themeModeFlow.collectAsState()
            val colorSchemeOption by viewModel.colorSchemeOptionFlow.collectAsState()
            val isDarkTheme =
                when (themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }

            // Pause auto-refresh when app goes to background
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer =
                    LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> {
                                viewModel.resumeAutoRefresh()
                            }

                            Lifecycle.Event.ON_STOP -> {
                                viewModel.pauseAutoRefresh()
                            }

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
