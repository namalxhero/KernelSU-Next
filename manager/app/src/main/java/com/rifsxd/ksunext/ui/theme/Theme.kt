package com.rifsxd.ksunext.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

const val NOVA_ANIME_DARK = 0
const val NOVA_OCEAN_GLASS = 1
const val NOVA_MIDNIGHT_AMOLED = 2

fun Color.blend(other: Color, ratio: Float): Color {
    val inverse = 1f - ratio
    return Color(
        red = red * inverse + other.red * ratio,
        green = green * inverse + other.green * ratio,
        blue = blue * inverse + other.blue * ratio,
        alpha = alpha
    )
}

// "Liquid glass": surfaces are see-through so the live background shines through.
private fun novaScheme(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    background: Color,
    glass: Color,
): ColorScheme {
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF0B0716),
        primaryContainer = primary.blend(background, 0.70f),
        onPrimaryContainer = Color(0xFFF3EEFF),
        secondary = secondary,
        onSecondary = Color(0xFF0B0716),
        secondaryContainer = secondary.blend(background, 0.72f),
        onSecondaryContainer = Color(0xFFFFEEF6),
        tertiary = tertiary,
        onTertiary = Color(0xFF0B0716),
        tertiaryContainer = tertiary.blend(background, 0.75f),
        onTertiaryContainer = Color(0xFFE9FBFF),
        background = background,
        onBackground = Color(0xFFF1ECFF),
        surface = glass.copy(alpha = 0.30f),
        onSurface = Color(0xFFF1ECFF),
        surfaceVariant = glass.copy(alpha = 0.55f),
        onSurfaceVariant = Color(0xFFD3CBEA),
        surfaceContainerLowest = glass.copy(alpha = 0.25f),
        surfaceContainerLow = glass.copy(alpha = 0.40f),
        surfaceContainer = glass.copy(alpha = 0.55f),
        surfaceContainerHigh = glass.copy(alpha = 0.72f),
        surfaceContainerHighest = glass.copy(alpha = 0.85f),
        outline = Color.White.copy(alpha = 0.35f),
        outlineVariant = Color.White.copy(alpha = 0.15f),
    )
}

private val AnimeDarkScheme = novaScheme(
    primary = NOVA_PURPLE, secondary = NOVA_PINK, tertiary = NOVA_CYAN,
    background = Color(0xFF0B0716), glass = Color(0xFF241447),
)
private val OceanGlassScheme = novaScheme(
    primary = OCEAN_CYAN, secondary = OCEAN_BLUE, tertiary = OCEAN_ICE,
    background = Color(0xFF061018), glass = Color(0xFF0E3350),
)
private val MidnightAmoledScheme = novaScheme(
    primary = MIDNIGHT_GREEN, secondary = MIDNIGHT_MINT, tertiary = MIDNIGHT_LIME,
    background = Color(0xFF000000), glass = Color(0xFF0F1D16),
)

@Composable
fun KernelSUTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    amoledMode: Boolean = false,
    novaTheme: Int = NOVA_ANIME_DARK,
    liveBackground: Boolean = true,
    content: @Composable () -> Unit
) {
    // Nova Root themes are always dark. darkTheme / dynamicColor are kept only so old call sites still compile.
    var colorScheme = when (novaTheme) {
        NOVA_OCEAN_GLASS -> OceanGlassScheme
        NOVA_MIDNIGHT_AMOLED -> MidnightAmoledScheme
        else -> AnimeDarkScheme
    }
    if (amoledMode) {
        colorScheme = colorScheme.copy(background = AMOLED_BLACK)
    }

    SystemBarStyle(darkMode = true)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NovaBackground(novaTheme = novaTheme, live = liveBackground)
            content()
        }
    }
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
            navigationBarStyle = SystemBarStyle.dark(navigationBarScrim.toArgb())
        )
    }
}
