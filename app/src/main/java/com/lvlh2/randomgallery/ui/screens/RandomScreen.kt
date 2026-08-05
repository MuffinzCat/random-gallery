package com.lvlh2.randomgallery.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lvlh2.randomgallery.ui.GalleryViewModel

@Composable
fun RandomScreen(
    viewModel: GalleryViewModel,
    state: GalleryViewModel.GalleryUiState,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    when {
        state.loading && state.images.isEmpty() ->
            Box(
                modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

        state.error != null ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error, textAlign = TextAlign.Center)
            }

        state.images.isEmpty() ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No images to show\nImport a folder in the Folders tab to start browsing.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

        else ->
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(state.images, key = { _, item -> item.uri }) { index, item ->
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable { viewModel.openViewer(index) },
                    )
                }
            }
    }
}
