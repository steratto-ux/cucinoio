@file:OptIn(ExperimentalMaterial3Api::class)

package com.iocucino.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iocucino.app.data.Meal
import com.iocucino.app.data.dayFull
import com.iocucino.app.data.defaultWeekStart
import com.iocucino.app.data.weekLabel
import com.iocucino.app.ui.MainViewModel
import com.iocucino.app.ui.components.AppButton
import com.iocucino.app.ui.components.AppChip
import com.iocucino.app.ui.components.BtnStyle
import com.iocucino.app.ui.components.CircleButton
import com.iocucino.app.ui.components.CreamCard
import com.iocucino.app.ui.components.EmptyState
import com.iocucino.app.ui.components.NoInsets
import com.iocucino.app.ui.components.Pill
import com.iocucino.app.ui.components.SageRow
import com.iocucino.app.ui.components.ScreenHeader
import com.iocucino.app.ui.components.SectionLabel
import com.iocucino.app.ui.theme.Pal
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Menu' della settimana: pranzo e cena per ogni giorno, con lista della spesa unica. */
@Composable
fun PlannerScreen(
    vm: MainViewModel,
    onOpenRecipe: (String) -> Unit,
    onPick: (LocalDate, Meal) -> Unit,
    onGoShopping: () -> Unit
) {
    val plan by vm.repo.plan.collectAsState()
    val recipes by vm.repo.recipes.collectAsState()
    var weekStartText by rememberSaveable { mutableStateOf(defaultWeekStart().toString()) }
    val weekStart = LocalDate.parse(weekStartText)
    val today = LocalDate.now()

    var menuOpen by remember { mutableStateOf(false) }
    var showGenerate by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val recipeById = remember(recipes) { recipes.associateBy { it.id } }
    val weekEnd = weekStart.plusDays(6).toString()
    val weekEntries = plan.filter { it.date >= weekStart.toString() && it.date <= weekEnd }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = NoInsets,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Menù",
                    subtitle = "Pranzo e cena della settimana",
                    actions = {
                        Box {
                            CircleButton(
                                icon = Icons.Filled.MoreVert,
                                description = "Altre azioni",
                                onClick = { menuOpen = true },
                                size = 44.dp
                            )
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                                containerColor = Pal.Cream
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Proponimi un menù") },
                                    onClick = { menuOpen = false; showGenerate = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Svuota la settimana") },
                                    onClick = { menuOpen = false; showClear = true }
                                )
                            }
                        }
                    }
                )
            }
            item {
                CreamCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircleButton(
                            icon = Icons.Filled.ChevronLeft,
                            description = "Settimana precedente",
                            onClick = { weekStartText = weekStart.minusWeeks(1).toString() }
                        )
                        Text(
                            weekLabel(weekStart),
                            style = MaterialTheme.typography.titleMedium,
                            color = Pal.Green,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        CircleButton(
                            icon = Icons.Filled.ChevronRight,
                            description = "Settimana successiva",
                            onClick = { weekStartText = weekStart.plusWeeks(1).toString() }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppButton(
                            text = "Proponimi un menù",
                            icon = Icons.Filled.AutoAwesome,
                            onClick = { showGenerate = true },
                            modifier = Modifier.weight(1f),
                            compact = true
                        )
                        if (weekEntries.isNotEmpty()) {
                            AppButton(
                                text = "Lista spesa",
                                icon = Icons.Filled.ShoppingCart,
                                style = BtnStyle.Green,
                                onClick = {
                                    val n = vm.repo.shoppingFromPlan(weekStart)
                                    if (n == 0) {
                                        scope.launch {
                                            snackbar.currentSnackbarData?.dismiss()
                                            snackbar.showSnackbar("Hai già tutto in casa o nella lista 🎉")
                                        }
                                    } else {
                                        onGoShopping()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                compact = true
                            )
                        }
                    }
                }
            }
            if (weekEntries.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🗓️",
                        title = "Settimana ancora vuota",
                        subtitle = "Tocca \"Aggiungi\" su un pasto per scegliere una ricetta, " +
                            "oppure fatti proporre un menù."
                    )
                }
            }
            items(7) { i ->
                val date = weekStart.plusDays(i.toLong())
                val isToday = date == today
                CreamCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dayFull(date),
                            style = MaterialTheme.typography.titleMedium,
                            color = Pal.Green
                        )
                        if (isToday) {
                            Spacer(Modifier.width(8.dp))
                            Pill("Oggi", container = Pal.Orange, content = Color.White)
                        }
                    }
                    Meal.entries.forEach { meal ->
                        val entries = plan.filter { it.date == date.toString() && it.meal == meal }
                        Spacer(Modifier.height(10.dp))
                        SectionLabel(meal.label)
                        Spacer(Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            entries.forEach { entry ->
                                val r = recipeById[entry.recipeId]
                                SageRow(onClick = if (r != null) ({ onOpenRecipe(entry.recipeId) }) else null) {
                                    Text(r?.emoji ?: "🍽️", fontSize = 22.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        r?.title ?: "Ricetta non più disponibile",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Pal.Ink,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { vm.repo.removePlan(entry.id) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Togli",
                                            tint = Pal.InkSoft,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .clickable { onPick(date, meal) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = Pal.OrangeDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Aggiungi",
                                    color = Pal.OrangeDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGenerate) {
        GenerateDialog(
            onDismiss = { showGenerate = false },
            onConfirm = { level, pantryFirst ->
                showGenerate = false
                vm.repo.generateWeek(weekStart, level, pantryFirst)
            }
        )
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            containerColor = Pal.Cream,
            title = { Text("Svuotare la settimana?", color = Pal.Green) },
            text = { Text("Verranno tolti tutti i piatti dal menù di ${weekLabel(weekStart)}.") },
            confirmButton = {
                TextButton(onClick = { vm.repo.clearWeek(weekStart); showClear = false }) {
                    Text("Svuota", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text("Annulla", color = Pal.InkSoft) }
            }
        )
    }
}

@Composable
private fun GenerateDialog(onDismiss: () -> Unit, onConfirm: (Int, Boolean) -> Unit) {
    var level by remember { mutableIntStateOf(2) }
    var pantryFirst by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Pal.Cream,
        title = { Text("Proponimi un menù", color = Pal.Green) },
        text = {
            Column {
                Text(
                    "Riempio solo i pasti ancora vuoti, da oggi in poi. Quello che hai già scelto resta com'è.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(14.dp))
                SectionLabel("Quanto impegno?")
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppChip(selected = level == 1, onClick = { level = 1 }, label = "Solo piatti semplici")
                    AppChip(selected = level == 2, onClick = { level = 2 }, label = "Semplici e medi")
                    AppChip(selected = level == 3, onClick = { level = 3 }, label = "Anche i più impegnativi")
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Dai la precedenza a quello che ho in casa",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = pantryFirst,
                        onCheckedChange = { pantryFirst = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Pal.Orange,
                            checkedThumbColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            AppButton(text = "Proponi", compact = true, onClick = { onConfirm(level, pantryFirst) })
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = Pal.InkSoft) } }
    )
}
