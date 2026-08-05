package com.lvlh2.randomgallery.ui.screens

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lvlh2.randomgallery.model.ImageItem
import kotlinx.coroutines.CancellationException
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState

/**
 * One pager page: zoomable image plus the vertical swipe gestures.
 *
 * Gesture arbitration: telephoto only consumes drags when the content can actually
 * pan (scale > 1) — at scale 1 the vertical detector below receives the events.
 * When zoomed in, the detector cancels itself on drag start so the pan is never stolen.
 */
@Composable
fun ViewerPage(
    item: ImageItem,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
) {
    val zoomState = rememberZoomableImageState()

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(zoomState) {
                var totalY = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        // Zoomed in → let telephoto pan; drop out of this detector.
                        if ((zoomState.zoomableState.zoomFraction ?: 0f) > 0f) throw CancellationException()
                        totalY = 0f
                    },
                    onVerticalDrag = { _, dragAmount -> totalY += dragAmount },
                    onDragEnd = {
                        val threshold = 120.dp.toPx()
                        when {
                            totalY < -threshold -> onSwipeUp()
                            totalY > threshold -> onSwipeDown()
                        }
                    },
                )
            },
    ) {
        ZoomableAsyncImage(
            model = item.uri,
            contentDescription = item.name,
            state = zoomState,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
