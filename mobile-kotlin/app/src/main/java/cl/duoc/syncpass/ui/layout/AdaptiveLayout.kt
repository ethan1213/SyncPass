package cl.duoc.syncpass.ui.layout

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Formato de pantalla con el que se decide el layout.
 *
 * El caso §3.3 exige "adaptabilidad a distintas pantallas". Estos tres formatos
 * siguen las clases de tamano de ventana de Material 3:
 *
 *  - [TELEFONO]  compact  (< 600 dp): una sola columna, operable con una mano.
 *  - [TABLET]    medium   (600-840 dp): dos columnas.
 *  - [ESCRITORIO] expanded (> 840 dp): tres columnas, tablet en horizontal o Chrome OS.
 */
enum class FormatoPantalla {
    TELEFONO,
    TABLET,
    ESCRITORIO;

    /** Columnas de la grilla de talleres para este formato. */
    val columnas: Int
        get() = when (this) {
            TELEFONO -> 1
            TABLET -> 2
            ESCRITORIO -> 3
        }

    /** Margen lateral del contenido. En tablet el texto no debe cruzar la pantalla. */
    val margenLateral: Dp
        get() = when (this) {
            TELEFONO -> 16.dp
            TABLET -> 24.dp
            ESCRITORIO -> 32.dp
        }

    /** Ancho maximo de una columna de lectura, para no tener lineas larguisimas. */
    val anchoMaximoContenido: Dp
        get() = when (this) {
            TELEFONO -> Dp.Unspecified
            TABLET -> 720.dp
            ESCRITORIO -> 1040.dp
        }

    val esCompacto: Boolean get() = this == TELEFONO
}

/** Area tactil minima exigida por el caso §5.1: 48x48 dp. */
val AREA_TACTIL_MINIMA: Dp = 48.dp

val LocalFormatoPantalla: ProvidableCompositionLocal<FormatoPantalla> =
    compositionLocalOf { FormatoPantalla.TELEFONO }

/** Traduce la clase de ancho de ventana de Material 3 a nuestro formato. */
fun WindowSizeClass.aFormatoPantalla(): FormatoPantalla = when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> FormatoPantalla.TELEFONO
    WindowWidthSizeClass.Medium -> FormatoPantalla.TABLET
    else -> FormatoPantalla.ESCRITORIO
}

/**
 * Calcula el formato de la ventana actual. Se recalcula solo al rotar el
 * dispositivo o al cambiar de tamano en multiventana.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun recordarFormatoPantalla(activity: androidx.activity.ComponentActivity): FormatoPantalla =
    calculateWindowSizeClass(activity).aFormatoPantalla()
