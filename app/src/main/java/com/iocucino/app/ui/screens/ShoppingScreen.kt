@file:OptIn(ExperimentalMaterial3Api::class)

package com.iocucino.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iocucino.app.data.Aisle
import com.iocucino.app.data.ShoppingItem
import com.iocucino.app.data.cap
import com.iocucino.app.data.quantityOrEmpty
import com.iocucino.app.ui.MainViewModel
import com.iocucino.app.ui.components.AppButton
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

/** Lista della spesa: toccando il cerchio l'ingrediente passa in dispensa/frigo/freezer. */
@Composable
fun ShoppingScreen(vm: MainViewModel) {
    val items by vm.repo.shopping.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ShoppingItem?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmBuyAll by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val groups = remember(items) {
        Aisle.entries.mapNotNull { a ->
            items.filter { it.aisle == a }.sortedBy { it.name }
                .takeIf { it.isNotEmpty() }?.let { a to it }
        }
    }

    fun buy(item: ShoppingItem) {
        val bought = vm.repo.buy(item.id) ?: return
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val res = snackbar.showSnackbar(
                message = "${bought.name.cap()} → ${bought.aisle.place.label}",
                actionLabel = "Annulla",
                duration = SnackbarDuration.Short
            )
            if (res == SnackbarResult.ActionPerformed) vm.repo.undoBuy(bought)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = NoInsets,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            CircleButton(
                icon = Icons.Filled.Add,
                description = "Aggiungi alla lista",
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
                ScreenHeader(
                    title = "Spesa",
                    subtitle = if (items.isEmpty()) "Lista vuota"
                    else if (items.size == 1) "1 cosa da comprare" else "${items.size} cose da comprare",
                    actions = {
                        if (items.isNotEmpty()) {
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
                                        text = { Text("Segna tutto come comprato") },
                                        onClick = { menuOpen = false; confirmBuyAll = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Svuota la lista") },
                                        onClick = { menuOpen = false; confirmClear = true }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            if (items.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🛒",
                        title = "La lista è vuota",
                        subtitle = "Crea la lista dal Menù della settimana, aggiungi gli ingredienti " +
                            "mancanti da una ricetta oppure scrivili a mano con il tasto +."
                    )
                }
            } else {
                item {
                    AppButton(
                        text = "Spesa fatta: metti tutto via",
                        style = BtnStyle.Green,
                        onClick = { confirmBuyAll = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(groups, key = { it.first.name }) { (aisle, list) ->
                CreamCard {
                    SectionLabel("${aisle.emoji}  ${aisle.label}")
                    Spacer(Modifier.size(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        list.forEach { item ->
                            SageRow(onClick = { editing = item }) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Pal.CreamHi)
                                        .border(2.dp, Pal.Orange, CircleShape)
                                        .clickable { buy(item) },
                                    contentAlignment = Alignment.Center
                                ) {}
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.name.cap(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Pal.Ink
                                    )
                                    if (item.note.isNotBlank()) {
                                        Text(
                                            item.note,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Pal.InkSoft,
                                            maxLines = 1
                                        )
                                    }
                                }
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
                                IconButton(
                                    onClick = { vm.repo.removeShopping(item.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Togli dalla lista",
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
            title = "Aggiungi alla spesa",
            confirmLabel = "Aggiungi",
            initial = ItemDraft(),
            suggestions = vm.repo.knownIngredients,
            showPlace = false,
            guessPlace = { null },
            onDismiss = { showAdd = false },
            onConfirm = { d ->
                vm.repo.addShopping(d.name, d.qty, d.unit)
                showAdd = false
            }
        )
    }

    editing?.let { item ->
        ItemDialog(
            title = "Modifica",
            confirmLabel = "Salva",
            initial = ItemDraft(item.name, item.qty, item.unit.ifBlank { "pz" }),
            suggestions = vm.repo.knownIngredients,
            showPlace = false,
            guessPlace = { null },
            onDismiss = { editing = null },
            onConfirm = { d ->
                vm.repo.updateShopping(
                    item.copy(
                        name = d.name.lowercase(),
                        qty = d.qty,
                        unit = if (d.qty == null) "" else d.unit
                    )
                )
                editing = null
            },
            onDelete = {
                vm.repo.removeShopping(item.id)
                editing = null
            }
        )
    }

    if (confirmBuyAll) {
        AlertDialog(
            onDismissRequest = { confirmBuyAll = false },
            containerColor = Pal.Cream,
            title = { Text("Spesa fatta?", color = Pal.Green) },
            text = { Text("Tutti gli ingredienti della lista vengono messi in dispensa, frigo o freezer.") },
            confirmButton = {
                AppButton(text = "Sì, metti via tutto", compact = true, onClick = {
                    vm.repo.buyAll()
                    confirmBuyAll = false
                })
            },
            dismissButton = {
                TextButton(onClick = { confirmBuyAll = false }) { Text("Annulla", color = Pal.InkSoft) }
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Pal.Cream,
            title = { Text("Svuotare la lista?", color = Pal.Green) },
            text = { Text("Gli ingredienti vengono tolti dalla lista senza finire in dispensa.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.repo.clearShopping()
                    confirmClear = false
                }) { Text("Svuota", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Annulla", color = Pal.InkSoft) }
            }
        )
    }
}
