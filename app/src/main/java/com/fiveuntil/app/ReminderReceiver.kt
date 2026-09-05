package com.fiveuntil.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val store = EventStore(context)
                ReminderScheduler.rescheduleAll(context, store.load())
            }
            ACTION_FIRE -> {
                val id = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
                val reminderName = intent.getStringExtra(EXTRA_REMINDER) ?: ReminderType.NOTIFICATION.name
                val reminder = try {
                    ReminderType.valueOf(reminderName)
                } catch (_: Exception) {
                    ReminderType.NOTIFICATION
                }
                val store = EventStore(context)
                val events = store.load()
                val event = events.find { it.id == id }
                val title = event?.title ?: context.getString(R.string.app_name)
                when (reminder) {
                    ReminderType.NONE -> {}
                    ReminderType.NOTIFICATION -> showNotification(context, id.toInt(), title, false)
                    ReminderType.NOTIFICATION_SOUND -> showNotification(context, id.toInt(), title, true)
                    ReminderType.MELODY -> MelodyPlayer.playOnce()
                }
            }
        }
    }

    private fun showNotification(context: Context, notifId: Int, title: String, withSound: Boolean) {
        ensureChannel(context, withSound)
        val open = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = if (withSound) CHANNEL_SOUND else CHANNEL_SILENT
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.happened))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (!withSound) {
            builder.setSilent(true)
        }
        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    private fun ensureChannel(context: Context, withSound: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val silent = NotificationChannel(
            CHANNEL_SILENT,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            setSound(null, null)
        }
        val sound = NotificationChannel(
            CHANNEL_SOUND,
            context.getString(R.string.notification_channel_name) + " +",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
        }
        nm.createNotificationChannel(silent)
        nm.createNotificationChannel(sound)
    }

    companion object {
        const val ACTION_FIRE = "com.fiveuntil.app.REMINDER_FIRE"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_REMINDER = "reminder"
        private const val CHANNEL_SILENT = "events_silent"
        private const val CHANNEL_SOUND = "events_sound"
    }
}
