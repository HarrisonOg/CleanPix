package com.harrisonog.cleanpix.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.harrisonog.cleanpix.ui.MainViewModel
import com.harrisonog.cleanpix.ui.screens.ImageEditorScreen
import com.harrisonog.cleanpix.ui.screens.MainScreen
import androidx.core.net.toUri

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object ImageEditor : Screen("imageEditor/{imageUri}") {
        fun createRoute(imageUri: Uri): String {
            val encodedUri = Uri.encode(imageUri.toString())
            return "imageEditor/$encodedUri"
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            val state by viewModel.state.collectAsState()

            MainScreen(
                state = state,
                onImageSelected = viewModel::selectImage,
                onStripMetadata = viewModel::stripMetadata,
                onSaveImage = { filename ->
                    viewModel.saveCleanedImage(filename)
                },
                onClearState = viewModel::clearState,
                onDismissError = viewModel::dismissError,
                onDismissSaved = viewModel::dismissSavedMessage,
                onEditImage = { uri ->
                    navController.navigate(Screen.ImageEditor.createRoute(uri))
                }
            )
        }

        composable(
            route = Screen.ImageEditor.route,
            arguments = listOf(
                navArgument("imageUri") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri")
            val imageUri = encodedUri?.let { Uri.decode(it).toUri() }

            ImageEditorScreen(
                imageUri = imageUri,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
