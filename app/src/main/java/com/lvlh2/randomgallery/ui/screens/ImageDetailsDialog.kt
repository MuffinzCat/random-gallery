package com.lvlh2.randomgallery.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun ImageDetailsDialog(
    details: ImageDetails,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Details") },
        text = {
            Column {
                Text("Name:")
                SelectionContainer {
                    Text(details.name)
                }
                Spacer(Modifier.height(12.dp))
                DetailRow("Type", details.mimeType)
                Spacer(Modifier.height(12.dp))
                DetailRow(
                    "Dimensions",
                    if (details.width != null && details.height != null) "${details.width} × ${details.height} px" else "Unknown",
                )
                Spacer(Modifier.height(12.dp))
                DetailRow("Size", formatFileSize(details.sizeBytes))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Text("$label: $value")
}

private fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes < 0) return "Unknown"
    if (bytes < 1024) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024) return String.format(Locale.US, "%.1f KB", kib)
    val mib = kib / 1024.0
    if (mib < 1024) return String.format(Locale.US, "%.1f MB", mib)
    return String.format(Locale.US, "%.1f GB", mib / 1024.0)
}
