package com.lvlh2.randomgallery.data

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.lvlh2.randomgallery.model.ImageItem
import com.lvlh2.randomgallery.model.SavedFolder
import com.lvlh2.randomgallery.util.getTreeDocumentId
import com.lvlh2.randomgallery.util.isImageFile
import com.lvlh2.randomgallery.util.primaryDocId
import com.lvlh2.randomgallery.util.primaryVolumeRelativePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One full scan: the found images plus folders that could not be mapped to MediaStore. */
data class ScanResult(
    val images: List<ImageItem>,
    val unsupportedFolders: List<String>,
)

/**
 * Enumerates images through the MediaStore index (a millisecond-level SQL query
 * instead of a per-file SAF directory walk) and builds SAF tree URIs for each
 * file, so display (Coil) and delete (DocumentsContract) keep working on the
 * imported tree's read+write grant. Never copies files.
 *
 * Only folders on the internal "primary" volume are supported — SD-card
 * volumes and third-party providers have no MediaStore RELATIVE_PATH mapping
 * and are reported via [ScanResult.unsupportedFolders].
 */
class ImageRepository(
    private val contentResolver: ContentResolver,
) {
    suspend fun enumerate(folders: List<SavedFolder>): ScanResult =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<ImageItem>()
            val seen = mutableSetOf<String>()
            val unsupported = mutableListOf<String>()
            for (folder in folders.filter { it.enabled }) {
                val treeUri = Uri.parse(folder.uri)
                val docId = getTreeDocumentId(folder.uri)
                if (!isMappable(treeUri, docId)) {
                    unsupported += folder.name
                    continue
                }
                try {
                    queryViaMediaStore(treeUri, docId, folder.id, items, seen)
                } catch (_: Exception) {
                    // skip folder, keep it listed — old app behavior
                }
            }
            ScanResult(images = items, unsupportedFolders = unsupported)
        }

    /** The internal ExternalStorageProvider exposes "primary:<path>" docIds — the only shape we can map. */
    fun isMappable(uriString: String): Boolean = isMappable(Uri.parse(uriString), getTreeDocumentId(uriString))

    private fun isMappable(
        treeUri: Uri,
        docId: String,
    ): Boolean =
        treeUri.authority == EXTERNAL_STORAGE_PROVIDER_AUTHORITY &&
            primaryVolumeRelativePath(docId) != null

    private fun queryViaMediaStore(
        treeUri: Uri,
        docId: String,
        sourceFolderId: String,
        out: MutableList<ImageItem>,
        seen: MutableSet<String>,
    ) {
        val relativePath = primaryVolumeRelativePath(docId) ?: return
        // RELATIVE_PATH exists since API 29; older devices only have the DATA column.
        val selection: String
        val selectionArgs: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ?"
            selectionArgs = arrayOf(relativePath)
        } else {
            selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            selectionArgs = arrayOf("/storage/emulated/0/$relativePath%")
        }
        contentResolver
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                selection,
                selectionArgs,
                "${MediaStore.Images.Media.DISPLAY_NAME} ASC",
            )?.use { cursor ->
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    if (!isImageFile(name)) continue
                    val uri = buildDocumentUri(treeUri, primaryDocId(relativePath, name))
                    if (seen.add(uri)) out += ImageItem(uri = uri, name = name, sourceFolderId = sourceFolderId)
                }
            }
    }

    /**
     * Document uri via the framework helper. NOTE: some vendor ROMs (e.g. vivo)
     * implement buildDocumentUriUsingTree by appending to the TREE uri, yielding
     * content://.../tree/<treeId>/document/<docId> instead of the AOSP standard
     * content://.../document/<docId>. On those ROMs the tree-shaped uri is what
     * the DocumentsProvider actually serves — the standard shape fails to load
     * images there — so keep the framework helper and don't "normalize".
     */
    private fun buildDocumentUri(
        treeUri: Uri,
        docId: String,
    ): String = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()

    private companion object {
        /** The system's internal-storage DocumentsProvider (stable across ROMs). */
        const val EXTERNAL_STORAGE_PROVIDER_AUTHORITY = "com.android.externalstorage.documents"
    }
}
