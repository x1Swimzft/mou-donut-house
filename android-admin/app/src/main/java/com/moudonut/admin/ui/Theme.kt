package com.moudonut.admin.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = lightColorScheme(
    primary = Color(0xFF8E1B3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7E7DF),
    onPrimaryContainer = Color(0xFF2A1712),
    secondary = Color(0xFF5C443C),
    onSecondary = Color.White,
    background = Color(0xFFFFF6F2),
    onBackground = Color(0xFF2A1712),
    surface = Color.White,
    onSurface = Color(0xFF2A1712),
    surfaceVariant = Color(0xFFF7E7DF),
    onSurfaceVariant = Color(0xFF5C443C),
    error = Color(0xFF701818),
    onError = Color.White,
    errorContainer = Color(0xFFFBE9E7),
    onErrorContainer = Color(0xFF701818)
)

@Composable
fun MouDonutTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
