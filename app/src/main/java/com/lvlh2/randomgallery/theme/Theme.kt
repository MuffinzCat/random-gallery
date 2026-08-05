package com.lvlh2.randomgallery.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// App is always dark (README: "Always dark"); pure black background matches the
// black windowBackground in themes.xml (no white startup flash).
private val DarkColors =
    darkColorScheme(
        background = Color.Black,
        surface = Color.Black,
    )

@Composable
fun RandomGalleryTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
