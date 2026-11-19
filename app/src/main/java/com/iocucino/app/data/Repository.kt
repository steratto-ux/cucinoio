package com.iocucino.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * Archivio dell'app: ricette (incluse nell'app + aggiornamento online), dispensa,
 * lista della spesa e menu' settimanale. I dati dell'utente sono salvati in file JSON
 * nella memoria privata dell'app.
 */
/** Catalogo online predefinito: un file recipes.json nel repository GitHub dell'utente. */
const val DEFAULT_CATALOG_URL =
    "https://raw.githubusercontent.com/steratto-ux/io-cucino-ricette/main/recipes.json"

class Repository(private val ctx: Context) {

    private val prefs = ctx.getSharedPreferences("io_cucino", Context.MODE_PRIVATE)
    private val writer = Executors.newSingleThreadExecutor()

    val recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val pantry = MutableStateFlow<List<PantryItem>>(emptyList())
    val shopping = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val plan = MutableStateFlow<List<PlanEntry>>(emptyList())
    val favorites = MutableStateFlow<Set<String>>(emptySet())
    val settings = MutableStateFlow(AppSettings())

    private var aisleIndex: Map<String, Aisle> = emptyMap()

    /** Chiamata ogni volta che il menu' cambia: serve ad aggiornare il widget. */
    var onMenuChanged: () -> Unit = {}

    // Foto: quelle del catalogo, quelle trovate su Wikipedia e quelle scattate dall'utente.
    private var catalog: List<Recipe> = emptyList()
    private var userPhotos: Map<String, String> = emptyMap() // id ricetta -> percorso file
    private var wikiPhotos: Map<String, String> = emptyMap() // id ricetta -> indirizzo
    private var wikiTried: Map<String, String> = emptyMap() // id ricetta -> quando e' stata cercata senza successo

    /** Nomi degli ingredienti noti (per i suggerimenti quando si aggiunge a mano). */
    var knownIngredients: List<String> = emptyList()
        private set
    var bundledCount: Int = 0
        private set
    var onlineCount: Int = 0
        private set

    init {
        settings.value = AppSettings(
            servings = prefs.getInt("servings", 4),
            remoteUrl = prefs.getString("remoteUrl", DEFAULT_CATALOG_URL) ?: DEFAULT_CATALOG_URL,
            lastSync = prefs.getLong("lastSync", 0L)
        )
        favorites.value = prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()
        userPhotos = loadMap("userPhotos")
        wikiPhotos = loadMap("wikiPhotos")
        wikiTried = loadMap("wikiTried")
        reloadRecipes()
        pantry.value = readList("pantry.json") { it.toPantryItem() }
        shopping.value = readList("shopping.json") { it.toShoppingItem() }
        plan.value = readList("plan.json") { it.toPlanEntry() }
        publishMenuSnapshot()
    }

    private fun newId(): String = UUID.randomUUID().toString()

    // ---------------------------------------------------------------- ricette

    fun reloadRecipes() {
        val bundled = runCatching {
            parseRecipes(ctx.assets.open("recipes.json").bufferedReader().use { it.readText() })
        }.getOrDefault(emptyList())
        val remoteFile = File(ctx.filesDir, "recipes_remote.json")
        val remote = if (remoteFile.exists()) {
            runCatching { parseRecipes(remoteFile.readText()) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        bundledCount = bundled.size
        onlineCount = remote.size
        val merged = LinkedHashMap<String, Recipe>()
        bundled.forEach { merged[it.id] = it }
        remote.forEach { merged[it.id] = it } // le ricette online sostituiscono quelle con lo stesso id
        setRecipes(merged.values.toList())
    }

    private fun setRecipes(list: List<Recipe>) {
        catalog = list
        publishRecipes()
        val idx = HashMap<String, Aisle>()
        val names = HashSet<String>()
        for (r in list) {
            for (i in r.ingredients) {
                idx.putIfAbsent(keyOf(i.name), i.aisle)
                if (!i.staple) names.add(i.name)
            }
        }
        aisleIndex = idx
        knownIngredients = names.sorted()
    }

    /** Pubblica l'elenco ricette con la foto da mostrare: la tua, poi quella del catalogo, poi quella di Wikipedia. */
    private fun publishRecipes() {
        val first = recipes.value.isEmpty()
        recipes.value = catalog.map { r ->
            val mine = userPhotos[r.id]?.takeIf { File(it).exists() }
            val wiki = wikiPhotos[r.id]
            when {
                mine != null -> r.copy(imageUrl = mine, photoCredit = "")
                r.imageUrl.isNotBlank() -> r
                wiki != null -> r.copy(imageUrl = wiki, photoCredit = "Foto da Wikipedia")
                else -> r
            }
        }
        if (!first) publishMenuSnapshot()
    }

    private fun loadMap(key: String): Map<String, String> {
        val text = prefs.getString(key, null) ?: return emptyMap()
        return runCatching {
            JSONArray(text).objects().associate { it.getString("id") to it.getString("v") }
        }.getOrDefault(emptyMap())
    }

    private fun saveMap(key: String, map: Map<String, String>) {
        val arr = JSONArray()
        map.forEach { (k, v) -> arr.put(JSONObject().put("id", k).put("v", v)) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    /** Cerca su Wikipedia una foto per le ricette che non ne hanno. Si ferma se manca la rete. */
    suspend fun resolveWikiPhotos() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val retryAfter = 7L * 24 * 60 * 60 * 1000
        for (r in catalog) {
            if (r.imageUrl.isNotBlank() || r.wiki.isBlank()) continue
            if (wikiPhotos.containsKey(r.id)) continue
            val tried = wikiTried[r.id]?.toLongOrNull()
            if (tried != null && now - tried < retryAfter) continue
            val result = runCatching { WikiPhotos.lookup(r.wiki) }
            if (result.isFailure) break
            val url = result.getOrNull()
            if (url != null) {
                wikiPhotos = wikiPhotos + (r.id to url)
                saveMap("wikiPhotos", wikiPhotos)
                publishRecipes()
            } else {
                wikiTried = wikiTried + (r.id to now.toString())
                saveMap("wikiTried", wikiTried)
            }
            delay(120)
        }
    }

    /** Salva la foto scelta dall'utente per una ricetta. */
    suspend fun setUserPhoto(recipeId: String, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(ctx.filesDir, "photos")
            dir.mkdirs()
            val dest = File(dir, "$recipeId-${System.currentTimeMillis()}.jpg")
            val input = ctx.contentResolver.openInputStream(uri) ?: error("Foto non leggibile")
            input.use { src -> dest.outputStream().use { out -> src.copyTo(out) } }
            val old = userPhotos[recipeId]
            userPhotos = userPhotos + (recipeId to dest.absolutePath)
            saveMap("userPhotos", userPhotos)
            if (old != null) File(old).delete()
            publishRecipes()
            true
        }.getOrDefault(false)
    }

    fun removeUserPhoto(recipeId: String) {
        val old = userPhotos[recipeId] ?: return
        userPhotos = userPhotos - recipeId
        saveMap("userPhotos", userPhotos)
        File(old).delete()
        publishRecipes()
    }

    fun hasUserPhoto(recipeId: String): Boolean = userPhotos.containsKey(recipeId)

    /** Scarica il catalogo online e lo salva in locale. Restituisce il numero di ricette scaricate. */
    suspend fun syncRemote(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val url = settings.value.remoteUrl.trim()
            require(url.startsWith("http://") || url.startsWith("https://")) { "Indirizzo non valido" }
            val conn = java.net.URI(url).toURL().openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 20_000
            val text = try {
                if (conn.responseCode != 200) error("Il server ha risposto ${conn.responseCode}")
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val parsed = parseRecipes(text)
            require(parsed.isNotEmpty()) { "Il file non contiene ricette valide" }
            File(ctx.filesDir, "recipes_remote.json").writeText(text)
            val now = System.currentTimeMillis()
            prefs.edit().putLong("lastSync", now).apply()
            settings.update { it.copy(lastSync = now) }
            reloadRecipes()
            parsed.size
        }
    }

    fun toggleFavorite(id: String) {
        val updated = favorites.value.let { if (id in it) it - id else it + id }
        favorites.value = updated
        prefs.edit().putStringSet("favorites", updated).apply()
    }

    // ------------------------------------------------------------ impostazioni

    fun setServings(n: Int) {
        val v = n.coerceIn(1, 12)
        prefs.edit().putInt("servings", v).apply()
        settings.update { it.copy(servings = v) }
    }

    fun setRemoteUrl(url: String) {
        prefs.edit().putString("remoteUrl", url.trim()).apply()
        settings.update { it.copy(remoteUrl = url.trim()) }
    }

    // ---------------------------------------------------------------- dispensa

    fun guessPlace(name: String): Place? = aisleIndex[keyOf(name)]?.place

    fun addPantry(name: String, qty: Double?, unit: String, place: Place? = null, aisle: Aisle? = null) {
        val clean = name.trim().lowercase()
        if (clean.isEmpty()) return
        val k = keyOf(clean)
        val ai = aisle ?: aisleIndex[k] ?: Aisle.ALTRO
        val pl = place ?: ai.place
        val u = if (qty == null) "" else unit
        pantry.update { list ->
            val idx = list.indexOfFirst { keyOf(it.name) == k && it.unit == u && it.place == pl }
            if (idx >= 0) {
                val ex = list[idx]
                list.toMutableList().also { it[idx] = ex.copy(qty = addQty(ex.qty, qty)) }
            } else {
                list + PantryItem(newId(), clean, qty, u, pl, ai)
            }
        }
        savePantry()
    }

    fun updatePantry(item: PantryItem) {
        pantry.update { list -> list.map { if (it.id == item.id) item else it } }
        savePantry()
    }

    fun removePantry(id: String) {
        pantry.update { list -> list.filter { it.id != id } }
        savePantry()
    }

    fun restorePantry(item: PantryItem) {
        pantry.update { it + item }
        savePantry()
    }

    /** Toglie una quantita' dalla dispensa (stessa unita' di misura). Con qty nulla rimuove una voce senza quantita'. */
    fun consumeFromPantry(name: String, qty: Double?, unit: String) {
        val k = keyOf(name)
        pantry.update { list ->
            if (qty == null) {
                val i = list.indexOfFirst { keyOf(it.name) == k && it.qty == null }
                if (i >= 0) list.filterIndexed { idx, _ -> idx != i } else list
            } else {
                var remaining: Double = qty
                val out = ArrayList<PantryItem>()
                for (p in list) {
                    val have = p.qty
                    if (remaining > 0.0001 && keyOf(p.name) == k && p.unit == unit && have != null) {
                        val left = have - remaining
                        if (left > 0.0001) {
                            out.add(p.copy(qty = left))
                            remaining = 0.0
                        } else {
                            remaining = -left
                        }
                    } else {
                        out.add(p)
                    }
                }
                out
            }
        }
        savePantry()
    }

    /** Quanto manca di un ingrediente rispetto a quello che c'e' in dispensa. */
    private fun gapAgainstPantry(name: String, need: Double?, unit: String): Gap {
        val k = keyOf(name)
        val have = pantry.value.filter { keyOf(it.name) == k }
        if (have.isEmpty()) return Gap(true, need)
        if (need == null) return Gap(false, null)
        if (have.any { it.qty == null }) return Gap(false, null)
        val same = have.filter { it.unit == unit }
        if (same.isEmpty()) return Gap(false, null)
        val sum = same.sumOf { it.qty ?: 0.0 }
        return if (sum >= need) Gap(false, null) else Gap(true, need - sum)
    }

    // ------------------------------------------------------------------- spesa

    fun addShopping(name: String, qty: Double?, unit: String, note: String = "") {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val ai = aisleIndex[keyOf(clean)] ?: Aisle.ALTRO
        mergeShopping(clean, qty, if (qty == null) "" else unit, ai, note)
        saveShopping()
    }

    private fun mergeShopping(name: String, qty: Double?, unit: String, aisle: Aisle, note: String) {
        val k = keyOf(name)
        shopping.update { list ->
            val idx = list.indexOfFirst { keyOf(it.name) == k && it.unit == unit }
            if (idx >= 0) {
                val ex = list[idx]
                val notes = (ex.note.split(", ") + note.split(", "))
                    .filter { it.isNotBlank() }.distinct().joinToString(", ")
                list.toMutableList().also {
                    it[idx] = ex.copy(qty = addQty(ex.qty, qty), note = notes)
                }
            } else {
                list + ShoppingItem(newId(), name.trim().lowercase(), qty, unit, aisle, note)
            }
        }
    }

    fun updateShopping(item: ShoppingItem) {
        shopping.update { list -> list.map { if (it.id == item.id) item else it } }
        saveShopping()
    }

    fun removeShopping(id: String) {
        shopping.update { list -> list.filter { it.id != id } }
        saveShopping()
    }

    fun clearShopping() {
        shopping.value = emptyList()
        saveShopping()
    }

    /** Segna come comprato: l'ingrediente passa dalla lista alla dispensa/frigo/freezer. */
    fun buy(id: String): ShoppingItem? {
        val item = shopping.value.firstOrNull { it.id == id } ?: return null
        shopping.update { list -> list.filter { it.id != id } }
        saveShopping()
        addPantry(item.name, item.qty, item.unit, item.aisle.place, item.aisle)
        return item
    }

    fun undoBuy(item: ShoppingItem) {
        consumeFromPantry(item.name, item.qty, item.unit)
        shopping.update { it + item }
        saveShopping()
    }

    fun buyAll(): Int {
        val items = shopping.value
        items.forEach { addPantry(it.name, it.qty, it.unit, it.aisle.place, it.aisle) }
        shopping.value = emptyList()
        saveShopping()
        return items.size
    }

    private fun needsOf(recipe: Recipe, servings: Int): List<Need> {
        val f = servings.toDouble() / recipe.servings
        return recipe.ingredients.filter { !it.staple }
            .map { Need(it.name, scaleQty(it.qty, it.unit, f), it.unit, it.aisle) }
    }

    /** Aggiunge alla lista della spesa solo cio' che manca (tenendo conto di dispensa e lista). */
    private fun addNeeds(needs: List<Need>, note: String): Int {
        val agg = LinkedHashMap<String, Need>()
        for (n in needs) {
            val key = keyOf(n.name) + "|" + n.unit
            val ex = agg[key]
            agg[key] = if (ex == null) n else ex.copy(qty = addQty(ex.qty, n.qty))
        }
        var added = 0
        for (n in agg.values) {
            val gap = gapAgainstPantry(n.name, n.qty, n.unit)
            if (!gap.missing) continue
            var q: Double? = gap.qty
            val k = keyOf(n.name)
            val onList = shopping.value.filter { keyOf(it.name) == k && it.unit == n.unit }
            if (onList.isNotEmpty()) {
                val target = q
                if (target == null || onList.any { it.qty == null }) continue
                val already = onList.sumOf { it.qty ?: 0.0 }
                if (already >= target) continue
                q = target - already
            }
            mergeShopping(n.name, q, n.unit, n.aisle, note)
            added++
        }
        saveShopping()
        return added
    }

    fun addRecipeToShopping(recipe: Recipe, servings: Int): Int =
        addNeeds(needsOf(recipe, servings), recipe.title)

    fun shoppingFromPlan(weekStart: LocalDate): Int {
        val today = LocalDate.now()
        val from = if (weekStart.isBefore(today)) today else weekStart
        val s = from.toString()
        val e = weekStart.plusDays(6).toString()
        val byId = recipes.value.associateBy { it.id }
        val needs = ArrayList<Need>()
        for (entry in plan.value) {
            if (entry.date < s || entry.date > e) continue
            val r = byId[entry.recipeId] ?: continue
            needs.addAll(needsOf(r, entry.servings))
        }
        return addNeeds(needs, "Menù della settimana")
    }

    /** "Ho cucinato": scala dalla dispensa gli ingredienti usati. */
    fun cook(recipe: Recipe, servings: Int) {
        for (n in needsOf(recipe, servings)) {
            if (n.qty != null) consumeFromPantry(n.name, n.qty, n.unit)
        }
    }

    // -------------------------------------------------------------------- menu'

    fun addPlan(date: LocalDate, meal: Meal, recipeId: String, servings: Int) {
        plan.update { it + PlanEntry(newId(), date.toString(), meal, recipeId, servings) }
        savePlan()
    }

    /** Aggiunge al menu' un piatto libero, che non e' tra le ricette. */
    fun addCustomPlan(date: LocalDate, meal: Meal, title: String) {
        val t = title.trim()
        if (t.isEmpty()) return
        plan.update { it + PlanEntry(newId(), date.toString(), meal, "", settings.value.servings, t.cap()) }
        savePlan()
    }

    /** Piatti liberi gia' usati, i piu' recenti per primi: per riproporli con un tocco. */
    fun customDishes(): List<String> =
        plan.value.filter { it.custom.isNotBlank() }
            .sortedByDescending { it.date }
            .map { it.custom }
            .distinctBy { it.lowercase() }

    fun removePlan(id: String) {
        plan.update { list -> list.filter { it.id != id } }
        savePlan()
    }

    fun clearWeek(weekStart: LocalDate) {
        val s = weekStart.toString()
        val e = weekStart.plusDays(6).toString()
        plan.update { list -> list.filter { it.date < s || it.date > e } }
        savePlan()
    }

    /** Riempie i pasti ancora vuoti della settimana con una proposta variata. */
    fun generateWeek(weekStart: LocalDate, maxLevel: Int, preferPantry: Boolean) {
        val all = recipes.value.filter { it.difficulty.level <= maxLevel }
        if (all.isEmpty()) return
        val primi = all.filter { it.course == Course.PRIMO }
        val secondi = all.filter { it.course == Course.SECONDO }
        val unici = all.filter { it.course == Course.UNICO }
        val contorni = all.filter { it.course == Course.CONTORNO }
        val pantryKeys = pantry.value.map { keyOf(it.name) }.toSet()
        val today = LocalDate.now()
        val servings = settings.value.servings
        val used = plan.value.map { it.recipeId }.toMutableSet()
        val added = ArrayList<PlanEntry>()

        val month = today.monthValue
        fun pick(list: List<Recipe>): Recipe? {
            if (list.isEmpty()) return null
            // Meglio i piatti di stagione: quelli fuori stagione solo se non c'e' altro.
            val ok = list.filter { Season.status(it, month) != SeasonStatus.OUT }
            val pool = if (ok.isEmpty()) list else ok
            var cand = pool.filter { it.id !in used }
            if (cand.isEmpty()) cand = pool
            cand = cand + cand.filter { Season.status(it, month) == SeasonStatus.IN }
            if (preferPantry) {
                cand = cand.sortedByDescending { r ->
                    r.ingredients.count { !it.staple && keyOf(it.name) in pantryKeys }
                }.take(5)
            }
            val r = cand.random()
            used.add(r.id)
            return r
        }

        for (d in 0..6) {
            val date = weekStart.plusDays(d.toLong())
            if (date.isBefore(today)) continue
            for (meal in Meal.entries) {
                val occupied = plan.value.any { it.date == date.toString() && it.meal == meal }
                if (occupied) continue
                val chosen = ArrayList<Recipe>()
                val roll = Random.nextInt(100)
                if (roll < 40) {
                    pick(primi)?.let { chosen.add(it) }
                } else if (roll < 75) {
                    pick(secondi)?.let { chosen.add(it) }
                    if (chosen.isNotEmpty()) pick(contorni)?.let { chosen.add(it) }
                } else {
                    pick(unici)?.let { chosen.add(it) }
                }
                if (chosen.isEmpty()) pick(all)?.let { chosen.add(it) }
                for (r in chosen) {
                    added.add(PlanEntry(newId(), date.toString(), meal, r.id, servings))
                }
            }
        }
        plan.update { it + added }
        savePlan()
    }

    // ------------------------------------------------------------- persistenza

    private fun <T> readList(name: String, decode: (JSONObject) -> T): List<T> {
        val f = File(ctx.filesDir, name)
        if (!f.exists()) return emptyList()
        return runCatching { JSONArray(f.readText()).objects().map(decode) }.getOrDefault(emptyList())
    }

    private fun writeJson(name: String, array: JSONArray) {
        val text = array.toString()
        writer.execute {
            runCatching {
                val tmp = File(ctx.filesDir, "$name.tmp")
                tmp.writeText(text)
                tmp.renameTo(File(ctx.filesDir, name))
            }
        }
    }

    private fun savePantry() {
        val arr = JSONArray()
        pantry.value.forEach { arr.put(it.toJson()) }
        writeJson("pantry.json", arr)
    }

    private fun saveShopping() {
        val arr = JSONArray()
        shopping.value.forEach { arr.put(it.toJson()) }
        writeJson("shopping.json", arr)
    }

    private fun savePlan() {
        val arr = JSONArray()
        plan.value.forEach { arr.put(it.toJson()) }
        writeJson("plan.json", arr)
        publishMenuSnapshot()
    }

    /** Copia leggera del menu' (da ieri in poi), letta dal widget senza avviare l'app. */
    private fun publishMenuSnapshot() {
        val titles = recipes.value.associate { it.id to it.title }
        val from = LocalDate.now().minusDays(1).toString()
        val arr = JSONArray()
        for (e in plan.value.filter { it.date >= from }.sortedBy { it.date }) {
            val t = if (e.custom.isNotBlank()) e.custom else titles[e.recipeId]
            if (t == null) continue
            arr.put(JSONObject().put("d", e.date).put("m", e.meal.name).put("t", t))
        }
        prefs.edit().putString("widgetMenu", arr.toString()).apply()
        onMenuChanged()
    }
}
