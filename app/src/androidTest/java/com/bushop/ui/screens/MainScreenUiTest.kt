package com.bushop.ui.screens

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bushop.domain.model.ColorSchemeOption
import com.bushop.domain.model.ThemeMode
import com.bushop.ui.theme.BusHopTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsSheetShowsThemeOptions() {
        composeTestRule.setContent {
            BusHopTheme {
                SettingsSheet(
                    currentTheme = ThemeMode.SYSTEM,
                    currentInterval = 30,
                    currentColorScheme = ColorSchemeOption.BLUE,
                    onThemeChange = {},
                    onColorSchemeChange = {},
                    onIntervalChange = {},
                    onCheckUpdate = {},
                    isCheckingUpdate = false,
                    isDownloadingUpdate = false,
                    updateInfo = null,
                    onDownloadUpdate = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onNodeWithText("System").assertExists()
        composeTestRule.onNodeWithText("Light").assertExists()
        composeTestRule.onNodeWithText("Dark").assertExists()
        composeTestRule.onNodeWithText("Colour Scheme").assertExists()
    }

    @Test
    fun settingsSheetShowsColourSchemeOptions() {
        composeTestRule.setContent {
            BusHopTheme {
                SettingsSheet(
                    currentTheme = ThemeMode.SYSTEM,
                    currentInterval = 30,
                    currentColorScheme = ColorSchemeOption.BLUE,
                    onThemeChange = {},
                    onColorSchemeChange = {},
                    onIntervalChange = {},
                    onCheckUpdate = {},
                    isCheckingUpdate = false,
                    isDownloadingUpdate = false,
                    updateInfo = null,
                    onDownloadUpdate = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Blue Classic").assertExists()
        composeTestRule.onNodeWithText("Contrast Blue").assertExists()
    }

    @Test
    fun settingsSheetShowsAutoRefreshOptions() {
        composeTestRule.setContent {
            BusHopTheme {
                SettingsSheet(
                    currentTheme = ThemeMode.SYSTEM,
                    currentInterval = 30,
                    currentColorScheme = ColorSchemeOption.BLUE,
                    onThemeChange = {},
                    onColorSchemeChange = {},
                    onIntervalChange = {},
                    onCheckUpdate = {},
                    isCheckingUpdate = false,
                    isDownloadingUpdate = false,
                    updateInfo = null,
                    onDownloadUpdate = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Auto Refresh").assertExists()
        composeTestRule.onNodeWithText("30s").assertExists()
        composeTestRule.onNodeWithText("1m").assertExists()
    }
}
