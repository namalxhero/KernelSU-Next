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
    primary = Color(0xFF3CC8FF), onPrimary = Color(0xFF001018),
    primaryContainer = Color(0xFF0B2A36), onPrimaryContainer = Color(0xFFBFF0FF),
    secondary = Color(0xFF6FDCFF), onSecondary = Color(0xFF001018),
    secondaryContainer = Color(0xFF12303C), onSecondaryContainer = Color(0xFFD5F4FF),
    tertiary = Color(0xFFC3A4FF), onTertiary = Color(0xFF1B0F33),
    tertiaryContainer = Color(0xFF2A1F45), onTertiaryContainer = Color(0xFFE8DDFF),
    background = Color(0xFF000000), onBackground = Color(0xFFEEF6F8),
    surface = Color(0xFF000000), onSurface = Color(0xFFEEF6F8),
    surfaceVariant = Color(0xFF111A1D), onSurfaceVariant = Color(0xFF9FB1B8),
    surfaceContainerLowest = Color(0xFF000000), surfaceContainerLow = Color(0xFF070B0C),
    surfaceContainer = Color(0xFF0C1214), surfaceContainerHigh = Color(0xFF121A1D),
    surfaceContainerHighest = Color(0xFF182226),
    outline = Color(0xFF3A4A50), outlineVariant = Color(0xFF1E2A2F),
    error = Color(0xFFFF6B6B), errorContainer = Color(0xFF3A1416), onErrorContainer = Color(0xFFFFB4B4)
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