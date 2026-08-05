package com.lvlh2.randomgallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lvlh2.randomgallery.ui.GalleryViewModel
import com.lvlh2.randomgallery.ui.nextIndexAfterDelete
import kotlinx.coroutines.launch

@Composable
fun ViewerScreen(
    viewModel: GalleryViewModel,
    initialIndex: Int,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val images = state.images
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showConfirm by remember { mutableStateOf(false) }

    val pagerState =
        rememberPagerState(
            initialPage = initialIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0)),
        ) { images.size }

    fun confirmDelete() {
        val idx = pagerState.currentPage
        if (idx !in images.indices) return
        val next = nextIndexAfterDelete(idx, images.size)
        scope.launch {
            // ★ 4d3c1c8 fix: scroll BEFORE the list shrinks so the pager never points
            //   past the end (that is what froze the old RN gallery).
            if (idx == images.lastIndex && images.size > 1) {
                pagerState.animateScrollToPage(next)
            }
            val deleted = viewModel.deleteImage(images[idx])
            if (!deleted) {
                snackbarHostState.showSnackbar("Could not delete image")
            } else if (images.size == 1) {
                viewModel.closeViewer() // deleted the only image → grid shows its empty state
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
                onSwipeUp = { showConfirm = true },
                onSwipeDown = { viewModel.closeViewer() },
            )
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
                confirmDelete()
            },
            onDismiss = { showConfirm = false },
        )
    }
}
