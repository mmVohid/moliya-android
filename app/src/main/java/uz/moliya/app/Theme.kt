package uz.moliya.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

object Brand {
    val primary = Color(0xFF0F766E)
    val primaryDark = Color(0xFF115E59)
    val income = Color(0xFF15803D)
    val expense = Color(0xFFDC2626)
    val warning = Color(0xFFB45309)
    val ink = Color(0xFF0F172A)
    val muted = Color(0xFF475569)
    val track = Color(0xFFE2E8F0)

    private val palette = listOf(
        Color(0xFF0F766E), Color(0xFF2563EB), Color(0xFFD97706), Color(0xFF7C3AED),
        Color(0xFFDB2777), Color(0xFF059669), Color(0xFF0891B2), Color(0xFF65A30D),
    )

    fun categoryColor(name: String): Color = palette[abs(name.hashCode()) % palette.size]
}

private val LightColors = lightColorScheme(
    primary = Brand.primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCEFEA),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF334155),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Brand.ink,
    background = Color(0xFFF5F7F8),
    onBackground = Brand.ink,
    surface = Color.White,
    onSurface = Brand.ink,
    surfaceVariant = Color(0xFFE9EEF0),
    onSurfaceVariant = Brand.muted,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Brand.expense,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF1F5F9),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MoliyaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = AppShapes,
        content = content,
    )
}
