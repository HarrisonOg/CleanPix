package com.harrisonog.cleanpix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.harrisonog.cleanpix.ui.MainViewModel
import com.harrisonog.cleanpix.ui.screens.MainScreen
import com.harrisonog.cleanpix.ui.theme.CleanPixTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel with context
        viewModel.initialize(applicationContext)

        setContent {
            CleanPixTheme() {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.state.collectAsState()

                    MainScreen(
                        state = state,
                        onImageSelected = viewModel::selectImage,
                        onStripMetadata = viewModel::stripMetadata,
                        onSaveImage = viewModel::saveCleanedImage,
                        onClearState = viewModel::clearState,
                        onDismissError = viewModel::dismissError,
                        onDismissSaved = viewModel::dismissSavedMessage
                    )
                }
            }
        }
    }
}