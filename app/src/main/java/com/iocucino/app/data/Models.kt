package com.iocucino.app.data

/** Tipo di piatto. */
enum class Course(val label: String, val plural: String, val emoji: String) {
    PRIMO("Primo", "Primi", "🍝"),
    SECONDO("Secondo", "Secondi", "🍗"),
    UNICO("Piatto unico", "Piatti unici", "🥧"),
    CONTORNO("Contorno", "Contorni", "🥗");

    companion object {
        fun from(s: String?): Course =
            entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: UNICO
    }
}

/** Livello di impegno della ricetta. */
enum class Difficulty(val label: String, val plural: String, val level: Int) {
    SEMPLICE("Semplice", "Semplici", 1),
    MEDIA("Media", "Medie", 2),
    IMPEGNATIVA("Impegnativa", "Impegnative", 3);

    companion object {
        fun from(s: String?): Difficulty =
            entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: SEMPLICE
    }
}

enum class Meal(val label: String) {
    PRANZO("Pranzo"),
    CENA("Cena");

    companion object {
        fun from(s: String?): Meal =
            entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: CENA
    }
}

/** Dove si conserva un ingrediente in casa. */
enum class Place(val label: String, val emoji: String) {
    DISPENSA("Dispensa", "🗄️"),
    FRIGO("Frigo", "🧊"),
    FREEZER("Freezer", "❄️");

    companion object {
        fun from(s: String?): Place =
            entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: DISPENSA
    }
}

/** Reparto del supermercato (raggruppa la lista della spesa e suggerisce dove conservare). */
enum class Aisle(val label: String, val place: Place, val emoji: String) {
    VERDURA("Frutta e verdura", Place.FRIGO, "🥕"),
    CARNE("Carne e salumi", Place.FRIGO, "🥩"),
    PESCE("Pesce", Place.FRIGO, "🐟"),
    LATTICINI("Latticini e uova", Place.FRIGO, "🧀"),
    PASTA("Pasta, pane e farine", Place.DISPENSA, "🍞"),
    DISPENSA("Dispensa e scatolame", Place.DISPENSA, "🥫"),
    SURGELATI("Surgelati", Place.FREEZER, "❄️"),
    CONDIMENTI("Condimenti e spezie", Place.DISPENSA, "🧂"),
    ALTRO("Altro", Place.DISPENSA, "🛒");

    companion object {
        fun from(s: String?): Aisle =
            entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: ALTRO
    }
}

data class Ingredient(
    val name: String,
    val qty: Double?,
    val unit: String,
    val aisle: Aisle,
    /** Ingredienti "sempre in casa" (sale, olio, pepe, acqua): non finiscono nella lista della spesa. */
    val staple: Boolean
)

data class Recipe(
    val id: String,
    val title: String,
    val course: Course,
    val difficulty: Difficulty,
    val minutes: Int,
    val servings: Int,
    val emoji: String,
    val imageUrl: String,
    val tags: List<String>,
    val ingredients: List<Ingredient>,
    val steps: List<String>,
    val note: String,
    /** Parole da cercare su Wikipedia per trovare una foto del piatto. */
    val wiki: String = "",
    /** Chi ha fornito la foto (vuoto se e' quella dell'utente o non c'e'). */
    val photoCredit: String = "",
    /** Mesi (1-12) in cui la ricetta e' di stagione, se il catalogo li indica. Vuoto = calcolati dagli ingredienti. */
    val months: List<Int> = emptyList()
)

data class PantryItem(
    val id: String,
    val name: String,
    val qty: Double?,
    val unit: String,
    val place: Place,
    val aisle: Aisle
)

data class ShoppingItem(
    val id: String,
    val name: String,
    val qty: Double?,
    val unit: String,
    val aisle: Aisle,
    val note: String
)

data class PlanEntry(
    val id: String,
    /** Data in formato ISO (aaaa-mm-gg). */
    val date: String,
    val meal: Meal,
    /** Vuoto se il piatto e' libero (scritto dall'utente, senza ricetta). */
    val recipeId: String,
    val servings: Int,
    /** Nome del piatto libero, ad esempio "Pizza fatta in casa". */
    val custom: String = ""
)

data class AppSettings(
    val servings: Int = 4,
    val remoteUrl: String = "",
    val lastSync: Long = 0L
)

data class Need(val name: String, val qty: Double?, val unit: String, val aisle: Aisle)

data class Gap(val missing: Boolean, val qty: Double?)
