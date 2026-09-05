package com.fiveuntil.app

enum class ReminderType {
    NONE,
    NOTIFICATION,
    NOTIFICATION_SOUND,
    MELODY
}

data class Event(
    val id: Long,
    var title: String,
    var atMillis: Long,
    var reminder: ReminderType = ReminderType.NONE,
    var happenedAtMillis: Long? = null
) {
    fun isHappened(now: Long = System.currentTimeMillis()): Boolean {
        val marked = happenedAtMillis
        return marked != null || atMillis <= now
    }

    fun shouldAutoClear(now: Long = System.currentTimeMillis()): Boolean {
        val marked = happenedAtMillis ?: return false
        return now - marked >= AUTO_CLEAR_MS
    }

    companion object {
        const val AUTO_CLEAR_MS = 60L * 60L * 1000L
        const val MAX_SLOTS = 5
    }
}
