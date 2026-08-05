package com.lvlh2.randomgallery.data

import android.content.Context
import com.lvlh2.randomgallery.model.SavedFolder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface FolderStore {
    fun load(): List<SavedFolder>

    fun save(folders: List<SavedFolder>)
}

/** SharedPreferences-backed store; the whole folder list lives in one JSON string. */
class SharedPrefsFolderStore(
    context: Context,
) : FolderStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): List<SavedFolder> {
        val raw = prefs.getString(KEY_FOLDERS, null) ?: return emptyList()
        return FolderCodec.decode(raw)
    }

    override fun save(folders: List<SavedFolder>) {
        prefs.edit().putString(KEY_FOLDERS, FolderCodec.encode(folders)).apply()
    }

    private companion object {
        const val PREFS_NAME = "random_gallery"
        const val KEY_FOLDERS = "folders.json"
    }
}

/** Pure codec, no Android deps — unit-testable. Corrupt data decodes to an empty list. */
object FolderCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    fun encode(folders: List<SavedFolder>): String = json.encodeToString(folders)

    fun decode(raw: String): List<SavedFolder> =
        try {
            json.decodeFromString<List<SavedFolder>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
}
