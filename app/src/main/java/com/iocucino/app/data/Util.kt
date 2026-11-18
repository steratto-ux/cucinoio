package com.iocucino.app.data

import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Chiave per confrontare i nomi degli ingredienti ignorando maiuscole, accenti
 * e la vocale finale (uovo/uova, patata/patate, zucchina/zucchine...).
 */
fun keyOf(name: String): String {
    val base = Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    return base.split(Regex("\\s+")).joinToString(" ") { w ->
        if (w.length > 3 && w.last() in "aeiou") w.dropLast(1) else w
    }
}

fun String.cap(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun fmtNum(v: Double): String {
    val r = Math.round(v * 10) / 10.0
    val whole = Math.floor(r).toLong()
    val frac = r - whole
    return when {
        frac < 0.05 -> whole.toString()
        Math.abs(frac - 0.5) < 0.05 -> if (whole == 0L) "½" else "$whole½"
        else -> r.toString().replace('.', ',')
    }
}

/** "500 g", "3", "q.b." */
fun quantityLabel(qty: Double?, unit: String): String = when {
    qty == null -> "q.b."
    unit == "pz" || unit.isBlank() -> fmtNum(qty)
    else -> fmtNum(qty) + " " + unit
}

/** Quantita' per la lista della spesa/dispensa: vuota se non e' nota. */
fun quantityOrEmpty(qty: Double?, unit: String): String =
    if (qty == null) "" else quantityLabel(qty, unit)

fun scaleQty(q: Double?, unit: String, factor: Double): Double? {
    if (q == null) return null
    val v = q * factor
    return when (unit) {
        "pz", "spicchi", "fette" ->
            if (q < 1.0) Math.max(0.5, Math.round(v * 2) / 2.0) else Math.max(1.0, Math.ceil(v - 0.01))
        "g", "ml" ->
            if (v >= 100) Math.round(v / 10.0) * 10.0 else Math.max(5.0, Math.round(v / 5.0) * 5.0)
        else -> Math.max(0.5, Math.round(v * 2) / 2.0)
    }
}

fun addQty(a: Double?, b: Double?): Double? =
    if (a == null && b == null) null else (a ?: 0.0) + (b ?: 0.0)

// ---------- date ----------

/** Lunedi' della settimana da mostrare: nel fine settimana propone la settimana successiva. */
fun defaultWeekStart(): LocalDate {
    val today = LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    return if (today.dayOfWeek.value >= 6) monday.plusWeeks(1) else monday
}

fun dayFull(d: LocalDate): String =
    d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ITALIAN).cap() + " " + d.dayOfMonth

fun dayShort(d: LocalDate): String =
    d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ITALIAN) + " " + d.dayOfMonth

private fun monthShort(d: LocalDate): String =
    d.month.getDisplayName(TextStyle.SHORT, Locale.ITALIAN)

fun weekLabel(start: LocalDate): String {
    val end = start.plusDays(6)
    return if (start.month == end.month) {
        "${start.dayOfMonth} – ${end.dayOfMonth} ${monthShort(end)}"
    } else {
        "${start.dayOfMonth} ${monthShort(start)} – ${end.dayOfMonth} ${monthShort(end)}"
    }
}

fun dayLabelRelative(d: LocalDate): String {
    val today = LocalDate.now()
    return when (d) {
        today -> "Oggi"
        today.plusDays(1) -> "Domani"
        else -> dayShort(d)
    }
}

fun formatDateTime(millis: Long): String =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ITALIAN)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(millis))
