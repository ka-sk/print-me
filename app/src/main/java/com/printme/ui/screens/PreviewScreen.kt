package com.printme.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.printme.ui.components.LayoutSelector
import com.printme.ui.components.MarginConfigurator
import com.printme.ui.components.PageIndicator
import com.printme.ui.components.PagePreview
import com.printme.ui.components.PaperSizeSelector
import com.printme.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * Preview screen showing page layouts before printing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val pages by viewModel.pages.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val layoutType by viewModel.layoutType.collectAsState()
    val paperSize by viewModel.paperSize.collectAsState()
    val marginConfig by viewModel.marginConfig.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    // Page navigation
                    IconButton(
                        onClick = { viewModel.previousPage() },
                        enabled = currentPageIndex > 0
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous page")
                    }

                    PageIndicator(
                        currentPage = currentPageIndex + 1,
                        totalPages = pages.size
                    )

                    IconButton(
                        onClick = { viewModel.nextPage() },
                        enabled = currentPageIndex < pages.size - 1
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next page")
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { viewModel.print() }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (pages.isEmpty()) {
                Text(
                    text = "No pages to preview",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Current page preview
                    val currentPage = pages.getOrNull(currentPageIndex)
                    if (currentPage != null) {
                        PagePreview(
                            page = currentPage,
                            paperSize = paperSize,
                            marginConfig = marginConfig,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Layout info
                    Text(
                        text = "Layout: ${layoutType.displayName} | Paper: ${paperSize.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Settings bottom sheet
        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = sheetState
            ) {
                SettingsContent(
                    layoutType = layoutType,
                    onLayoutSelected = { viewModel.setLayoutType(it) },
                    paperSize = paperSize,
                    onPaperSizeSelected = { viewModel.setPaperSize(it) },
                    marginConfig = marginConfig,
                    onMarginChanged = { viewModel.setMarginConfig(it) },
                    onDismiss = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showSettings = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun SettingsContent(
    layoutType: com.printme.model.LayoutType,
    onLayoutSelected: (com.printme.model.LayoutType) -> Unit,
    paperSize: com.printme.model.PaperSize,
    onPaperSizeSelected: (com.printme.model.PaperSize) -> Unit,
    marginConfig: com.printme.model.MarginConfig,
    onMarginChanged: (com.printme.model.MarginConfig) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Print Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            LayoutSelector(
                selectedLayout = layoutType,
                onLayoutSelected = onLayoutSelected,
                modifier = Modifier.weight(1f)
            )

            PaperSizeSelector(
                selectedSize = paperSize,
                onSizeSelected = onPaperSizeSelected,
                modifier = Modifier.weight(1f)
            )
        }

        MarginConfigurator(
            marginConfig = marginConfig,
            onMarginChanged = onMarginChanged
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
