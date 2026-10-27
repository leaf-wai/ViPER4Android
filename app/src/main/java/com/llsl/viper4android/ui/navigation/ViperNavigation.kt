package com.llsl.viper4android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.llsl.viper4android.ui.screens.debug.DebugLogScreen
import com.llsl.viper4android.ui.screens.eq.EqEditScreen
import com.llsl.viper4android.ui.screens.main.MainScreen
import com.llsl.viper4android.ui.screens.main.MainViewModel
import com.llsl.viper4android.ui.screens.settings.ExcludedAppsScreen

@Composable
fun ViperNavigation(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                viewModel = viewModel,
                onOpenEqScreen = { navController.navigate("eq_edit") },
                onOpenExcludedApps = { navController.navigate("excluded_apps") },
                onOpenDebugLog = { navController.navigate("debug_log") }
            )
        }
        composable("eq_edit") {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            EqEditScreen(
                bands = state.eq.bands.map { it.toFloat() },
                bandCount = state.eq.bandCount,
                presetId = state.eq.presetId,
                presets = state.eq.presets,
                onBandCountChange = viewModel::setEqBandCount,
                onBandsChange = viewModel::setEqBands,
                onPresetSelect = viewModel::setEqPreset,
                onPresetAdd = viewModel::addEqPreset,
                onPresetDelete = viewModel::deleteEqPreset,
                onReset = viewModel::resetEqBands,
                onBack = { navController.popBackStack() }
            )
        }
        composable("excluded_apps") {
            val excludedApps by viewModel.excludedApps.collectAsStateWithLifecycle()
            ExcludedAppsScreen(
                excludedApps = excludedApps,
                onToggle = viewModel::setAppExcluded,
                loadInstalledApps = viewModel::loadInstalledApps,
                onBack = { navController.popBackStack() }
            )
        }
        composable("debug_log") {
            DebugLogScreen(
                onDisableDebug = {
                    viewModel.disableDebugMode()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
