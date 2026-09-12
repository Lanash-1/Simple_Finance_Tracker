package com.codigitech.ft.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codigitech.ft.platform.ThemeMode

// ---------------------------------------------------------------------------------------------
// Material colour schemes. Pocketsum brand: charcoal surfaces with a single emerald accent.
// Brand tokens are documented in docs/branding/brand.md - keep the two in sync.
// ---------------------------------------------------------------------------------------------

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF022C22),
    secondary = Color(0xFF4B5D57),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E3DC),
    onSecondaryContainer = Color(0xFF0A1F18),
    tertiary = Color(0xFF3B5FA0),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9E4FF),
    onTertiaryContainer = Color(0xFF001B3E),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F8F7),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF7F8F7),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDDE3E0),
    onSurfaceVariant = Color(0xFF3F4944),
    outline = Color(0xFF6F7973),
    outlineVariant = Color(0xFFBFC9C2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F3F2),
    surfaceContainer = Color(0xFFEBEEEC),
    surfaceContainerHigh = Color(0xFFE6E9E7),
    surfaceContainerHighest = Color(0xFFE3E7E5),
    inverseSurface = Color(0xFF2A302D),
    inverseOnSurface = Color(0xFFEEF1EF),
    inversePrimary = Color(0xFF34D399),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFFB0C0BA),
    onSecondary = Color(0xFF1D2A25),
    secondaryContainer = Color(0xFF33413C),
    onSecondaryContainer = Color(0xFFD5E3DC),
    tertiary = Color(0xFFA3C1FF),
    onTertiary = Color(0xFF07305F),
    tertiaryContainer = Color(0xFF244677),
    onTertiaryContainer = Color(0xFFD9E4FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B0F0E),
    onBackground = Color(0xFFE6EAE7),
    surface = Color(0xFF0B0F0E),
    onSurface = Color(0xFFE6EAE7),
    surfaceVariant = Color(0xFF3B423F),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF89938D),
    outlineVariant = Color(0xFF3B423F),
    surfaceContainerLowest = Color(0xFF060908),
    surfaceContainerLow = Color(0xFF101514),
    surfaceContainer = Color(0xFF161B19),
    surfaceContainerHigh = Color(0xFF202624),
    surfaceContainerHighest = Color(0xFF2A302D),
    inverseSurface = Color(0xFFE6EAE7),
    inverseOnSurface = Color(0xFF2A302D),
    inversePrimary = Color(0xFF059669),
)

// ---------------------------------------------------------------------------------------------
// Finance-specific semantic colours. Provided through a composition local so they follow the
// *effective* theme (including the in-app Light/Dark override), not just the system setting.
// ---------------------------------------------------------------------------------------------

@Immutable
data class FinanceColors(
    val income: Color,
    val onIncomeContainer: Color,
    val incomeContainer: Color,
    val expense: Color,
    val onExpenseContainer: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val onTransferContainer: Color,
    val transferContainer: Color,
    /** Gradient stops for the hero balance card. */
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val onHeroMuted: Color,
)

private val LightFinanceColors = FinanceColors(
    income = Color(0xFF059669),
    onIncomeContainer = Color(0xFF022C22),
    incomeContainer = Color(0xFFD1FAE5),
    expense = Color(0xFFDC2626),
    onExpenseContainer = Color(0xFF450A0A),
    expenseContainer = Color(0xFFFEE2E2),
    transfer = Color(0xFF0284C7),
    onTransferContainer = Color(0xFF082F49),
    transferContainer = Color(0xFFE0F2FE),
    heroStart = Color(0xFF064E3B),
    heroEnd = Color(0xFF059669),
    onHero = Color.White,
    onHeroMuted = Color(0xFFA7F3D0),
)

private val DarkFinanceColors = FinanceColors(
    income = Color(0xFF34D399),
    onIncomeContainer = Color(0xFFD1FAE5),
    incomeContainer = Color(0xFF065F46),
    expense = Color(0xFFF87171),
    onExpenseContainer = Color(0xFFFEE2E2),
    expenseContainer = Color(0xFF7F1D1D),
    transfer = Color(0xFF38BDF8),
    onTransferContainer = Color(0xFFE0F2FE),
    transferContainer = Color(0xFF0C4A6E),
    heroStart = Color(0xFF111614),
    heroEnd = Color(0xFF064E3B),
    onHero = Color(0xFFECFDF5),
    onHeroMuted = Color(0xFFA7F3D0),
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

/** `MaterialTheme.finance.income` etc. */
val MaterialTheme.finance: FinanceColors
    @Composable @ReadOnlyComposable get() = LocalFinanceColors.current

@Composable @ReadOnlyComposable fun incomeColor(): Color = LocalFinanceColors.current.income
@Composable @ReadOnlyComposable fun expenseColor(): Color = LocalFinanceColors.current.expense
@Composable @ReadOnlyComposable fun transferColor(): Color = LocalFinanceColors.current.transfer

// ---------------------------------------------------------------------------------------------
// Typography & shapes
// ---------------------------------------------------------------------------------------------

private val baseTypography = Typography()

/** Numbers line up in columns when every digit has the same advance width. */
val TextStyle.tabular: TextStyle
    get() = copy(fontFeatureSettings = "tnum")

private val FinanceTypography = baseTypography.copy(
    displayMedium = baseTypography.displayMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineLarge = baseTypography.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
    headlineMedium = baseTypography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = baseTypography.labelLarge.copy(fontWeight = FontWeight.Medium),
)

private val FinanceShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun FinanceTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalFinanceColors provides if (dark) DarkFinanceColors else LightFinanceColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = FinanceTypography,
            shapes = FinanceShapes,
            content = content,
        )
    }
}

fun parseHexColor(hex: String, fallback: Color = Color.Gray): Color {
    val clean = hex.removePrefix("#")
    if (clean.length != 6) return fallback
    val value = clean.toLongOrNull(16) ?: return fallback
    return Color(0xFF000000L or value)
}
