package uz.moliya.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Daromad / xarajat / ogohlantirish ranglari: yorug' va qorong'i rejimda kontrast saqlanishi uchun alohida. */
@Immutable
data class BrandColors(val income: Color, val expense: Color, val warning: Color)

private val LightBrand = BrandColors(
    income = Color(0xFF15803D),
    expense = Color(0xFFDC2626),
    warning = Color(0xFFB45309),
)

private val DarkBrand = BrandColors(
    income = Color(0xFF4ADE80),
    expense = Color(0xFFF87171),
    warning = Color(0xFFFBBF24),
)

val LocalBrandColors = staticCompositionLocalOf { LightBrand }

object Brand {
    val heroStart = Color(0xFF0F766E)
    val heroEnd = Color(0xFF115E59)

    val primary: Color @Composable get() = MaterialTheme.colorScheme.primary
    val income: Color @Composable get() = LocalBrandColors.current.income
    val expense: Color @Composable get() = LocalBrandColors.current.expense
    val warning: Color @Composable get() = LocalBrandColors.current.warning
    val ink: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val track: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant

    private val palette = listOf(
        Color(0xFF0F766E), Color(0xFF2563EB), Color(0xFFD97706), Color(0xFF7C3AED),
        Color(0xFFDB2777), Color(0xFF059669), Color(0xFF0891B2), Color(0xFF65A30D),
    )

    fun categoryColor(name: String): Color = palette[abs(name.hashCode()) % palette.size]
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCEFEA),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF334155),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiaryContainer = Color(0xFFFFF7ED),
    onTertiaryContainer = Color(0xFF7C2D12),
    background = Color(0xFFF5F7F8),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE9EEF0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF1F5F9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FD1C5),
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF0F4F4A),
    onPrimaryContainer = Color(0xFFCCEFEA),
    secondary = Color(0xFFB5C4C2),
    onSecondary = Color(0xFF0B1211),
    secondaryContainer = Color(0xFF2A3836),
    onSecondaryContainer = Color(0xFFE5ECEB),
    tertiaryContainer = Color(0xFF3B2A10),
    onTertiaryContainer = Color(0xFFFDE68A),
    background = Color(0xFF0B1211),
    onBackground = Color(0xFFE5ECEB),
    surface = Color(0xFF131C1B),
    onSurface = Color(0xFFE5ECEB),
    surfaceVariant = Color(0xFF22302E),
    onSurfaceVariant = Color(0xFFA7B6B4),
    outline = Color(0xFF3A4A48),
    outlineVariant = Color(0xFF2A3836),
    error = Color(0xFFF87171),
    onError = Color(0xFF2B0000),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    surfaceContainerLowest = Color(0xFF0B1211),
    surfaceContainerLow = Color(0xFF131C1B),
    surfaceContainer = Color(0xFF172221),
    surfaceContainerHigh = Color(0xFF1D2927),
    surfaceContainerHighest = Color(0xFF22302E),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MoliyaTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = AppShapes,
    ) {
        CompositionLocalProvider(
            LocalBrandColors provides (if (dark) DarkBrand else LightBrand),
            content = content,
        )
    }
}
