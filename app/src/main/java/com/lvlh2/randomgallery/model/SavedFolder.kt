package com.lvlh2.randomgallery.model

import kotlinx.serialization.Serializable

/** A folder the user imported via SAF, persisted across restarts. */
@Serializable
data class SavedFolder(
    val id: String,
    val uri: String,
    val name: String,
    val enabled: Boolean = true,
)
