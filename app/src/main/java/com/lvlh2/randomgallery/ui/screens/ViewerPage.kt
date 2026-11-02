package com.lvlh2.randomgallery.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lvlh2.randomgallery.model.ImageItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState

/**
 * One pager page: zoomable image plus the vertical swipe gestures.
 *
 * Gesture arbitration: telephoto only consumes drags when the content can actually
 * pan (scale > 1) — at scale 1 the vertical detector below receives the events.
 * When zoomed in, the detector cancels itself on drag start so the pan is never stolen.
 *
 * At scale 1 the image follows the finger (page-space translation applied outside
 * telephoto's own transform); releasing below the threshold springs it back, past
 * the threshold fires delete/exit.
 */
@Composable
fun ViewerPage(
    item: ImageItem,
    onTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
) {
    val zoomState = rememberZoomableImageState()
    val dragOffset = remember { Animatable(0f) } // translationY in px
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(zoomState) {
                var totalY = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        // Zoomed in → let telephoto pan; drop out of this detector.
                        if ((zoomState.zoomableState.zoomFraction ?: 0f) > 0f) throw CancellationException()
                        scope.launch { dragOffset.stop() } // cancel a spring-back in flight
                        totalY = dragOffset.value // continue from the current visual position
                    },
                    onVerticalDrag = { _, dragAmount ->
                        totalY += dragAmount
                        scope.launch { dragOffset.snapTo(totalY) } // follow the finger
                    },
                    onDragEnd = {
                        val threshold = 120.dp.toPx()
                        when {
                            totalY < -threshold -> {
                                scope.launch { dragOffset.snapTo(0f) }
                                onSwipeUp()
                            }
                            totalY > threshold -> {
                                scope.launch { dragOffset.snapTo(0f) }
                                onSwipeDown()
                            }
                            else ->
                                scope.launch {
                                    dragOffset.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }
                        }
                    },
                    onDragCancel = {
                        // Pointer lost mid-drag (system gesture etc.) — never leave the image displaced.
                        scope.launch { dragOffset.animateTo(0f, spring()) }
                    },
                )
            },
    ) {
        ZoomableAsyncImage(
            model = item.uri,
            contentDescription = item.name,
            state = zoomState,
            contentScale = ContentScale.Fit,
            onClick = { onTap() },
            modifier = Modifier.fillMaxSize().graphicsLayer { translationY = dragOffset.value },
        )
    }
}
