package cl.duoc.syncpass.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Esquema claro. Uso en terreno con luz solar directa: contraste alto sobre fondo
 * blanco. Caso §3.3 (buena legibilidad bajo condiciones de luz variables).
 */
private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = BlueDark,
    secondary = CyanAccent,
    onSecondary = BlueDark,
    secondaryContainer = BlueLight,
    onSecondaryContainer = BlueDark,
    background = Color.White,
    onBackground = GrayText,
    surface = Color.White,
    onSurface = GrayText,
    surfaceVariant = GrayBackground,
    onSurfaceVariant = GrayMuted,
    outline = BorderLight,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorOnContainerLight
)

/**
 * Esquema oscuro. Uso en terreno de noche o en interiores sin iluminacion:
 * reduce el deslumbramiento y el consumo de bateria en pantallas OLED.
 */
private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = NavyDeep,
    primaryContainer = NavySurfaceHigh,
    onPrimaryContainer = BlueLightDark,
    secondary = CyanAccent,
    onSecondary = NavyDeep,
    secondaryContainer = NavySurfaceHigh,
    onSecondaryContainer = BlueLightDark,
    background = NavyDeep,
    onBackground = GrayTextDark,
    surface = NavySurface,
    onSurface = GrayTextDark,
    surfaceVariant = NavySurfaceHigh,
    onSurfaceVariant = GrayMutedDark,
    outline = BorderDark,
    error = ErrorRedDark,
    onError = NavyDeep,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorRedDark
)

/**
 * Tema de SyncPass. Sigue el ajuste del sistema y permite forzar el modo con
 * `darkTheme` para demostrar las dos variantes durante la presentacion.
 */
@Composable
fun SyncPassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
