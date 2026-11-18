@file:OptIn(ExperimentalMaterial3Api::class)

package com.iocucino.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iocucino.app.data.Place
import com.iocucino.app.data.Recipe
import com.iocucino.app.ui.theme.Pal
import com.iocucino.app.ui.theme.courseColors
import java.io.File

/** Per le schermate che gestiscono da sole gli spazi di barra di stato e di navigazione. */
val NoInsets = WindowInsets(0, 0, 0, 0)

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 0.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Pal.Green
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Pal.InkSoft
                )
            }
        }
        actions()
    }
}

/** Foto da caricare: indirizzo web oppure file salvato nel telefono. */
fun imageModel(url: String): Any = if (url.startsWith("http")) url else File(url)

@Composable
fun RecipeThumb(recipe: Recipe, modifier: Modifier = Modifier, emojiSize: TextUnit = 44.sp) {
    val (c1, c2) = courseColors(recipe.course)
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(c1, c2))),
        contentAlignment = Alignment.Center
    ) {
        Text(text = recipe.emoji, fontSize = emojiSize)
        if (recipe.imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageModel(recipe.imageUrl),
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = Pal.CreamLow,
    content: Color = Pal.InkSoft,
    icon: ImageVector? = null
) {
    Surface(color = container, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
        }
    }
}

@Composable
fun RecipeCard(
    recipe: Recipe,
    favorite: Boolean,
    coverage: Pair<Int, Int>?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Pal.Cream),
        border = BorderStroke(1.dp, Pal.Border.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            RecipeThumb(recipe, Modifier.fillMaxWidth().height(104.dp), 46.sp)
            Surface(
                color = Color.White.copy(alpha = 0.88f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(8.dp).align(Alignment.TopStart)
            ) {
                Text(
                    recipe.course.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF3A2A25),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            if (favorite) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Preferita",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(20.dp)
                )
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                recipe.title,
                style = MaterialTheme.typography.titleSmall,
                color = Pal.Green,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = Pal.InkSoft,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    "${recipe.minutes} min",
                    style = MaterialTheme.typography.labelMedium,
                    color = Pal.InkSoft
                )
                Spacer(Modifier.weight(1f))
                DifficultyTag(recipe.difficulty)
            }
            if (coverage != null) {
                Spacer(Modifier.height(8.dp))
                val (have, total) = coverage
                val complete = have >= total
                Tag(
                    text = if (complete) "Hai tutto" else "Hai $have su $total",
                    container = if (complete) Pal.Mint else Pal.Ochre,
                    content = if (complete) Pal.MintText else Color.White
                )
            }
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Pal.Green,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Pal.InkSoft,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------- dialog ingrediente

data class ItemDraft(
    val name: String = "",
    val qty: Double? = null,
    val unit: String = "pz",
    val place: Place = Place.DISPENSA
)

private fun plain(v: Double): String =
    if (v == Math.floor(v)) v.toLong().toString() else v.toString()

/** Dialog per aggiungere o modificare un ingrediente (spesa e dispensa). */
@Composable
fun ItemDialog(
    title: String,
    confirmLabel: String,
    initial: ItemDraft,
    suggestions: List<String>,
    showPlace: Boolean,
    guessPlace: (String) -> Place?,
    onDismiss: () -> Unit,
    onConfirm: (ItemDraft) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initial.name) }
    var qtyText by remember { mutableStateOf(initial.qty?.let { plain(it) } ?: "") }
    var unit by remember { mutableStateOf(if (initial.unit.isBlank()) "pz" else initial.unit) }
    var place by remember { mutableStateOf(initial.place) }
    var placeTouched by remember { mutableStateOf(initial.name.isNotBlank()) }

    LaunchedEffect(name) {
        if (!placeTouched) {
            guessPlace(name)?.let { place = it }
        }
    }

    val matches = remember(name, suggestions) {
        val q = name.trim()
        if (q.length < 2) emptyList()
        else suggestions.filter { it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true) }.take(6)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Pal.Cream,
        title = { Text(title, color = Pal.Green) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ingrediente") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                if (matches.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        matches.forEach { s ->
                            SuggestionChip(onClick = { name = s }, label = { Text(s) })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Quantità (facoltativa)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("pz", "g", "ml").forEach { u ->
                        AppChip(selected = unit == u, onClick = { unit = u }, label = u)
                    }
                }
                if (showPlace) {
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("Dove lo metti?")
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Place.entries.forEach { p ->
                            AppChip(
                                selected = place == p,
                                onClick = { place = p; placeTouched = true },
                                label = p.label
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = confirmLabel,
                enabled = name.isNotBlank(),
                compact = true,
                onClick = {
                    onConfirm(
                        ItemDraft(
                            name = name.trim(),
                            qty = qtyText.replace(',', '.').trim().toDoubleOrNull(),
                            unit = unit,
                            place = place
                        )
                    )
                }
            )
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Elimina", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Annulla", color = Pal.InkSoft) }
            }
        }
    )
}
