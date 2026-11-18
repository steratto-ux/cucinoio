package com.iocucino.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iocucino.app.data.Course

/** Colori dell'app: tela beige, verde bosco per i titoli, arancione per le azioni. */
object Pal {
    val Green = Color(0xFF2F4A32)
    val GreenMid = Color(0xFF4F7D55)
    val GreenLight = Color(0xFF6FA06F)
    val Orange = Color(0xFFE5822C)
    val OrangeLight = Color(0xFFF4A455)
    val OrangeDark = Color(0xFFD26C16)
    val Cream = Color(0xFFFAF5E8)
    val CreamHi = Color(0xFFFFFCF4)
    val CreamLow = Color(0xFFEDE5D2)
    val Sage = Color(0xFFE4E9D8)
    val LinenTop = Color(0xFFE2DBCA)
    val Linen = Color(0xFFCFC7B4)
    val Border = Color(0xFFE9A964)
    val Line = Color(0xFFD9CFB8)
    val Ink = Color(0xFF2A2822)
    val InkSoft = Color(0xFF6C6858)
    val Ochre = Color(0xFFC49A3C)
    val Mint = Color(0xFFD5EADB)
    val MintText = Color(0xFF2E6A4B)
    val Sand = Color(0xFFF0DDB0)
    val SandText = Color(0xFF7A5A12)
    val Terra = Color(0xFFF3CFC1)
    val TerraText = Color(0xFF8A3418)
}

private val LightColors = lightColorScheme(
    primary = Pal.Orange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBDDBE),
    onPrimaryContainer = Color(0xFF4A2300),
    secondary = Pal.GreenMid,
    onSecondary = Color.White,
    secondaryContainer = Pal.Sage,
    onSecondaryContainer = Pal.Green,
    tertiary = Pal.Ochre,
    onTertiary = Color.White,
    tertiaryContainer = Pal.Sand,
    onTertiaryContainer = Pal.SandText,
    background = Pal.Linen,
    onBackground = Pal.Ink,
    surface = Pal.Cream,
    onSurface = Pal.Ink,
    surfaceVariant = Pal.CreamLow,
    onSurfaceVariant = Pal.InkSoft,
    outline = Color(0xFF8C8672),
    outlineVariant = Pal.Line,
    surfaceContainerLowest = Pal.CreamHi,
    surfaceContainerLow = Pal.Cream,
    surfaceContainer = Pal.Cream,
    surfaceContainerHigh = Pal.CreamLow,
    surfaceContainerHighest = Color(0xFFE6DDC8)
)

private val Serif = FontFamily.Serif

private val AppTypography: Typography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
        titleSmall = t.titleSmall.copy(fontFamily = Serif, fontWeight = FontWeight.Bold)
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun IoCucinoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

/** Colori della testata delle card, uno per tipo di piatto. */
fun courseColors(course: Course): Pair<Color, Color> = when (course) {
    Course.PRIMO -> Color(0xFFF6B04A) to Color(0xFFE8742A)
    Course.SECONDO -> Color(0xFFE8664A) to Color(0xFFB8341C)
    Course.UNICO -> Color(0xFF9BBB55) to Color(0xFF5F8A2E)
    Course.CONTORNO -> Color(0xFF5CC095) to Color(0xFF2E8B63)
}
