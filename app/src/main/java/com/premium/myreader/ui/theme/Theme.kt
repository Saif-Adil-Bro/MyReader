package com.premium.myreader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EmeraldGreen = Color(0xFF50C878)
val DarkEmerald = Color(0xFF2E8B57)
val PureWhite = Color(0xFFFFFFFF)
val LuxuryGold = Color(0xFFFFD700)
val DarkBackground = Color(0xFF121212)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldGreen,
    secondary = LuxuryGold,
    background = PureWhite,
    surface = PureWhite
)

@Composable
fun MyReaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}