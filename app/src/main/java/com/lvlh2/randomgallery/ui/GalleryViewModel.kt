package com.lvlh2.randomgallery.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lvlh2.randomgallery.data.FolderStore
import com.lvlh2.randomgallery.data.ImageRepository
import com.lvlh2.randomgallery.data.SharedPrefsFolderStore
import com.lvlh2.randomgallery.data.addFolder
import com.lvlh2.randomgallery.data.removeFolder
import com.lvlh2.randomgallery.data.toggleFolder
import com.lvlh2.randomgallery.model.ImageItem
import com.lvlh2.randomgallery.model.SavedFolder
import com.lvlh2.randomgallery.util.deriveFolderName
import com.lvlh2.randomgallery.util.fisherYatesShuffle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Single activity-scoped ViewModel holding all app state — the Compose-native
 * replacement for the old Expo app's three-layer cache (order survives tab
 * switches and viewer round-trips by construction).
 */
class GalleryViewModel(
    private val app: Application,
) : AndroidViewModel(app) {
    data class GalleryUiState(
        val folders: List<SavedFolder> = emptyList(),
        val images: List<ImageItem> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val message: String? = null,
        val screen: Screen = Screen.Tabs,
    )

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val folderStore: FolderStore = SharedPrefsFolderStore(app)
    private val imageRepository: ImageRepository = ImageRepository(app.contentResolver)

    init {
        _uiState.update { it.copy(folders = folderStore.load()) }
        reload()
    }

    /** Called on activity ON_RESUME: re-scan, but deferred while the viewer is open. */
    fun onAppResume() {
        if (_uiState.value.screen is Screen.Viewer) {
            pendingReloadAfterViewer = true
        } else {
            reload()
        }
    }

    private var pendingReloadAfterViewer = false

    fun importFolder(uri: String) {
        val folders = _uiState.value.folders
        if (folders.any { it.uri == uri }) {
            _uiState.update { it.copy(message = "Folder already imported") }
            return
        }
        val name = deriveFolderName(uri)
        // Persist the SAF grant so the folder survives restarts.
        try {
            app.contentResolver.takePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: Exception) {
            // Grant may already be held; keep importing.
        }
        persistAndSetFolders(addFolder(folders, uri, name) { UUID.randomUUID().toString() })
    }

    fun toggleFolder(id: String) = persistAndSetFolders(toggleFolder(_uiState.value.folders, id))

    fun removeFolder(id: String) = persistAndSetFolders(removeFolder(_uiState.value.folders, id))

    /** Re-shuffles the current list only — no re-scan (old app behavior). */
    fun reshuffle() {
        _uiState.update { it.copy(images = fisherYatesShuffle(it.images)) }
    }

    fun openViewer(index: Int) {
        if (index !in _uiState.value.images.indices) return
        _uiState.update { it.copy(screen = Screen.Viewer(index)) }
    }

    fun closeViewer() {
        _uiState.update { it.copy(screen = Screen.Tabs) }
        if (pendingReloadAfterViewer) {
            pendingReloadAfterViewer = false
            reload()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /** Permanently deletes the image file; removes it from the list only on success. */
    suspend fun deleteImage(item: ImageItem): Boolean {
        val deleted =
            try {
                withContext(Dispatchers.IO) {
                    DocumentsContract.deleteDocument(app.contentResolver, Uri.parse(item.uri))
                }
            } catch (_: Exception) {
                false
            }
        if (deleted) {
            _uiState.update { it.copy(images = it.images.filterNot { image -> image.uri == item.uri }) }
        }
        return deleted
    }

    private fun persistAndSetFolders(folders: List<SavedFolder>) {
        folderStore.save(folders)
        _uiState.update { it.copy(folders = folders) }
        reload()
    }

    private var reloadJob: Job? = null

    private fun reload() {
        reloadJob?.cancel()
        reloadJob =
            viewModelScope.launch {
                _uiState.update { it.copy(loading = true, error = null) }
                val enabled = _uiState.value.folders.filter { it.enabled }
                val images = imageRepository.enumerate(enabled)
                _uiState.update {
                    it.copy(loading = false, error = null, images = fisherYatesShuffle(images))
                }
            }
    }
}
