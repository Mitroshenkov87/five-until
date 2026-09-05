package com.fiveuntil.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class EventStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): MutableList<Event> {
        ensureDemo()
        val raw = prefs.getString(KEY_EVENTS, null) ?: return mutableListOf()
        val arr = JSONArray(raw)
        val list = mutableListOf<Event>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Event(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    atMillis = o.getLong("at"),
                    reminder = ReminderType.valueOf(o.optString("reminder", ReminderType.NONE.name)),
                    happenedAtMillis = if (o.has("happenedAt") && !o.isNull("happenedAt")) {
                        o.getLong("happenedAt")
                    } else null
                )
            )
        }
        return list
    }

    fun save(events: List<Event>) {
        val arr = JSONArray()
        for (e in events.take(Event.MAX_SLOTS)) {
            arr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("title", e.title)
                    put("at", e.atMillis)
                    put("reminder", e.reminder.name)
                    if (e.happenedAtMillis != null) put("happenedAt", e.happenedAtMillis)
                    else put("happenedAt", JSONObject.NULL)
                }
            )
        }
        prefs.edit().putString(KEY_EVENTS, arr.toString()).apply()
    }

    fun nextId(): Long {
        val id = prefs.getLong(KEY_NEXT_ID, 1L)
        prefs.edit().putLong(KEY_NEXT_ID, id + 1L).apply()
        return id
    }

    private fun ensureDemo() {
        if (prefs.getBoolean(KEY_DEMO_DONE, false)) return
        if (prefs.contains(KEY_EVENTS)) {
            prefs.edit().putBoolean(KEY_DEMO_DONE, true).apply()
            return
        }
        val cal = Calendar.getInstance().apply {
            set(2071, Calendar.JUNE, 28, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val demo = Event(
            id = nextId(),
            title = "Илон Маск 100 лет",
            atMillis = cal.timeInMillis,
            reminder = ReminderType.NONE
        )
        save(listOf(demo))
        prefs.edit().putBoolean(KEY_DEMO_DONE, true).apply()
    }

    companion object {
        private const val PREFS = "five_until"
        private const val KEY_EVENTS = "events"
        private const val KEY_NEXT_ID = "next_id"
        private const val KEY_DEMO_DONE = "demo_done"
    }
}
