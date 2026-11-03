package com.lvlh2.randomgallery.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.lvlh2.randomgallery.model.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImageDetails(
    val name: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val width: Int?,
    val height: Int?,
)

suspend fun loadImageDetails(
    context: Context,
    item: ImageItem,
): ImageDetails =
    withContext(Dispatchers.IO) {
        val uri = item.uri.toUri()
        val mimeType = context.contentResolver.getType(uri) ?: "Unknown"
        val sizeBytes =
            try {
                context.contentResolver
                    .query(
                        uri,
                        arrayOf(OpenableColumns.SIZE),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
                    }
            } catch (_: Exception) {
                null
            }

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (_: Exception) {
        }

        ImageDetails(
            name = item.name,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            width = options.outWidth.takeIf { it > 0 },
            height = options.outHeight.takeIf { it > 0 },
        )
    }
