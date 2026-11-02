package com.lvlh2.randomgallery.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lvlh2.randomgallery.R
import com.lvlh2.randomgallery.model.ImageItem
import com.lvlh2.randomgallery.ui.GalleryViewModel
import com.lvlh2.randomgallery.ui.nextIndexAfterDelete
import kotlinx.coroutines.launch

@Composable
fun ViewerScreen(
    viewModel: GalleryViewModel,
    initialIndex: Int,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val images = state.images
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showConfirm by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(false) }

    val pagerState =
        rememberPagerState(
            initialPage = initialIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0)),
        ) { images.size }

    var pendingTrashItem by remember { mutableStateOf<ImageItem?>(null) }
    var pendingTrashIndex by remember { mutableStateOf(-1) }
    var pendingTrashSize by remember { mutableStateOf(0) }

    val trashLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val item = pendingTrashItem
            val index = pendingTrashIndex
            val size = pendingTrashSize

            pendingTrashItem = null
            pendingTrashIndex = -1
            pendingTrashSize = 0

            if (result.resultCode == Activity.RESULT_OK && item != null) {
                scope.launch {
                    val next = nextIndexAfterDelete(index, size)
                    if (index == size - 1 && size > 1) {
                        pagerState.animateScrollToPage(next)
                    }
                    viewModel.removeTrashedImage(item)
                    if (size == 1) {
                        viewModel.closeViewer()
                    }
                }
            }
        }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.updateViewerPosition(page)
        }
    }

    fun openCurrentImage() {
        val item = images.getOrNull(pagerState.currentPage) ?: return
        val uri = Uri.parse(item.uri)
        val mimeType = context.contentResolver.getType(uri) ?: "image/*"
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        try {
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (_: ActivityNotFoundException) {
            scope.launch {
                snackbarHostState.showSnackbar("No app was found that can open this image")
            }
        }
    }

    fun requestTrash() {
        val index = pagerState.currentPage
        val item = images.getOrNull(index) ?: return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            scope.launch {
                snackbarHostState.showSnackbar("Moving images to trash requires Android 11 or newer")
            }
            return
        }

        val mediaUri =
            try {
                MediaStore.getMediaUri(context, Uri.parse(item.uri))?.let { fileUri ->
                    val mediaId = fileUri.lastPathSegment?.toLongOrNull()
                    mediaId?.let {
                        MediaStore.Images.Media.getContentUri(MediaStore.getVolumeName(fileUri), it)
                    }
                }
            } catch (_: Exception) {
                null
            }

        if (mediaUri == null) {
            scope.launch {
                snackbarHostState.showSnackbar("This image could not be matched to the Android media trash")
            }
            return
        }

        pendingTrashItem = item
        pendingTrashIndex = index
        pendingTrashSize = images.size

        try {
            val request = MediaStore.createTrashRequest(context.contentResolver, listOf(mediaUri), true)
            trashLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } catch (_: Exception) {
            pendingTrashItem = null
            pendingTrashIndex = -1
            pendingTrashSize = 0
            scope.launch {
                snackbarHostState.showSnackbar("Could not open the Android trash request")
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = images.getOrNull(page) ?: return@HorizontalPager
            ViewerPage(
                item = item,
                onTap = { controlsVisible = !controlsVisible },
                onSwipeUp = { showConfirm = true },
                onSwipeDown = { viewModel.closeViewer() },
            )
        }
        if (controlsVisible) {
            Text(
                text = images.getOrNull(pagerState.currentPage)?.name.orEmpty(),
                color = Color.White,
                modifier =
                    Modifier
                        .align(
                            Alignment.TopCenter,
                        ).fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.65f),
                        ).statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
            )
            TextButton(
                onClick = { openCurrentImage() },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_open_with),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("Open with…", color = Color.White)
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }

    if (showConfirm) {
        DeleteConfirmDialog(
            onConfirm = {
                showConfirm = false
                requestTrash()
            },
            onDismiss = { showConfirm = false },
        )
    }
}
