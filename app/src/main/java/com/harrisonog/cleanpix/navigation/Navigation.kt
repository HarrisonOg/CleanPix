package com.harrisonog.cleanpix.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.harrisonog.cleanpix.ui.MainViewModel
import com.harrisonog.cleanpix.ui.screens.ImageSelectionScreen
import com.harrisonog.cleanpix.ui.screens.ImageMetadataScreen
import com.harrisonog.cleanpix.ui.screens.PrivacyPolicyScreen

sealed class Screen(val route: String) {
    object ImageSelection : Screen("imageSelection")
    object ImageMetadata : Screen("imageMetadata")
    object PrivacyPolicy : Screen("privacyPolicy")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ImageSelection.route
    ) {
        composable(Screen.ImageSelection.route) {
            val state by viewModel.state.collectAsState()

            ImageSelectionScreen(
                showOnboarding = state.showOnboarding,
                onDismissOnboarding = viewModel::dismissOnboarding,
                onImageSelected = { uri ->
                    viewModel.selectImage(uri)
                    navController.navigate(Screen.ImageMetadata.route)
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
            )
        }

        composable(Screen.ImageMetadata.route) {
            val state by viewModel.state.collectAsState()

            ImageMetadataScreen(
                state = state,
                onStripMetadata = viewModel::stripMetadata,
                onSaveImage = viewModel::saveCleanedImage,
                onCancel = {
                    viewModel.clearState()
                    navController.popBackStack()
                },
                onStartOver = {
                    viewModel.clearState()
                    navController.popBackStack()
                },
                onDismissError = viewModel::dismissError,
                onDismissSaved = viewModel::dismissSavedMessage,
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
            )
        }

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
