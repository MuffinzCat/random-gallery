package com.lvlh2.randomgallery

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lvlh2.randomgallery.theme.RandomGalleryTheme
import com.lvlh2.randomgallery.ui.AppTab
import com.lvlh2.randomgallery.ui.GalleryViewModel
import com.lvlh2.randomgallery.ui.Screen
import com.lvlh2.randomgallery.ui.screens.FoldersScreen
import com.lvlh2.randomgallery.ui.screens.RandomScreen
import com.lvlh2.randomgallery.ui.screens.ViewerScreen
import kotlinx.coroutines.launch

@Composable
fun RandomGalleryApp(viewModel: GalleryViewModel = viewModel()) {
    RandomGalleryTheme {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val gridState = rememberLazyGridState()

        LaunchedEffect(state.screen, state.returnToImageIndex) {
            val returnIndex = state.returnToImageIndex
            if (state.screen is Screen.Tabs && returnIndex != null) {
                if (returnIndex in state.images.indices) {
                    gridState.scrollToItem(returnIndex)
                }
                viewModel.consumeReturnToImage()
            }
        }
        BackHandler(enabled = state.screen is Screen.Viewer) { viewModel.closeViewer() }

        // Re-scan on return to the app (new photos appear); deferred while the viewer is open.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer =
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) viewModel.onAppResume()
                }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        // Request the media permissions needed for MediaStore enumeration at launch.
        val permissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
                viewModel.onMediaPermissionResult(grants.values.any { it })
            }
        LaunchedEffect(Unit) {
            if (!viewModel.hasMediaPermission()) {
                permissionLauncher.launch(GalleryViewModel.mediaPermissions)
            }
        }

        when (val screen = state.screen) {
            is Screen.Tabs ->
                TabScaffold(viewModel, state, gridState, onRetryPermission = {
                    permissionLauncher.launch(GalleryViewModel.mediaPermissions)
                })
            is Screen.Viewer -> ViewerScreen(viewModel, screen.index)
        }
    }
}

@Composable
private fun TabScaffold(
    viewModel: GalleryViewModel,
    state: GalleryViewModel.GalleryUiState,
    gridState: LazyGridState,
    onRetryPermission: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(AppTab.RANDOM) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) viewModel.importFolder(uri.toString())
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == AppTab.RANDOM,
                    onClick = { tab = AppTab.RANDOM },
                    icon = { Icon(painterResource(R.drawable.ic_shuffle), contentDescription = null) },
                    label = { Text("Random") },
                )
                NavigationBarItem(
                    selected = tab == AppTab.FOLDERS,
                    onClick = { tab = AppTab.FOLDERS },
                    icon = { Icon(painterResource(R.drawable.ic_folder), contentDescription = null) },
                    label = { Text("Folders") },
                )
            }
        },
        floatingActionButton = {
            if (tab == AppTab.RANDOM) {
                FloatingActionButton(
                    onClick = {
                        viewModel.reshuffle()
                        scope.launch { gridState.animateScrollToItem(0) }
                    },
                ) {
                    Icon(painterResource(R.drawable.ic_shuffle), contentDescription = "Reshuffle")
                }
            }
        },
    ) { padding ->
        when (tab) {
            AppTab.RANDOM ->
                RandomScreen(
                    viewModel,
                    state,
                    gridState,
                    onRetryPermission = onRetryPermission,
                    modifier = Modifier.padding(padding),
                )

            AppTab.FOLDERS ->
                FoldersScreen(
                    viewModel,
                    state,
                    onImportClick = { folderPicker.launch(null) },
                    modifier = Modifier.padding(padding),
                )
        }
    }
}
