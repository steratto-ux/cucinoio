package com.iocucino.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iocucino.app.data.Difficulty
import com.iocucino.app.ui.theme.Pal

/** Sfondo a tela: beige con una trama finissima di fili. */
@Composable
fun LinenBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Pal.LinenTop, Pal.Linen)))
            .drawBehind {
                val step = 3.dp.toPx()
                val thread = Color(0x0E6B5F47)
                var y = 0f
                while (y < size.height) {
                    drawLine(thread, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
                var x = 0f
                while (x < size.width) {
                    drawLine(thread, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
            },
        content = content
    )
}

/** Card crema con ombra e doppio bordo arancione sottile. */
@Composable
fun CreamCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, shape)
            .clip(shape)
            .background(Pal.Cream)
            .then(click)
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Pal.Border.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            content = content
        )
    }
}

/** Riga verde salvia con bordo arancione, per gli elenchi. */
@Composable
fun SageRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Pal.Sage,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color)
            .border(1.dp, Pal.Border.copy(alpha = 0.7f), shape)
            .then(click)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

enum class BtnStyle { Orange, Green, Cream }

/** Bottone "in rilievo" a pillola: arancione, verde o crema. */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BtnStyle = BtnStyle.Orange,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    val shape = RoundedCornerShape(50)
    val colors = when (style) {
        BtnStyle.Orange -> listOf(Pal.OrangeLight, Pal.OrangeDark)
        BtnStyle.Green -> listOf(Pal.GreenLight, Pal.GreenMid)
        BtnStyle.Cream -> listOf(Color(0xFFFFFDF6), Color(0xFFE9E0CB))
    }
    val fg = if (style == BtnStyle.Cream) Pal.Ink else Color.White
    val edge = if (style == BtnStyle.Cream) Modifier.border(1.dp, Color(0x26000000), shape) else Modifier
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .shadow(if (enabled) 5.dp else 0.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(colors))
            .then(edge)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 16.dp else 20.dp, vertical = if (compact) 8.dp else 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Bottone tondo con icona. */
@Composable
fun CircleButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    accent: Boolean = false
) {
    val colors = if (accent) listOf(Pal.OrangeLight, Pal.OrangeDark)
    else listOf(Color(0xFFFFFDF6), Color(0xFFE9E0CB))
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.verticalGradient(colors))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (accent) Color.White else Pal.Ink,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/** Pallino con un numero (scelta del numero di persone). */
@Composable
fun NumberCircle(n: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = if (selected) listOf(Pal.OrangeLight, Pal.OrangeDark)
    else listOf(Color(0xFFFFFDF6), Color(0xFFE9E0CB))
    Box(
        modifier = Modifier
            .size(38.dp)
            .shadow(if (selected) 8.dp else 3.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.verticalGradient(colors))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$n",
            color = if (selected) Color.White else Pal.Ink,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Barra con il tasto "Indietro" per le schermate di dettaglio. */
@Composable
fun BackBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        AppButton("Indietro", onBack, icon = Icons.AutoMirrored.Filled.ArrowBack, compact = true)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = Pal.InkSoft,
        modifier = modifier
    )
}

@Composable
fun Tag(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text.uppercase(),
            color = content,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

@Composable
fun DifficultyTag(d: Difficulty, modifier: Modifier = Modifier) {
    when (d) {
        Difficulty.SEMPLICE -> Tag("Facile", Pal.Mint, Pal.MintText, modifier)
        Difficulty.MEDIA -> Tag("Media", Pal.Sand, Pal.SandText, modifier)
        Difficulty.IMPEGNATIVA -> Tag("Impegnativa", Pal.Terra, Pal.TerraText, modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        leadingIcon = if (icon != null) {
            { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else {
            null
        },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Pal.CreamHi,
            labelColor = Pal.Ink,
            iconColor = Pal.InkSoft,
            selectedContainerColor = Pal.Orange,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White
        ),
        border = BorderStroke(1.dp, if (selected) Pal.OrangeDark else Pal.Line)
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pal.Orange,
    unfocusedBorderColor = Pal.Line,
    focusedContainerColor = Pal.CreamHi,
    unfocusedContainerColor = Pal.CreamHi,
    cursorColor = Pal.Orange,
    focusedLabelColor = Pal.OrangeDark
)

@Composable
fun FullWidth(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), content = content)
}
