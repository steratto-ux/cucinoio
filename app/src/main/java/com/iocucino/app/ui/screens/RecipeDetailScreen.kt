@file:OptIn(ExperimentalMaterial3Api::class)

package com.iocucino.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iocucino.app.data.Meal
import com.iocucino.app.data.Recipe
import com.iocucino.app.data.Season
import com.iocucino.app.data.SeasonStatus
import com.iocucino.app.data.cap
import com.iocucino.app.data.dayLabelRelative
import com.iocucino.app.data.keyOf
import com.iocucino.app.data.quantityLabel
import com.iocucino.app.data.scaleQty
import com.iocucino.app.ui.MainViewModel
import com.iocucino.app.ui.components.AppButton
import com.iocucino.app.ui.components.AppChip
import com.iocucino.app.ui.components.BackBar
import com.iocucino.app.ui.components.BtnStyle
import com.iocucino.app.ui.components.CircleButton
import com.iocucino.app.ui.components.CreamCard
import com.iocucino.app.ui.components.DifficultyTag
import com.iocucino.app.ui.components.NoInsets
import com.iocucino.app.ui.components.Pill
import com.iocucino.app.ui.components.RecipeThumb
import com.iocucino.app.ui.components.SageRow
import com.iocucino.app.ui.components.SectionLabel
import com.iocucino.app.ui.theme.Pal
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun RecipeDetailScreen(vm: MainViewModel, recipeId: String, onBack: () -> Unit) {
    val recipes by vm.repo.recipes.collectAsState()
    val recipe = recipes.firstOrNull { it.id == recipeId }
    if (recipe == null) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            BackBar(onBack)
            Spacer(Modifier.height(16.dp))
            Text(
                "Ricetta non trovata",
                style = MaterialTheme.typography.titleMedium,
                color = Pal.Green
            )
        }
        return
    }
    RecipeDetailContent(vm, recipe, onBack)
}

@Composable
private fun RecipeDetailContent(vm: MainViewModel, recipe: Recipe, onBack: () -> Unit) {
    val pantry by vm.repo.pantry.collectAsState()
    val favs by vm.repo.favorites.collectAsState()
    val settings by vm.repo.settings.collectAsState()

    var servings by rememberSaveable(recipe.id) { mutableStateOf(settings.servings) }
    var showPlanDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val pantryKeys = remember(pantry) { pantry.map { keyOf(it.name) }.toSet() }
    val factor = servings.toDouble() / recipe.servings
    val isFav = recipe.id in favs
    val nonStaple = recipe.ingredients.filter { !it.staple }
    val have = nonStaple.count { keyOf(it.name) in pantryKeys }

    fun say(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = vm.repo.setUserPhoto(recipe.id, uri)
                say(if (ok) "Foto salvata" else "Non riesco a leggere questa foto")
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = NoInsets,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ---- foto in testa
            item {
                Box(Modifier.fillMaxWidth().height(250.dp)) {
                    RecipeThumb(recipe, Modifier.fillMaxSize(), 110.sp)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .background(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent)))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CircleButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            description = "Indietro",
                            onClick = onBack
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircleButton(
                                icon = Icons.Filled.PhotoCamera,
                                description = "Foto della ricetta",
                                onClick = { showPhotoDialog = true }
                            )
                            CircleButton(
                                icon = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                description = "Preferita",
                                onClick = { vm.repo.toggleFavorite(recipe.id) },
                                accent = isFav
                            )
                        }
                    }
                }
            }

            // ---- titolo e tag
            item {
                Column(Modifier.padding(horizontal = 16.dp).padding(top = 14.dp)) {
                    CreamCard {
                        Text(
                            recipe.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Pal.Green
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Pill(
                                    recipe.course.label,
                                    container = Pal.Sage,
                                    content = Pal.Green
                                )
                            }
                            item { DifficultyTag(recipe.difficulty) }
                            if (Season.status(recipe) == SeasonStatus.IN) {
                                item { Pill("🌿 Di stagione", container = Pal.Mint, content = Pal.MintText) }
                            }
                            item { Pill("${recipe.minutes} min", icon = Icons.Outlined.Timer) }
                            items(recipe.tags) { t -> Pill(t) }
                        }
                        if (recipe.photoCredit.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Foto: ${recipe.photoCredit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Pal.InkSoft
                            )
                        }
                    }
                }
            }

            // ---- porzioni e azioni
            item {
                Column(Modifier.padding(horizontal = 16.dp).padding(top = 14.dp)) {
                    CreamCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Per $servings ${if (servings == 1) "persona" else "persone"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Pal.Green,
                                modifier = Modifier.weight(1f)
                            )
                            CircleButton(
                                icon = Icons.Filled.Remove,
                                description = "Meno",
                                onClick = { if (servings > 1) servings -= 1 }
                            )
                            Spacer(Modifier.width(10.dp))
                            CircleButton(
                                icon = Icons.Filled.Add,
                                description = "Più",
                                onClick = { if (servings < 12) servings += 1 },
                                accent = true
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppButton(
                                text = "Nel menù",
                                icon = Icons.Outlined.CalendarMonth,
                                onClick = { showPlanDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "Manca → spesa",
                                icon = Icons.Filled.ShoppingCart,
                                style = BtnStyle.Green,
                                onClick = {
                                    val n = vm.repo.addRecipeToShopping(recipe, servings)
                                    say(
                                        if (n == 0) "Hai già tutto in casa o in lista 🎉"
                                        else "Aggiunti $n ingredienti alla lista della spesa"
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        AppButton(
                            text = "Ho cucinato questa: scala dalla dispensa",
                            icon = Icons.Filled.Check,
                            style = BtnStyle.Cream,
                            onClick = {
                                vm.repo.cook(recipe, servings)
                                say("Fatto! Ho scalato gli ingredienti dalla dispensa")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ---- ingredienti
            item {
                Column(Modifier.padding(horizontal = 16.dp).padding(top = 14.dp)) {
                    CreamCard {
                        Text(
                            "Ingredienti",
                            style = MaterialTheme.typography.titleLarge,
                            color = Pal.Green
                        )
                        Text(
                            "In casa ne hai $have su ${nonStaple.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Pal.InkSoft
                        )
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recipe.ingredients.forEach { ing ->
                                val inPantry = ing.staple || keyOf(ing.name) in pantryKeys
                                val qty = scaleQty(ing.qty, ing.unit, factor)
                                SageRow(color = if (inPantry) Pal.Sage else Pal.CreamHi) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (inPantry) Pal.GreenMid else Pal.Line),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (inPantry) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = "In casa",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            ing.name.cap(),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Pal.Ink
                                        )
                                        if (ing.staple) {
                                            Text(
                                                "di solito in casa",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Pal.InkSoft
                                            )
                                        }
                                    }
                                    Text(
                                        quantityLabel(qty, ing.unit),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Pal.Green
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---- preparazione
            item {
                Column(Modifier.padding(horizontal = 16.dp).padding(top = 14.dp)) {
                    CreamCard {
                        Text(
                            "Preparazione",
                            style = MaterialTheme.typography.titleLarge,
                            color = Pal.Green
                        )
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            recipe.steps.forEachIndexed { i, step ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.verticalGradient(listOf(Pal.OrangeLight, Pal.OrangeDark))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${i + 1}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        step,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Pal.Ink,
                                        modifier = Modifier.weight(1f).padding(top = 3.dp)
                                    )
                                }
                            }
                        }
                        if (recipe.note.isNotBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Pal.Sand)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    "💡 ${recipe.note}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Pal.SandText
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding().height(8.dp)) }
        }
    }

    if (showPlanDialog) {
        PlanDialog(
            onDismiss = { showPlanDialog = false },
            onConfirm = { date, meal ->
                vm.repo.addPlan(date, meal, recipe.id, servings)
                showPlanDialog = false
                say("Aggiunta al menù: ${dayLabelRelative(date)}, ${meal.label.lowercase()}")
            }
        )
    }

    if (showPhotoDialog) {
        val hasOwn = vm.repo.hasUserPhoto(recipe.id)
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            containerColor = Pal.Cream,
            title = { Text("Foto della ricetta", color = Pal.Green) },
            text = {
                Text(
                    if (hasOwn) "Stai usando una tua foto. Puoi cambiarla o tornare a quella di default."
                    else "Scegli dalla galleria una foto del piatto: resta sul tuo telefono e sostituisce quella automatica.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                AppButton(
                    text = "Scegli dalla galleria",
                    compact = true,
                    onClick = {
                        showPhotoDialog = false
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            },
            dismissButton = {
                Row {
                    if (hasOwn) {
                        TextButton(onClick = {
                            vm.repo.removeUserPhoto(recipe.id)
                            showPhotoDialog = false
                            say("Foto personale tolta")
                        }) { Text("Togli la mia", color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { showPhotoDialog = false }) { Text("Annulla", color = Pal.InkSoft) }
                }
            }
        )
    }
}

@Composable
private fun PlanDialog(onDismiss: () -> Unit, onConfirm: (LocalDate, Meal) -> Unit) {
    val today = remember { LocalDate.now() }
    var dayIndex by remember { mutableIntStateOf(0) }
    var meal by remember { mutableStateOf(Meal.CENA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Pal.Cream,
        title = { Text("Aggiungi al menù", color = Pal.Green) },
        text = {
            Column {
                SectionLabel("Giorno")
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(14) { i ->
                        AppChip(
                            selected = dayIndex == i,
                            onClick = { dayIndex = i },
                            label = dayLabelRelative(today.plusDays(i.toLong()))
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                SectionLabel("Pasto")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Meal.entries.forEach { m ->
                        AppChip(selected = meal == m, onClick = { meal = m }, label = m.label)
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Aggiungi",
                compact = true,
                onClick = { onConfirm(today.plusDays(dayIndex.toLong()), meal) }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = Pal.InkSoft) } }
    )
}
