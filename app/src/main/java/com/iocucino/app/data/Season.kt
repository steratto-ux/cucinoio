package com.iocucino.app.data

import java.time.LocalDate

enum class SeasonStatus { IN, OUT, NEUTRAL }

/**
 * Calendario della frutta e verdura di stagione in Italia (periodi indicativi).
 * Una ricetta e' "di stagione" se i suoi ingredienti freschi presenti in questo elenco
 * sono tutti di stagione nel mese corrente; e' "fuori stagione" se almeno uno non lo e'.
 * Gli ingredienti che non compaiono qui (pasta, carne, patate, carote, cipolle...) non contano.
 * Un catalogo online puo' indicare i mesi direttamente con il campo "months".
 */
object Season {

    private fun range(from: Int, to: Int): Set<Int> =
        if (from <= to) (from..to).toSet() else ((from..12) + (1..to)).toSet()

    private class Entry(val display: String, val months: Set<Int>)

    private val byKey = HashMap<String, Entry>()
    private val entries = ArrayList<Entry>()

    private fun add(from: Int, to: Int, display: String, vararg names: String) {
        val e = Entry(display, range(from, to))
        entries.add(e)
        byKey[keyOf(display)] = e
        for (n in names) byKey[keyOf(n)] = e
    }

    init {
        // Verdura estiva
        add(5, 9, "zucchine", "zucchina")
        add(6, 9, "melanzane", "melanzana")
        add(6, 10, "peperoni", "peperone")
        add(6, 9, "pomodori", "pomodoro", "pomodorini", "pomodorino")
        add(6, 9, "fagiolini")
        add(6, 9, "cetrioli", "cetriolo")
        // Verdura autunnale e invernale
        add(10, 4, "spinaci")
        add(10, 3, "broccoli", "broccolo")
        add(10, 3, "cavolfiore", "cavolfiori")
        add(10, 3, "cavolo", "verza", "cavolo cappuccio")
        add(11, 2, "cavolo nero")
        add(10, 3, "finocchi", "finocchio")
        add(10, 1, "zucca")
        add(10, 3, "radicchio")
        add(10, 3, "porri", "porro")
        add(9, 11, "porcini", "funghi porcini")
        add(11, 3, "cime di rapa")
        add(10, 3, "barbabietola", "barbabietole")
        // Verdura primaverile
        add(3, 6, "asparagi", "asparago")
        add(2, 5, "carciofi", "carciofo")
        add(4, 6, "piselli freschi", "piselli")
        add(4, 5, "fave")
        add(3, 6, "ravanelli")
        // Frutta
        add(4, 6, "fragole", "fragola")
        add(5, 7, "ciliegie", "ciliegia")
        add(6, 9, "pesche", "pesca")
        add(6, 8, "albicocche", "albicocca")
        add(6, 8, "melone")
        add(6, 8, "anguria", "cocomero")
        add(8, 10, "uva")
        add(9, 3, "mele", "mela")
        add(9, 1, "pere", "pera")
        add(10, 12, "cachi", "caco")
        add(10, 12, "castagne", "castagna")
        add(11, 4, "arance", "arancia")
        add(11, 4, "mandarini", "clementine")
        add(11, 4, "kiwi")
    }

    /** I "piselli surgelati" e simili non sono freschi: non contano. */
    private fun isFresh(name: String): Boolean {
        val n = name.lowercase()
        return !(n.contains("surgel") || n.contains("scatola") || n.contains("sott") ||
            n.contains("passata") || n.contains("pelat") || n.contains("secch") || n.contains("conserva"))
    }

    fun status(recipe: Recipe, month: Int = LocalDate.now().monthValue): SeasonStatus {
        if (recipe.months.isNotEmpty()) {
            return if (month in recipe.months) SeasonStatus.IN else SeasonStatus.OUT
        }
        var known = 0
        for (i in recipe.ingredients) {
            if (i.staple || !isFresh(i.name)) continue
            val e = byKey[keyOf(i.name)] ?: continue
            known++
            if (month !in e.months) return SeasonStatus.OUT
        }
        return if (known > 0) SeasonStatus.IN else SeasonStatus.NEUTRAL
    }

    /** Frutta e verdura di stagione nel mese indicato, per mostrarla all'utente. */
    fun inSeason(month: Int = LocalDate.now().monthValue): List<String> =
        entries.filter { month in it.months }.map { it.display }

    fun monthName(month: Int): String = when (month) {
        1 -> "gennaio"; 2 -> "febbraio"; 3 -> "marzo"; 4 -> "aprile"
        5 -> "maggio"; 6 -> "giugno"; 7 -> "luglio"; 8 -> "agosto"
        9 -> "settembre"; 10 -> "ottobre"; 11 -> "novembre"; else -> "dicembre"
    }
}
