@file:OptIn(ExperimentalMaterial3Api::class)

package com.iocucino.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iocucino.app.data.Course
import com.iocucino.app.data.Difficulty
import com.iocucino.app.data.Recipe
import com.iocucino.app.data.keyOf
import com.iocucino.app.ui.MainViewModel
import com.iocucino.app.ui.components.AppButton
import com.iocucino.app.ui.components.AppChip
import com.iocucino.app.ui.components.BackBar
import com.iocucino.app.ui.components.BtnStyle
import com.iocucino.app.ui.components.CircleButton
import com.iocucino.app.ui.components.CreamCard
import com.iocucino.app.ui.components.EmptyState
import com.iocucino.app.ui.components.NumberCircle
import com.iocucino.app.ui.components.RecipeCard
import com.iocucino.app.ui.components.ScreenHeader
import com.iocucino.app.ui.components.SectionLabel
import com.iocucino.app.ui.components.fieldColors
import com.iocucino.app.ui.components.imageModel
import com.iocucino.app.ui.theme.Pal
import com.iocucino.app.ui.theme.courseColors
import java.time.LocalDate
import kotlin.random.Random

/** Schermata principale: idee, filtri per tipo di piatto, impegno e tempo. */
@Composable
fun RecipesScreen(vm: MainViewModel, onOpen: (String) -> Unit, onSettings: () -> Unit) {
    RecipeBrowser(vm = vm, pickTitle = null, onOpen = onOpen, onSettings = onSettings, onBack = null)
}

/** Stessa lista, usata per scegliere una ricetta da inserire nel menu'. */
@Composable
fun RecipePickerScreen(vm: MainViewModel, title: String, onPick: (String) -> Unit, onBack: () -> Unit) {
    RecipeBrowser(vm = vm, pickTitle = title, onOpen = onPick, onSettings = null, onBack = onBack)
}

private fun matchesQuery(r: Recipe, q: String): Boolean =
    r.title.contains(q, ignoreCase = true) ||
        r.tags.any { it.contains(q, ignoreCase = true) } ||
        r.ingredients.any { it.name.contains(q, ignoreCase = true) }

@Composable
private fun RecipeBrowser(
    vm: MainViewModel,
    pickTitle: String?,
    onOpen: (String) -> Unit,
    onSettings: (() -> Unit)?,
    onBack: (() -> Unit)?
) {
    val pickMode = pickTitle != null
    val recipes by vm.repo.recipes.collectAsState()
    val pantry by vm.repo.pantry.collectAsState()
    val favs by vm.repo.favorites.collectAsState()
    val settings by vm.repo.settings.collectAsState()
    val pantryMode by vm.pantryMode.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var course by rememberSaveable { mutableStateOf<Course?>(null) }
    var diff by rememberSaveable { mutableStateOf<Difficulty?>(null) }
    var maxMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
    var onlyFav by rememberSaveable { mutableStateOf(false) }
    var heroSeed by rememberSaveable { mutableStateOf(0) }

    // Per ogni ricetta: quanti ingredienti (esclusi sale, olio...) ci sono gia' in dispensa.
    val coverage = remember(recipes, pantry) {
        val keys = pantry.map { keyOf(it.name) }.toSet()
        recipes.associate { r ->
            val ing = r.ingredients.filter { !it.staple }
            r.id to (ing.count { keyOf(it.name) in keys } to ing.size)
        }
    }

    val filtered = remember(recipes, query, course, diff, maxMinutes, onlyFav, favs, pantryMode, coverage) {
        val base = recipes.filter { r ->
            (course == null || r.course == course) &&
                (diff == null || r.difficulty == diff) &&
                (maxMinutes?.let { r.minutes <= it } ?: true) &&
                (!onlyFav || r.id in favs) &&
                (query.isBlank() || matchesQuery(r, query.trim()))
        }
        if (pantryMode) {
            base.filter { (coverage[it.id]?.first ?: 0) > 0 }
                .sortedBy { r ->
                    val c = coverage[r.id] ?: (0 to 0)
                    c.second - c.first
                }
        } else {
            base
        }
    }

    val hero: Recipe? = remember(filtered, heroSeed) {
        if (filtered.isEmpty()) null
        else filtered[Random(LocalDate.now().toEpochDay() + heroSeed).nextInt(filtered.size)]
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                if (pickMode) {
                    BackBar(onBack = { onBack?.invoke() })
                    ScreenHeader(
                        title = pickTitle ?: "",
                        subtitle = "Scegli una ricetta da mettere nel menù"
                    )
                } else {
                    ScreenHeader(
                        title = "Io cucino!",
                        subtitle = "Idee e ricette al volo",
                        actions = {
                            CircleButton(
                                icon = Icons.Filled.Settings,
                                description = "Impostazioni",
                                onClick = { onSettings?.invoke() },
                                size = 44.dp
                            )
                        }
                    )
                    if (hero != null) {
                        HeroCard(
                            recipe = hero,
                            onOpen = { onOpen(hero.id) },
                            onShuffle = { heroSeed += 1 }
                        )
                    }
                }
            }
        }

        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cerca un piatto o un ingrediente") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancella")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)
                )
                CreamCard {
                    if (!pickMode) {
                        SectionLabel("Per quante persone cucini?")
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (n in 1..8) {
                                NumberCircle(n, selected = settings.servings == n) { vm.repo.setServings(n) }
                            }
                        }
                        if (pantry.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            AppButton(
                                text = if (pantryMode) "Solo ricette con i tuoi ingredienti: tocca per toglierlo"
                                else "Cosa cucino con ${pantry.size} ingredienti in casa?",
                                onClick = { vm.pantryMode.value = !pantryMode },
                                style = if (pantryMode) BtnStyle.Orange else BtnStyle.Green,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    SectionLabel("Tipo di piatto")
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppChip(selected = course == null, onClick = { course = null }, label = "Tutti")
                        Course.entries.forEach { c ->
                            AppChip(
                                selected = course == c,
                                onClick = { course = if (course == c) null else c },
                                label = "${c.emoji} ${c.plural}"
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("Impegno")
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppChip(selected = diff == null, onClick = { diff = null }, label = "Qualsiasi")
                        Difficulty.entries.forEach { d ->
                            AppChip(
                                selected = diff == d,
                                onClick = { diff = if (diff == d) null else d },
                                label = d.plural
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("Tempo a disposizione")
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppChip(selected = maxMinutes == null, onClick = { maxMinutes = null }, label = "Qualsiasi")
                        listOf(15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "1 ora").forEach { (m, label) ->
                            AppChip(
                                selected = maxMinutes == m,
                                onClick = { maxMinutes = if (maxMinutes == m) null else m },
                                label = label
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    AppChip(
                        selected = onlyFav,
                        onClick = { onlyFav = !onlyFav },
                        label = "Solo preferite",
                        icon = Icons.Filled.Favorite
                    )
                }
                Spacer(Modifier.height(12.dp))
                SectionLabel(if (filtered.size == 1) "1 ricetta" else "${filtered.size} ricette")
            }
        }

        if (filtered.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    emoji = "🔍",
                    title = "Nessuna ricetta trovata",
                    subtitle = if (pantryMode) {
                        "Con gli ingredienti in dispensa non c'è ancora nessuna ricetta. " +
                            "Aggiungi qualcosa in Dispensa o togli il filtro."
                    } else {
                        "Prova a cambiare i filtri o la ricerca."
                    }
                )
            }
        }

        items(filtered, key = { it.id }) { r ->
            RecipeCard(
                recipe = r,
                favorite = r.id in favs,
                coverage = if (pantryMode) coverage[r.id] else null,
                onClick = { onOpen(r.id) }
            )
        }

        if (pickMode) {
            item(key = "end", span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.navigationBarsPadding().height(8.dp))
            }
        }
    }
}

@Composable
private fun HeroCard(recipe: Recipe, onOpen: () -> Unit, onShuffle: () -> Unit) {
    val (c1, c2) = courseColors(recipe.course)
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Pal.Cream),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(176.dp)
                .background(Brush.linearGradient(listOf(c1, c2)))
        ) {
            if (recipe.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageModel(recipe.imageUrl),
                    contentDescription = recipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0xB0000000))))
                )
            }
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {
                    Text(
                        "L'IDEA DI OGGI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        recipe.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${recipe.minutes} min · ${recipe.difficulty.label} · ${recipe.course.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
                if (recipe.imageUrl.isBlank()) {
                    Text(recipe.emoji, fontSize = 64.sp, modifier = Modifier.padding(start = 8.dp, end = 4.dp))
                }
            }
            CircleButton(
                icon = Icons.Filled.Shuffle,
                description = "Un'altra idea",
                onClick = onShuffle,
                size = 40.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
            )
        }
    }
}
