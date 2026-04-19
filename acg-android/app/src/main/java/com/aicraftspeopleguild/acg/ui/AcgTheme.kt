package com.aicraftspeopleguild.acg.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AcgDark = darkColorScheme(
    primary        = Color(0xFF3FB950),
    onPrimary      = Color(0xFF0D1117),
    secondary      = Color(0xFFA371F7),
    background     = Color(0xFF0D1117),
    onBackground   = Color(0xFFE6EDF3),
    surface        = Color(0xFF161B22),
    onSurface      = Color(0xFFE6EDF3),
    error          = Color(0xFFF85149),
)

@Composable
fun AcgTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AcgDark, content = content)
}
