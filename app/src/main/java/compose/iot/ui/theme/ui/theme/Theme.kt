package compose.iot.ui.theme.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val lightScheme =
    lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = backgroundLight,
        onBackground = onBackgroundLight,
        surface = surfaceLight,
        onSurface = onSurfaceLight,
        surfaceVariant = surfaceVariantLight,
        onSurfaceVariant = onSurfaceVariantLight,
        outline = outlineLight,
        outlineVariant = outlineVariantLight,
        scrim = scrimLight,
        inverseSurface = inverseSurfaceLight,
        inverseOnSurface = inverseOnSurfaceLight,
        inversePrimary = inversePrimaryLight,
        surfaceDim = surfaceDimLight,
        surfaceBright = surfaceBrightLight,
        surfaceContainerLowest = surfaceContainerLowestLight,
        surfaceContainerLow = surfaceContainerLowLight,
        surfaceContainer = surfaceContainerLight,
        surfaceContainerHigh = surfaceContainerHighLight,
        surfaceContainerHighest = surfaceContainerHighestLight,
    )

private val darkScheme =
    darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        scrim = scrimDark,
        inverseSurface = inverseSurfaceDark,
        inverseOnSurface = inverseOnSurfaceDark,
        inversePrimary = inversePrimaryDark,
        surfaceDim = surfaceDimDark,
        surfaceBright = surfaceBrightDark,
        surfaceContainerLowest = surfaceContainerLowestDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        surfaceContainerHighest = surfaceContainerHighestDark,
    )

@Composable
fun AIOT_ComposeTheme(
    darkTheme: Boolean =
        when (compose.iot.AppState.darkMode.intValue) {
            1 -> false
            2 -> true
            else -> isSystemInDarkTheme()
        },
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val themeColorId = compose.iot.AppState.themeColor.intValue

    val colorScheme =
        when {
            dynamicColor && themeColorId == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            themeColorId > 0 -> {
                // Find matching color preset or Default
                val preset = compose.iot.ui.theme.function.AppThemeColorsList.find { it.id == themeColorId }
                if (preset != null) {
                    if (darkTheme) {
                        darkScheme.copy(
                            primary = preset.primaryDark,
                            onPrimary = Color(0xFF003738),
                            primaryContainer = preset.primaryDark.copy(alpha = 0.3f),
                            onPrimaryContainer = preset.primaryDark,
                            secondary = preset.primaryDark.copy(alpha = 0.8f),
                            onSecondary = Color(0xFF003738),
                            secondaryContainer = preset.primaryDark.copy(alpha = 0.2f),
                            onSecondaryContainer = preset.primaryDark,
                            tertiary = preset.primaryDark.copy(alpha = 0.9f),
                            onTertiary = Color(0xFF003738),
                            tertiaryContainer = preset.primaryDark.copy(alpha = 0.25f),
                            onTertiaryContainer = preset.primaryDark,
                            surfaceTint = preset.primaryDark,
                        )
                    } else {
                        lightScheme.copy(
                            primary = preset.primaryLight,
                            onPrimary = Color.White,
                            primaryContainer = preset.primaryContainerLight,
                            onPrimaryContainer = preset.primaryLight,
                            secondary = preset.primaryLight.copy(alpha = 0.8f),
                            onSecondary = Color.White,
                            secondaryContainer = preset.primaryContainerLight,
                            onSecondaryContainer = preset.primaryLight,
                            tertiary = preset.primaryLight.copy(alpha = 0.9f),
                            onTertiary = Color.White,
                            tertiaryContainer = preset.tertiaryContainerLight,
                            onTertiaryContainer = preset.primaryLight,
                            surfaceTint = preset.primaryLight,
                        )
                    }
                } else {
                    if (darkTheme) darkScheme else lightScheme
                }
            }
            darkTheme -> darkScheme
            else -> lightScheme
        }

    // Dynamic Corner Radius Based on AppState
    val radius =
        when (compose.iot.AppState.cornerShapeLevel.intValue) {
            0 -> 8.dp
            1 -> 12.dp
            2 -> 16.dp
            else -> 8.dp
        }

    val customShapes =
        Shapes(
            extraSmall = RoundedCornerShape(radius / 2),
            small = RoundedCornerShape(radius),
            medium = RoundedCornerShape(radius),
            large = RoundedCornerShape(radius),
            extraLarge = RoundedCornerShape(radius * 1.5f),
        )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            // Set bars to be edge-to-edge transparent (Deprecated in API 35, but still needed for API < 35)
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            // Adjust icons based on dark/light theme
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = customShapes,
        content = content,
    )
}
