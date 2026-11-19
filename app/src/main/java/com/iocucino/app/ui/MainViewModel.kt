package com.iocucino.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iocucino.app.MenuWidgetProvider
import com.iocucino.app.data.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repo = Repository(app.applicationContext)

    /** Se attivo, l'elenco ricette viene ordinato in base a quello che c'e' in dispensa. */
    val pantryMode = MutableStateFlow(false)

    init {
        // Ogni modifica al menu' aggiorna il widget "Menu' di oggi".
        val appContext = app.applicationContext
        repo.onMenuChanged = { MenuWidgetProvider.refreshAll(appContext) }
        MenuWidgetProvider.refreshAll(appContext)
        viewModelScope.launch {
            // Aggiornamento automatico del catalogo online, al massimo una volta ogni 12 ore.
            val s = repo.settings.value
            val twelveHours = 12L * 60 * 60 * 1000
            if (s.remoteUrl.isNotBlank() && System.currentTimeMillis() - s.lastSync > twelveHours) {
                repo.syncRemote()
            }
            // Foto dei piatti da Wikipedia (una sola volta: poi restano in memoria).
            repo.resolveWikiPhotos()
        }
    }

    fun syncRecipes(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val result = repo.syncRemote()
            val text = result.fold(
                onSuccess = {
                    repo.resolveWikiPhotos()
                    "Catalogo aggiornato: $it ricette online"
                },
                onFailure = { "Non riesco ad aggiornare: ${it.message ?: "errore di rete"}" }
            )
            onDone(text)
        }
    }
}
