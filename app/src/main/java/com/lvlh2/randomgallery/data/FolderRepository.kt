package com.lvlh2.randomgallery.data

import com.lvlh2.randomgallery.model.SavedFolder

/** Pure list helpers — mirrors the old Expo app's folder CRUD; unit-tested. */

fun addFolder(
    list: List<SavedFolder>,
    uri: String,
    name: String,
    idGen: () -> String,
): List<SavedFolder> = list + SavedFolder(id = idGen(), uri = uri, name = name)

fun toggleFolder(
    list: List<SavedFolder>,
    id: String,
): List<SavedFolder> = list.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }

fun removeFolder(
    list: List<SavedFolder>,
    id: String,
): List<SavedFolder> = list.filterNot { it.id == id }
