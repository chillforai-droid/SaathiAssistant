package com.saathi.assistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    secondary = Color(0xFF00796B),
    tertiary = Color(0xFF5D4037)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFF3E2000),
    secondary = Color(0xFF4DB6AC),
    tertiary = Color(0xFFBCAAA4)
)

@Composable
fun SaathiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
