package com.fiveuntil.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    fun rescheduleAll(context: Context, events: List<Event>) {
        for (e in events) {
            cancel(context, e.id)
            if (e.reminder == ReminderType.NONE) continue
            if (e.happenedAtMillis != null) continue
            if (e.atMillis <= System.currentTimeMillis()) continue
            schedule(context, e)
        }
    }

    fun schedule(context: Context, event: Event) {
        if (event.reminder == ReminderType.NONE) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, event.id, event.reminder)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, event.atMillis, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, event.atMillis, pi)
            }
        } catch (_: SecurityException) {
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, event.atMillis, pi)
            } catch (_: Exception) {
            }
        }
    }

    fun cancel(context: Context, eventId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (r in ReminderType.entries) {
            am.cancel(pendingIntent(context, eventId, r))
        }
    }

    private fun pendingIntent(
        context: Context,
        eventId: Long,
        reminder: ReminderType
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(ReminderReceiver.EXTRA_REMINDER, reminder.name)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, eventId.toInt(), intent, flags)
    }
}
