@file:OptIn(ExperimentalMaterial3Api::class)

package com.iocucino.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iocucino.app.data.PantryItem
import com.iocucino.app.data.Place
import com.iocucino.app.data.cap
import com.iocucino.app.data.quantityOrEmpty
import com.iocucino.app.ui.MainViewModel
import com.iocucino.app.ui.components.AppButton
import com.iocucino.app.ui.components.AppChip
import com.iocucino.app.ui.components.BtnStyle
import com.iocucino.app.ui.components.CircleButton
import com.iocucino.app.ui.components.CreamCard
import com.iocucino.app.ui.components.EmptyState
import com.iocucino.app.ui.components.ItemDialog
import com.iocucino.app.ui.components.ItemDraft
import com.iocucino.app.ui.components.NoInsets
import com.iocucino.app.ui.components.SageRow
import com.iocucino.app.ui.components.ScreenHeader
import com.iocucino.app.ui.components.SectionLabel
import com.iocucino.app.ui.theme.Pal
import kotlinx.coroutines.launch

/** Cosa c'e' in casa: dispensa, frigo e freezer. Si puo' aggiungere anche a mano. */
@Composable
fun PantryScreen(vm: MainViewModel, onCookWithPantry: () -> Unit) {
    val items by vm.repo.pantry.collectAsState()
    var filter by rememberSaveable { mutableStateOf<Place?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PantryItem?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val groups = remember(items, filter) {
        Place.entries
            .filter { filter == null || filter == it }
            .mapNotNull { p ->
                items.filter { it.place == p }.sortedBy { it.name }
                    .takeIf { it.isNotEmpty() }?.let { p to it }
            }
    }

    fun remove(item: PantryItem) {
        vm.repo.removePantry(item.id)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val res = snackbar.showSnackbar(
                message = "${item.name.cap()} tolto",
                actionLabel = "Annulla"
            )
            if (res == SnackbarResult.ActionPerformed) vm.repo.restorePantry(item)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = NoInsets,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            CircleButton(
                icon = Icons.Filled.Add,
                description = "Aggiungi in casa",
                onClick = { showAdd = true },
                size = 58.dp,
                accent = true
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    ScreenHeader(
                        title = "Dispensa",
                        subtitle = if (items.isEmpty()) "Ancora niente in casa"
                        else if (items.size == 1) "1 ingrediente in casa" else "${items.size} ingredienti in casa"
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppChip(selected = filter == null, onClick = { filter = null }, label = "Tutto")
                        Place.entries.forEach { p ->
                            AppChip(
                                selected = filter == p,
                                onClick = { filter = if (filter == p) null else p },
                                label = "${p.emoji} ${p.label}"
                            )
                        }
                    }
                    if (items.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        AppButton(
                            text = "Cosa cucino con quello che ho?",
                            icon = Icons.Filled.Restaurant,
                            style = BtnStyle.Green,
                            onClick = onCookWithPantry,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🧊",
                        title = "Dispensa vuota",
                        subtitle = "Qui finiscono gli ingredienti che segni come comprati nella lista della spesa. " +
                            "Puoi aggiungere a mano quello che hai già, con il tasto +."
                    )
                }
            }

            items(groups, key = { it.first.name }) { (place, list) ->
                CreamCard {
                    SectionLabel("${place.emoji}  ${place.label}")
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        list.forEach { item ->
                            SageRow(onClick = { editing = item }) {
                                Text(
                                    item.name.cap(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Pal.Ink,
                                    modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                                )
                                val q = quantityOrEmpty(item.qty, item.unit)
                                if (q.isNotEmpty()) {
                                    Text(
                                        q,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Pal.Green,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                }
                                IconButton(onClick = { remove(item) }, modifier = Modifier.size(34.dp)) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Togli",
                                        tint = Pal.InkSoft,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ItemDialog(
            title = "Aggiungi in casa",
            confirmLabel = "Aggiungi",
            initial = ItemDraft(),
            suggestions = vm.repo.knownIngredients,
            showPlace = true,
            guessPlace = { vm.repo.guessPlace(it) },
            onDismiss = { showAdd = false },
            onConfirm = { d ->
                vm.repo.addPantry(d.name, d.qty, d.unit, d.place)
                showAdd = false
            }
        )
    }

    editing?.let { item ->
        ItemDialog(
            title = "Modifica",
            confirmLabel = "Salva",
            initial = ItemDraft(item.name, item.qty, item.unit.ifBlank { "pz" }, item.place),
            suggestions = vm.repo.knownIngredients,
            showPlace = true,
            guessPlace = { null },
            onDismiss = { editing = null },
            onConfirm = { d ->
                vm.repo.updatePantry(
                    item.copy(
                        name = d.name.lowercase(),
                        qty = d.qty,
                        unit = if (d.qty == null) "" else d.unit,
                        place = d.place
                    )
                )
                editing = null
            },
            onDelete = {
                remove(item)
                editing = null
            }
        )
    }
}
