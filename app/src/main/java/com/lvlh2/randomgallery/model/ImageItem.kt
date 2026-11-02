package com.lvlh2.randomgallery.model

/** One image found inside an imported SAF folder. */
data class ImageItem(
    val uri: String,
    val name: String,
    val sourceFolderId: String? = null,
)
