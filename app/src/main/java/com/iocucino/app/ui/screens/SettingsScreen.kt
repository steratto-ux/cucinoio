@file:OptIn(ExperimentalMaterial3Api::class)

package com.iocucino.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.iocucino.app.data.formatDateTime
import com.iocucino.app.ui.MainViewModel
import com.iocucino.app.ui.components.AppButton
import com.iocucino.app.ui.components.BackBar
import com.iocucino.app.ui.components.CreamCard
import com.iocucino.app.ui.components.NumberCircle
import com.iocucino.app.ui.components.ScreenHeader
import com.iocucino.app.ui.components.fieldColors
import com.iocucino.app.ui.theme.Pal

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.repo.settings.collectAsState()
    val recipes by vm.repo.recipes.collectAsState()
    var url by remember { mutableStateOf(settings.remoteUrl) }
    var status by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        BackBar(onBack)
        ScreenHeader(title = "Impostazioni")

        CreamCard {
            Text(
                "Quante persone mangiano?",
                style = MaterialTheme.typography.titleMedium,
                color = Pal.Green
            )
            Text(
                "Serve per calcolare le quantità degli ingredienti e la lista della spesa.",
                style = MaterialTheme.typography.bodyMedium,
                color = Pal.InkSoft
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (n in 1..8) {
                    NumberCircle(n, selected = settings.servings == n) { vm.repo.setServings(n) }
                    if (n < 8) Spacer(Modifier.width(10.dp))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        CreamCard {
            Text(
                "Ricette online",
                style = MaterialTheme.typography.titleMedium,
                color = Pal.Green
            )
            Text(
                "Incolla l'indirizzo del file recipes.json del tuo catalogo: l'app lo scarica da sola " +
                    "ogni tanto e così trovi sempre ricette nuove, senza aggiornare l'app.",
                style = MaterialTheme.typography.bodyMedium,
                color = Pal.InkSoft
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    vm.repo.setRemoteUrl(it)
                },
                label = { Text("Indirizzo del catalogo") },
                placeholder = { Text("https://raw.githubusercontent.com/...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppButton(
                    text = if (loading) "Un attimo…" else "Aggiorna ora",
                    enabled = !loading && url.isNotBlank(),
                    compact = true,
                    onClick = {
                        loading = true
                        status = "Scarico le ricette…"
                        vm.syncRecipes {
                            status = it
                            loading = false
                        }
                    }
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (settings.lastSync == 0L) "Mai aggiornato"
                    else "Ultimo: ${formatDateTime(settings.lastSync)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Pal.InkSoft
                )
            }
            if (status.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(status, style = MaterialTheme.typography.bodyMedium, color = Pal.Ink)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Ricette disponibili ora: ${recipes.size} (incluse nell'app: ${vm.repo.bundledCount}, " +
                    "dal catalogo online: ${vm.repo.onlineCount})",
                style = MaterialTheme.typography.bodySmall,
                color = Pal.InkSoft
            )
        }

        Spacer(Modifier.height(14.dp))

        CreamCard {
            Text(
                "Foto delle ricette",
                style = MaterialTheme.typography.titleMedium,
                color = Pal.Green
            )
            Text(
                "Le foto dei piatti si cercano da sole su Wikipedia/Wikimedia Commons, solo quando " +
                    "sei online. In ogni ricetta, con il tasto della fotocamera, puoi scegliere una tua " +
                    "foto dalla galleria: ha sempre la precedenza.",
                style = MaterialTheme.typography.bodyMedium,
                color = Pal.InkSoft
            )
        }

        Spacer(Modifier.height(14.dp))

        CreamCard {
            Text("Io cucino!", style = MaterialTheme.typography.titleMedium, color = Pal.Green)
            Text(
                "Versione 0.2 · Ricette, menù settimanale, lista della spesa e dispensa.",
                style = MaterialTheme.typography.bodyMedium,
                color = Pal.InkSoft
            )
        }
        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}
