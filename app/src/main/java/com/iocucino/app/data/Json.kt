package com.iocucino.app.data

import org.json.JSONArray
import org.json.JSONObject

fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

private fun JSONObject.doubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) optDouble(key) else null

private fun JSONObject.putQty(qty: Double?): JSONObject {
    if (qty != null) put("qty", qty)
    return this
}

// ---------- ricette (catalogo) ----------

fun parseRecipes(text: String): List<Recipe> {
    val arr = JSONObject(text).getJSONArray("recipes")
    return arr.objects().mapNotNull { o -> runCatching { parseRecipe(o) }.getOrNull() }
}

private fun parseRecipe(o: JSONObject): Recipe {
    val ingredients = o.getJSONArray("ingredients").objects().map { i ->
        Ingredient(
            name = i.getString("name").trim().lowercase(),
            qty = i.doubleOrNull("qty"),
            unit = i.optString("unit", ""),
            aisle = Aisle.from(i.optString("aisle", "")),
            staple = i.optBoolean("staple", false)
        )
    }
    val tags = o.optJSONArray("tags")?.strings() ?: emptyList()
    val steps = o.getJSONArray("steps").strings()
    require(ingredients.isNotEmpty() && steps.isNotEmpty())
    return Recipe(
        id = o.getString("id"),
        title = o.getString("title"),
        course = Course.from(o.optString("course", "")),
        difficulty = Difficulty.from(o.optString("difficulty", "")),
        minutes = o.optInt("minutes", 30),
        servings = o.optInt("servings", 4).coerceAtLeast(1),
        emoji = o.optString("emoji", "🍽️"),
        imageUrl = o.optString("image", ""),
        tags = tags,
        ingredients = ingredients,
        steps = steps,
        note = o.optString("note", ""),
        wiki = o.optString("wiki", ""),
        months = o.optJSONArray("months")?.let { a ->
            (0 until a.length()).map { a.optInt(it) }.filter { it in 1..12 }
        } ?: emptyList()
    )
}

// ---------- dati dell'utente ----------

fun PantryItem.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("unit", unit)
    .put("place", place.name).put("aisle", aisle.name).putQty(qty)

fun JSONObject.toPantryItem() = PantryItem(
    id = getString("id"),
    name = getString("name"),
    qty = doubleOrNull("qty"),
    unit = optString("unit", ""),
    place = Place.from(optString("place", "")),
    aisle = Aisle.from(optString("aisle", ""))
)

fun ShoppingItem.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("unit", unit)
    .put("aisle", aisle.name).put("note", note).putQty(qty)

fun JSONObject.toShoppingItem() = ShoppingItem(
    id = getString("id"),
    name = getString("name"),
    qty = doubleOrNull("qty"),
    unit = optString("unit", ""),
    aisle = Aisle.from(optString("aisle", "")),
    note = optString("note", "")
)

fun PlanEntry.toJson(): JSONObject = JSONObject()
    .put("id", id).put("date", date).put("meal", meal.name)
    .put("recipeId", recipeId).put("servings", servings).put("custom", custom)

fun JSONObject.toPlanEntry() = PlanEntry(
    id = getString("id"),
    date = getString("date"),
    meal = Meal.from(optString("meal", "")),
    recipeId = optString("recipeId", ""),
    servings = optInt("servings", 4),
    custom = optString("custom", "")
)
