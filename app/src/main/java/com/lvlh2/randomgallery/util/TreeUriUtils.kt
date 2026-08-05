package com.lvlh2.randomgallery.util

import java.net.URLDecoder

// Parsing helpers for SAF tree URIs like
// "content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FCamera".
// Pure string code (no android.net.Uri) so the logic is unit-testable.

/** Decoded document id of a tree URI, e.g. "primary:DCIM/Camera". */
fun getTreeDocumentId(uriString: String): String = URLDecoder.decode(uriString.substringAfterLast('/'), Charsets.UTF_8)

/**
 * Display name for a tree URI: the last path segment after the volume part,
 * e.g. "primary:DCIM/Camera" → "Camera". Mirrors the old Expo app's extraction.
 * Falls back to "Folder" when there is no path segment (volume root).
 */
fun deriveFolderName(uriString: String): String {
    val decoded = getTreeDocumentId(uriString)
    val path = decoded.substringAfter(':') // drop the volume ("primary" / SD-card id)
    val name = path.substringAfterLast('/')
    return name.ifBlank { "Folder" }
}
