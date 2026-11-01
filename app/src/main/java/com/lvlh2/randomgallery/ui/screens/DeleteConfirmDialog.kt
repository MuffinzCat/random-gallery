package com.lvlh2.randomgallery.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move image to trash?") },
        text = { Text("The image will be moved to the Android media trash and can be restored later.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Move to trash") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
