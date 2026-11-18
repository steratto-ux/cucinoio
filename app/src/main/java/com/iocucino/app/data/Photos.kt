package com.iocucino.app.data

import java.net.HttpURLConnection
import java.net.URLEncoder

/** Trova su Wikipedia (in italiano) una foto adatta a un piatto. */
object WikiPhotos {
    const val USER_AGENT = "IoCucino/0.2 (app personale; https://github.com/steratto-ux)"

    private val ignored = setOf("dell", "dall", "nell", "alle", "dalla")

    private fun words(s: String): Set<String> =
        keyOf(s).split(" ").filter { it.length >= 4 && it !in ignored }.toSet()

    /** Restituisce l'indirizzo di una foto (500 px) o null se non c'e' nulla di pertinente. */
    fun lookup(query: String): String? {
        val q = URLEncoder.encode(query, "UTF-8")
        val url = "https://it.wikipedia.org/w/api.php?action=query&format=json&formatversion=2" +
            "&generator=search&gsrsearch=$q&gsrlimit=6&prop=pageimages&piprop=thumbnail" +
            "&pithumbsize=500&pilimit=6&redirects=1"
        val conn = java.net.URI(url).toURL().openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", USER_AGENT)
        val text = try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        return pick(text, query)
    }

    /** Sceglie la prima pagina pertinente che ha una foto. */
    fun pick(text: String, query: String): String? {
        val pages = org.json.JSONObject(text).optJSONObject("query")?.optJSONArray("pages") ?: return null
        val wanted = words(query)
        val candidates = pages.objects().sortedBy { it.optInt("index", 99) }
        for (p in candidates) {
            val thumb = p.optJSONObject("thumbnail") ?: continue
            val src = thumb.optString("source", "")
            if (src.isBlank()) continue
            val titleWords = words(p.optString("title", ""))
            if (wanted.any { it in titleWords }) return src
        }
        return null
    }
}
