package com.lvlh2.randomgallery.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import com.lvlh2.randomgallery.util.applyScanResult
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
        val returnToImageIndex: Int? = null,
    )

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val folderStore: FolderStore = SharedPrefsFolderStore(app)
    private val imageRepository: ImageRepository = ImageRepository(app.contentResolver)

    init {
        _uiState.update { it.copy(folders = folderStore.load()) }
        if (hasMediaPermission()) {
            reload()
        } else {
            _uiState.update { it.copy(loading = false, error = MEDIA_PERMISSION_MESSAGE) }
        }
    }

    /** Called on activity ON_RESUME: re-scan, but deferred while the viewer is open. */
    fun onAppResume() {
        if (_uiState.value.screen is Screen.Viewer) {
            pendingReloadAfterViewer = true
        } else if (reloadJob?.isActive == true) {
            // A scan is already in flight (e.g. the init scan right after cold
            // start) — it covers this resume, don't start a duplicate.
        } else {
            reload()
        }
    }

    private var pendingReloadAfterViewer = false
    private var currentViewerImageUri: String? = null
    private var currentViewerIndex: Int = 0

    /** Result of the runtime media-permission request (one dialog per install). */
    fun onMediaPermissionResult(granted: Boolean) {
        if (granted) {
            reload()
        } else {
            _uiState.update { it.copy(loading = false, error = MEDIA_PERMISSION_MESSAGE) }
        }
    }

    fun importFolder(uri: String) {
        if (!imageRepository.isMappable(uri)) {
            _uiState.update { it.copy(message = UNSUPPORTED_FOLDER_MESSAGE) }
            return
        }
        val folders = _uiState.value.folders
        if (folders.any { it.uri == uri }) {
            // The persisted grant may have been lost (app data restored onto a new
            // device/ROM, or the user revoked access). The user just re-picked this
            // folder, so re-taking the permission repairs it instead of blocking.
            takePersistableUriPermission(uri)
            _uiState.update { it.copy(message = "Folder already imported — access restored") }
            return
        }
        val name = deriveFolderName(uri)
        takePersistableUriPermission(uri)
        persistAndSetFolders(addFolder(folders, uri, name) { UUID.randomUUID().toString() })
    }

    /** Persists the SAF grant so the folder survives restarts. No-op if already held. */
    private fun takePersistableUriPermission(uri: String) {
        try {
            app.contentResolver.takePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: Exception) {
            // Grant may already be held; keep importing.
        }
    }

    fun toggleFolder(id: String) = persistAndSetFolders(toggleFolder(_uiState.value.folders, id))

    fun removeFolder(id: String) = persistAndSetFolders(removeFolder(_uiState.value.folders, id))

    /** Re-shuffles the current list only — no re-scan (old app behavior). */
    fun reshuffle() {
        _uiState.update { it.copy(images = fisherYatesShuffle(it.images)) }
    }

    fun openViewer(index: Int) {
        val item = _uiState.value.images.getOrNull(index) ?: return
        currentViewerImageUri = item.uri
        currentViewerIndex = index
        _uiState.update { it.copy(screen = Screen.Viewer(index), returnToImageIndex = null) }
    }

    fun updateViewerPosition(index: Int) {
        val item = _uiState.value.images.getOrNull(index) ?: return
        currentViewerImageUri = item.uri
        currentViewerIndex = index
    }

    fun closeViewer() {
        val images = _uiState.value.images
        val returnIndex = returnIndexForImage(currentViewerImageUri, currentViewerIndex, images.map { it.uri })
        _uiState.update { it.copy(screen = Screen.Tabs, returnToImageIndex = returnIndex) }
        currentViewerImageUri = null
        if (pendingReloadAfterViewer) {
            pendingReloadAfterViewer = false
            reload()
        }
    }

    fun consumeReturnToImage() {
        _uiState.update { it.copy(returnToImageIndex = null) }
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
            _uiState.update { state ->
                val remainingImages = state.images.filterNot { image -> image.uri == item.uri }
                if (currentViewerImageUri == item.uri) {
                    val survivingIndex = returnIndexForImage(null, currentViewerIndex, remainingImages.map { it.uri })
                    currentViewerIndex = survivingIndex ?: 0
                    currentViewerImageUri = survivingIndex?.let { remainingImages[it].uri }
                }
                state.copy(images = remainingImages)
            }
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
                val result = imageRepository.enumerate(enabled)
                val images = applyScanResult(_uiState.value.images, result.images)
                val unsupportedMessage =
                    result.unsupportedFolders
                        .takeIf { it.isNotEmpty() && it.toSet() != lastUnsupportedNames }
                        ?.let { UNSUPPORTED_FOLDER_MESSAGE }
                lastUnsupportedNames = result.unsupportedFolders.toSet()
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = null,
                        images = images,
                        message = it.message ?: unsupportedMessage,
                    )
                }
            }
    }

    /** Only surface the unsupported-folder message when the affected set changes. */
    private var lastUnsupportedNames: Set<String> = emptySet()

    fun hasMediaPermission(): Boolean = app.checkSelfPermission(mediaPermission) == PackageManager.PERMISSION_GRANTED

    companion object {
        /** Message shown when the runtime media permission is missing (or refused). */
        const val MEDIA_PERMISSION_MESSAGE = "Media access permission is needed to browse images."

        /** Message for folders that can't be mapped to the MediaStore index. */
        const val UNSUPPORTED_FOLDER_MESSAGE =
            "Unsupported folder — only folders inside internal storage are supported."

        /** READ_MEDIA_IMAGES on Android 13+, the legacy storage permission below. */
        val mediaPermission: String
            get() =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
    }
}
