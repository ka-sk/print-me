package com.printme.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.printme.ui.screens.PhotoSelectionScreen
import com.printme.ui.screens.PreviewScreen
import com.printme.viewmodel.MainViewModel

/**
 * Navigation routes
 */
object Routes {
    const val PHOTO_SELECTION = "photo_selection"
    const val PREVIEW = "preview"
}

/**
 * Main navigation host for the app
 */
@Composable
fun PrintMeNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.PHOTO_SELECTION
    ) {
        composable(Routes.PHOTO_SELECTION) {
            PhotoSelectionScreen(
                viewModel = viewModel,
                onNavigateToPreview = {
                    navController.navigate(Routes.PREVIEW)
                }
            )
        }

        composable(Routes.PREVIEW) {
            PreviewScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
