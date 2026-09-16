package com.fiveuntil.app

/**
 * How the user wants to be alerted when [Event.atMillis] is reached.
 * NONE: in-app due-state only (grey «happened»).
 * NOTIFICATION / NOTIFICATION_SOUND: system notification via [ReminderReceiver].
 * MELODY: one-shot synthesized tone via [MelodyPlayer] (no loop).
 */
enum class ReminderType {
    NONE,
    NOTIFICATION,
    NOTIFICATION_SOUND,
    MELODY
}

/**
 * One countdown slot. The UI holds at most [MAX_SLOTS] events in a packed list
 * (filled slots at the top, empty slots below — see [MainActivity]).
 *
 * Due-state: when [atMillis] passes, [MainActivity.processDueAndExpiry] sets
 * [happenedAtMillis]. The slot then shows grey «happened» / localized equivalent
 * until the user dismisses it or [AUTO_CLEAR_MS] (~1 hour) elapses.
 */
data class Event(
    val id: Long,
    var title: String,
    var atMillis: Long,
    var reminder: ReminderType = ReminderType.NONE,
    /** Wall-clock when the event was marked due; null while still counting down. */
    var happenedAtMillis: Long? = null
) {
    fun isHappened(now: Long = System.currentTimeMillis()): Boolean {
        val marked = happenedAtMillis
        return marked != null || atMillis <= now
    }

    /** True once the grey «happened» slot should be removed automatically. */
    fun shouldAutoClear(now: Long = System.currentTimeMillis()): Boolean {
        val marked = happenedAtMillis ?: return false
        return now - marked >= AUTO_CLEAR_MS
    }

    companion object {
        /** ~1 hour after due before the slot auto-clears and others pack up. */
        const val AUTO_CLEAR_MS = 60L * 60L * 1000L
        const val MAX_SLOTS = 5
    }
}
