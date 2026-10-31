package com.rifsxd.ksunext.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PRIMARY,
    secondary = PRIMARY_DARK,
    tertiary = SECONDARY_DARK
)

private val LightColorScheme = lightColorScheme(
    primary = PRIMARY,
    secondary = PRIMARY_LIGHT,
    tertiary = SECONDARY_LIGHT
)

fun Color.blend(other: Color, ratio: Float): Color {
    val inverse = 1f - ratio
    return Color(
        red = red * inverse + other.red * ratio,
        green = green * inverse + other.green * ratio,
        blue = blue * inverse + other.blue * ratio,
        alpha = alpha
    )
}

private val RootXColorScheme = darkColorScheme(
    primary = Color(0xFF4FD0FF), onPrimary = Color(0xFF00121C),
    primaryContainer = Color(0xFF123A5C), onPrimaryContainer = Color(0xFFCDF1FF),
    secondary = Color(0xFF8FE3FF), onSecondary = Color(0xFF00121C),
    secondaryContainer = Color(0xFF1C3A63), onSecondaryContainer = Color(0xFFD9F4FF),
    tertiary = Color(0xFFC3A4FF), onTertiary = Color(0xFF1B0F33),
    tertiaryContainer = Color(0xFF38285F), onTertiaryContainer = Color(0xFFEADFFF),
    background = Color.Transparent, onBackground = Color(0xFFEEF3FF),
    surface = Color(0xFF0E1538), onSurface = Color(0xFFEEF3FF),
    surfaceVariant = Color(0xFF1B2452), onSurfaceVariant = Color(0xFFA9B6DD),
    surfaceContainerLowest = Color(0xFF0C1232), surfaceContainerLow = Color(0xFF151D47),
    surfaceContainer = Color(0xFF1A2352), surfaceContainerHigh = Color(0xFF202A5E),
    surfaceContainerHighest = Color(0x2EFFFFFF),
    outline = Color(0xFF4A5A94), outlineVariant = Color(0xFF2A3566),
    error = Color(0xFFFF7B8A), errorContainer = Color(0xFF4A1A2C), onErrorContainer = Color(0xFFFFC2CA)
)

@Composable
fun KernelSUTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    amoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = RootXColorScheme

    SystemBarStyle(
        darkMode = true
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
private fun SystemBarStyle(
    darkMode: Boolean,
    statusBarScrim: Color = Color.Transparent,
    navigationBarScrim: Color = Color.Transparent,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                statusBarScrim.toArgb(),
                statusBarScrim.toArgb(),
            ) { darkMode },
            navigationBarStyle = when {
                darkMode -> SystemBarStyle.dark(
                    navigationBarScrim.toArgb()
                )

                else -> SystemBarStyle.light(
                    navigationBarScrim.toArgb(),
                    navigationBarScrim.toArgb(),
                )
            }
        )
    }
}