package com.fiveuntil.app

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var store: EventStore
    private lateinit var slotsContainer: LinearLayout
    private val events = mutableListOf<Event>()
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            processDueAndExpiry()
            refreshUi()
            handler.postDelayed(this, 15_000L)
        }
    }

    private val colorBg = 0xFF000000.toInt()
    private val colorSlot = 0xFF141414.toInt()
    private val colorText = 0xFFEEEEEE.toInt()
    private val colorMuted = 0xFF888888.toInt()
    private val colorDim = 0xFF555555.toInt()
    private val colorHappened = 0xFF666666.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = EventStore(this)
        events.clear()
        events.addAll(store.load())
        processDueAndExpiry()

        slotsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }

        val header = TextView(this).apply {
            text = getString(R.string.app_name)
            setTextColor(colorMuted)
            textSize = 14f
            letterSpacing = 0.12f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(16))
        }
        slotsContainer.addView(header)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(colorBg)
            isFillViewport = true
            addView(slotsContainer)
        }
        setContentView(scroll)
        window.statusBarColor = colorBg
        window.navigationBarColor = colorBg

        requestNeededPermissions()
        ReminderScheduler.rescheduleAll(this, events)
        rebuildSlots()
    }

    override fun onResume() {
        super.onResume()
        processDueAndExpiry()
        refreshUi()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    private fun requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                // Optional: user can grant in settings; in-app due still works.
            }
        }
    }

    private fun processDueAndExpiry() {
        val now = System.currentTimeMillis()
        var changed = false
        val iter = events.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            if (e.happenedAtMillis == null && e.atMillis <= now) {
                e.happenedAtMillis = now
                changed = true
                MelodyPlayer.stop()
            }
            if (e.shouldAutoClear(now)) {
                ReminderScheduler.cancel(this, e.id)
                iter.remove()
                changed = true
            }
        }
        if (changed) {
            persistAndReschedule()
        }
    }

    private fun persistAndReschedule() {
        // Pack: list order is already packed (no holes).
        while (events.size > Event.MAX_SLOTS) {
            val removed = events.removeAt(events.lastIndex)
            ReminderScheduler.cancel(this, removed.id)
        }
        store.save(events)
        ReminderScheduler.rescheduleAll(this, events)
    }

    private fun rebuildSlots() {
        // Keep header (index 0); remove slot views
        while (slotsContainer.childCount > 1) {
            slotsContainer.removeViewAt(1)
        }
        for (i in 0 until Event.MAX_SLOTS) {
            val slot = createSlotView(i)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            slotsContainer.addView(slot, lp)
        }
        refreshUi()
    }

    private fun createSlotView(index: Int): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorSlot)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            tag = "slot_$index"
        }

        val title = TextView(this).apply {
            tag = "title"
            setTextColor(colorText)
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
        }
        val countdown = TextView(this).apply {
            tag = "countdown"
            setTextColor(colorMuted)
            textSize = 15f
            setPadding(0, dp(6), 0, 0)
        }
        val reminder = TextView(this).apply {
            tag = "reminder"
            setTextColor(colorDim)
            textSize = 12f
            setPadding(0, dp(8), 0, 0)
        }
        val dismiss = TextView(this).apply {
            tag = "dismiss"
            text = getString(R.string.dismiss)
            setTextColor(colorMuted)
            textSize = 13f
            setPadding(0, dp(10), 0, 0)
            visibility = View.GONE
        }

        box.addView(title)
        box.addView(countdown)
        box.addView(reminder)
        box.addView(dismiss)

        box.setOnClickListener {
            val event = events.getOrNull(index)
            if (event == null) {
                startAddFlow(index)
            }
        }
        title.setOnClickListener {
            val event = events.getOrNull(index) ?: return@setOnClickListener
            if (event.happenedAtMillis != null) return@setOnClickListener
            editTitle(event)
        }
        countdown.setOnClickListener {
            val event = events.getOrNull(index) ?: return@setOnClickListener
            if (event.happenedAtMillis != null) return@setOnClickListener
            editDateTime(event)
        }
        reminder.setOnClickListener {
            val event = events.getOrNull(index) ?: return@setOnClickListener
            if (event.happenedAtMillis != null) return@setOnClickListener
            editReminder(event)
        }
        dismiss.setOnClickListener {
            val event = events.getOrNull(index) ?: return@setOnClickListener
            ReminderScheduler.cancel(this, event.id)
            events.remove(event)
            persistAndReschedule()
            refreshUi()
        }
        return box
    }

    private fun refreshUi() {
        for (i in 0 until Event.MAX_SLOTS) {
            val box = slotsContainer.findViewWithTag<LinearLayout>("slot_$i") ?: continue
            val title = box.findViewWithTag<TextView>("title")
            val countdown = box.findViewWithTag<TextView>("countdown")
            val reminder = box.findViewWithTag<TextView>("reminder")
            val dismiss = box.findViewWithTag<TextView>("dismiss")
            val event = events.getOrNull(i)
            if (event == null) {
                title.text = getString(R.string.tap_to_add)
                title.setTextColor(colorDim)
                countdown.text = ""
                reminder.text = ""
                dismiss.visibility = View.GONE
                box.alpha = 0.85f
            } else {
                val happened = event.happenedAtMillis != null
                title.text = event.title
                title.setTextColor(if (happened) colorHappened else colorText)
                if (happened) {
                    countdown.text = getString(R.string.happened)
                    countdown.setTextColor(colorHappened)
                    reminder.text = ""
                    dismiss.visibility = View.VISIBLE
                    box.alpha = 0.7f
                } else {
                    countdown.text = RemainingFormatter.format(this, event.atMillis)
                    countdown.setTextColor(colorMuted)
                    reminder.text = reminderLabel(event.reminder)
                    reminder.setTextColor(colorDim)
                    dismiss.visibility = View.GONE
                    box.alpha = 1f
                }
            }
        }
    }

    private fun reminderLabel(type: ReminderType): String {
        val name = when (type) {
            ReminderType.NONE -> getString(R.string.reminder_none)
            ReminderType.NOTIFICATION -> getString(R.string.reminder_notification)
            ReminderType.NOTIFICATION_SOUND -> getString(R.string.reminder_notification_sound)
            ReminderType.MELODY -> getString(R.string.reminder_melody)
        }
        return "${getString(R.string.reminder)}: $name"
    }

    private fun startAddFlow(slotIndex: Int) {
        // Only allow filling when there is room; new events pack to the end (top-packed list).
        if (events.size >= Event.MAX_SLOTS) return
        // If user tapped a lower empty slot, we still append (packs up automatically).
        promptTitle(null) { title ->
            pickDateTime(System.currentTimeMillis() + 3600_000L) { millis ->
                pickReminder(ReminderType.NONE) { rem ->
                    val event = Event(
                        id = store.nextId(),
                        title = title,
                        atMillis = millis,
                        reminder = rem
                    )
                    // Insert at first empty = append; packing keeps filled at top.
                    events.add(event)
                    persistAndReschedule()
                    maybePromptExactAlarm()
                    refreshUi()
                }
            }
        }
    }

    private fun editTitle(event: Event) {
        promptTitle(event.title) { title ->
            event.title = title
            persistAndReschedule()
            refreshUi()
        }
    }

    private fun editDateTime(event: Event) {
        pickDateTime(event.atMillis) { millis ->
            event.atMillis = millis
            event.happenedAtMillis = null
            persistAndReschedule()
            maybePromptExactAlarm()
            refreshUi()
        }
    }

    private fun editReminder(event: Event) {
        pickReminder(event.reminder) { rem ->
            event.reminder = rem
            persistAndReschedule()
            maybePromptExactAlarm()
            refreshUi()
        }
    }

    private fun promptTitle(initial: String?, onOk: (String) -> Unit) {
        val input = EditText(this).apply {
            setText(initial.orEmpty())
            setSelection(text.length)
            setTextColor(colorText)
            setHintTextColor(colorDim)
            hint = getString(R.string.title_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setBackgroundColor(0xFF222222.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(getString(R.string.edit_title))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val t = input.text.toString().trim()
                if (t.isNotEmpty()) onOk(t)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun pickDateTime(initialMillis: Long, onOk: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(
                    this,
                    { _, h, min ->
                        cal.set(Calendar.HOUR_OF_DAY, h)
                        cal.set(Calendar.MINUTE, min)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        onOk(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickReminder(current: ReminderType, onOk: (ReminderType) -> Unit) {
        val labels = arrayOf(
            getString(R.string.reminder_none),
            getString(R.string.reminder_notification),
            getString(R.string.reminder_notification_sound),
            getString(R.string.reminder_melody)
        )
        val values = arrayOf(
            ReminderType.NONE,
            ReminderType.NOTIFICATION,
            ReminderType.NOTIFICATION_SOUND,
            ReminderType.MELODY
        )
        val checked = values.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(getString(R.string.reminder))
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                onOk(values[which])
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun maybePromptExactAlarm() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        if (am.canScheduleExactAlarms()) return
        val needs = events.any { it.reminder != ReminderType.NONE && it.happenedAtMillis == null }
        if (!needs) return
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {
        }
    }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
}
