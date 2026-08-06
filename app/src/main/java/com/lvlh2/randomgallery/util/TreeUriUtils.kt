package com.lvlh2.randomgallery.util

import java.net.URLDecoder

// Parsing helpers for SAF tree URIs like
// "content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FCamera".
// Pure string code (no android.net.Uri) so the logic is unit-testable.

/**
 * Decoded document id of a tree URI, e.g. "primary:DCIM/Camera".
 *
 * Uses the (String, String) overload of URLDecoder: the Charset overload only
 * exists since Java 10 (Android API 33), so it would crash with NoSuchMethodError
 * on API 24–32 devices.
 */
@Suppress("DEPRECATION")
fun getTreeDocumentId(uriString: String): String = URLDecoder.decode(uriString.substringAfterLast('/'), "UTF-8")

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

/**
 * MediaStore RELATIVE_PATH for a tree document id on the internal "primary"
 * volume, e.g. "primary:DCIM/Camera" → "DCIM/Camera/" (MediaStore paths carry
 * a trailing slash). Returns null for anything else — SD-card volumes, other
 * volumes, or the volume root itself — which cannot be mapped to MediaStore.
 */
fun primaryVolumeRelativePath(docId: String): String? {
    val prefix = "primary:"
    if (!docId.startsWith(prefix)) return null
    val path = docId.removePrefix(prefix)
    if (path.isBlank()) return null
    return path.trimEnd('/') + "/"
}

/**
 * Rebuilds the tree document id for one file from its MediaStore path and
 * display name: "primary:" + RELATIVE_PATH + name. Inverse of
 * [primaryVolumeRelativePath].
 */
fun primaryDocId(
    relativePath: String,
    displayName: String,
): String = "primary:" + relativePath.trimEnd('/') + "/" + displayName
