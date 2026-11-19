package com.iocucino.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONArray
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Widget "Menù di oggi". Legge la copia del menù salvata dall'app (SharedPreferences),
 * quindi funziona anche quando l'app e' chiusa. Si aggiorna a ogni modifica del menù,
 * ogni 30 minuti e poco dopo la mezzanotte.
 */
class MenuWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(context, manager, id)
        scheduleMidnight(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refreshAll(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.iocucino.app.WIDGET_REFRESH"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MenuWidgetProvider::class.java))
            for (id in ids) update(context, manager, id)
            if (ids.isNotEmpty()) scheduleMidnight(context)
        }

        private fun dishes(context: Context, date: LocalDate, meal: String): List<String> {
            val json = context.getSharedPreferences("io_cucino", Context.MODE_PRIVATE)
                .getString("widgetMenu", null) ?: return emptyList()
            val out = ArrayList<String>()
            runCatching {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    if (o.optString("d") == date.toString() && o.optString("m") == meal) {
                        out.add(o.optString("t"))
                    }
                }
            }
            return out
        }

        private fun text(list: List<String>): String =
            if (list.isEmpty()) "Non ancora scelto" else list.joinToString("\n") { "• $it" }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val today = LocalDate.now()
            val day = today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.ITALIAN)
                .replaceFirstChar { it.uppercase() }
            val month = today.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ITALIAN)
            val views = RemoteViews(context.packageName, R.layout.widget_menu)
            views.setTextViewText(R.id.w_title, "Oggi · $day ${today.dayOfMonth} $month")
            views.setTextViewText(R.id.w_lunch, text(dishes(context, today, "PRANZO")))
            views.setTextViewText(R.id.w_dinner, text(dishes(context, today, "CENA")))

            val open = Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_START, "planner")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pi = PendingIntent.getActivity(
                context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.w_root, pi)
            manager.updateAppWidget(id, views)
        }

        /** Un controllo poco dopo la mezzanotte, cosi' il widget cambia giorno in tempo. */
        private fun scheduleMidnight(context: Context) {
            val next = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli() + 60_000L
            val intent = Intent(context, MenuWidgetProvider::class.java).setAction(ACTION_REFRESH)
            val pi = PendingIntent.getBroadcast(
                context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarm.set(AlarmManager.RTC, next, pi)
        }
    }
}
