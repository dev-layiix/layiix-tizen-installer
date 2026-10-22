package com.layiix.tizenbridge.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text

/** Palette layiix.com: --bg #070B12, --panel #10131A, accento blu #4FB0F2, ciano #59E8FF. */
val LayiixPrimary = Color(0xFF4FB0F2)
val LayiixCyan = Color(0xFF59E8FF)
val LayiixBg = Color(0xFF070B12)
val LayiixSurface = Color(0xFF10131A)
val LayiixSurface2 = Color(0xFF161C26)
val LayiixLine = Color(0x29FFFFFF)
val LayiixMuted = Color(0xFF969DAA)
val LayiixError = Color(0xFFFF5C6C)
val LayiixOk = Color(0xFF3DDC84)
val LayiixWarn = Color(0xFFFFC14D)

/** Alias storici: viola/ciano neon del brand (icona, footer). */
val LayiixPurple = LayiixPrimary

private val LayiixColors = darkColorScheme(
    primary = LayiixPrimary,
    onPrimary = Color.White,
    secondary = LayiixCyan,
    onSecondary = Color(0xFF062A33),
    tertiary = LayiixCyan,
    background = LayiixBg,
    onBackground = Color(0xFFF4F4F6),
    surface = LayiixSurface,
    onSurface = Color(0xFFF4F4F6),
    surfaceVariant = LayiixSurface2,
    onSurfaceVariant = LayiixMuted,
    outline = LayiixLine,
    error = LayiixError,
    onError = Color.White,
)

private val LayiixShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
)

private val LayiixType = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.25).sp),
    titleSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, color = LayiixMuted),
    labelSmall = TextStyle(fontSize = 11.sp, color = LayiixMuted),
)

/** Kicker stile sito: mono maiuscolo spaziato, es. "01 // CONNESSIONE". */
@Composable
fun Kicker(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
            color = LayiixPrimary,
        ),
        modifier = androidx.compose.ui.Modifier.padding(top = 4.dp),
    )
}

@Composable
fun LayiixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LayiixColors,
        typography = LayiixType,
        shapes = LayiixShapes,
        content = content,
    )
}
