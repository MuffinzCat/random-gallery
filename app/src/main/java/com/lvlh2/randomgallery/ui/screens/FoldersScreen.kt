package com.lvlh2.randomgallery.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lvlh2.randomgallery.model.SavedFolder
import com.lvlh2.randomgallery.ui.GalleryViewModel

@Composable
fun FoldersScreen(
    viewModel: GalleryViewModel,
    state: GalleryViewModel.GalleryUiState,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var removeTarget by remember { mutableStateOf<SavedFolder?>(null) }

    Column(modifier.fillMaxSize()) {
        if (state.folders.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "No folders yet.\nImport a folder to start browsing.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(state.folders, key = { it.id }) { folder ->
                    FolderRow(
                        folder = folder,
                        onToggle = { viewModel.toggleFolder(folder.id) },
                        onRemove = { removeTarget = folder },
                    )
                }
            }
        }
        Text(
            "💡 In the full-screen viewer:\nswipe up to delete, swipe down to exit",
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onImportClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Import Folder")
        }
    }

    removeTarget?.let { folder ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("Remove folder?") },
            text = { Text("Stop importing \"${folder.name}\"? Source files will NOT be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFolder(folder.id)
                    removeTarget = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun FolderRow(
    folder: SavedFolder,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = folder.enabled, onCheckedChange = { onToggle() })
        Text(
            text = folder.name,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}
