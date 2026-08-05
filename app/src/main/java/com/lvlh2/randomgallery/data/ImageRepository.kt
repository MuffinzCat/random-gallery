package com.lvlh2.randomgallery.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.lvlh2.randomgallery.model.ImageItem
import com.lvlh2.randomgallery.model.SavedFolder
import com.lvlh2.randomgallery.util.getTreeDocumentId
import com.lvlh2.randomgallery.util.isImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Enumerates images in imported SAF trees, in place. Never copies files. */
class ImageRepository(
    private val contentResolver: ContentResolver,
) {
    /**
     * Returns every image in the enabled folders, in folder order, deduplicated by URI.
     * A folder that can no longer be read (revoked grant, provider error) is skipped
     * but stays listed — old app behavior.
     */
    suspend fun enumerate(folders: List<SavedFolder>): List<ImageItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<ImageItem>()
            val seen = mutableSetOf<String>()
            for (folder in folders.filter { it.enabled }) {
                try {
                    enumerateFolder(folder.uri, items, seen)
                } catch (_: Exception) {
                    // skip folder, keep it listed
                }
            }
            items
        }

    private fun enumerateFolder(
        uriString: String,
        out: MutableList<ImageItem>,
        seen: MutableSet<String>,
    ) {
        val treeUri = Uri.parse(uriString)
        val childUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                getTreeDocumentId(uriString),
            )
        contentResolver
            .query(
                childUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                ),
                null,
                null,
                "${DocumentsContract.Document.COLUMN_DISPLAY_NAME} ASC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    if (!isImageFile(name)) continue
                    val docId = cursor.getString(idCol)
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
                    if (seen.add(uri)) out += ImageItem(uri = uri, name = name)
                }
            }
    }
}
