package com.example.sleepsaver.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LavenderAccent,
    secondary = IndigoPrimary,
    tertiary = LavenderAccent,
    background = NightBackground,
    surface = NightSurface,
    onBackground = OnDark,
    onSurface = OnDark
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    secondary = LavenderAccent,
    tertiary = IndigoDark,
    background = MorningBackground,
    surface = MorningSurface
)

@Composable
fun SleepSaverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SleepSaverShapes,
        content = content
    )
}