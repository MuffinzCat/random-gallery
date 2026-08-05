package com.lvlh2.randomgallery.ui

/** App-level navigation state — hand-rolled: two tabs plus one full-screen viewer. */
sealed interface Screen {
    data object Tabs : Screen

    data class Viewer(
        val index: Int,
    ) : Screen
}

enum class AppTab { RANDOM, FOLDERS }
