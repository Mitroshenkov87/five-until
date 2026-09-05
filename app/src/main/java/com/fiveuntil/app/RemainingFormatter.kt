package com.fiveuntil.app

import android.content.Context
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object RemainingFormatter {

    fun format(context: Context, targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        if (targetMillis <= nowMillis) return context.getString(R.string.happened)

        val lang = Locale.getDefault().language
        val diff = targetMillis - nowMillis
        val minutesTotal = TimeUnit.MILLISECONDS.toMinutes(diff).coerceAtLeast(0L)
        if (minutesTotal < 1L) return context.getString(R.string.less_than_minute)

        val years = yearsBetween(nowMillis, targetMillis)
        if (years > 0) {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            cal.add(Calendar.YEAR, years)
            val days = TimeUnit.MILLISECONDS.toDays(targetMillis - cal.timeInMillis).coerceAtLeast(0L)
            return join(
                unit(lang, years.toLong(), UnitKind.YEAR, context),
                if (days > 0) unit(lang, days, UnitKind.DAY, context) else ""
            )
        }

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        if (days >= 1L) {
            val afterDays = diff - TimeUnit.DAYS.toMillis(days)
            val hours = TimeUnit.MILLISECONDS.toHours(afterDays)
            return if (days >= 7L || hours == 0L) {
                unit(lang, days, UnitKind.DAY, context)
            } else {
                join(
                    unit(lang, days, UnitKind.DAY, context),
                    unit(lang, hours, UnitKind.HOUR, context)
                )
            }
        }

        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        if (hours >= 1L) {
            val afterHours = diff - TimeUnit.HOURS.toMillis(hours)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(afterHours)
            return if (minutes == 0L) {
                unit(lang, hours, UnitKind.HOUR, context)
            } else {
                join(
                    unit(lang, hours, UnitKind.HOUR, context),
                    unit(lang, minutes, UnitKind.MINUTE, context)
                )
            }
        }

        return unit(lang, minutesTotal, UnitKind.MINUTE, context)
    }

    private fun yearsBetween(from: Long, to: Long): Int {
        val a = Calendar.getInstance().apply { timeInMillis = from }
        val b = Calendar.getInstance().apply { timeInMillis = to }
        var years = b.get(Calendar.YEAR) - a.get(Calendar.YEAR)
        val probe = a.clone() as Calendar
        probe.add(Calendar.YEAR, years)
        if (probe.timeInMillis > to) years--
        return years.coerceAtLeast(0)
    }

    private fun join(a: String, b: String): String {
        if (b.isBlank()) return a
        if (a.isBlank()) return b
        return "$a $b"
    }

    private enum class UnitKind { YEAR, DAY, HOUR, MINUTE }

    private fun unit(lang: String, n: Long, kind: UnitKind, context: Context): String {
        if (n <= 0L) return ""
        return when (lang) {
            "ru", "uk", "be" -> slavic(lang, n, kind)
            else -> english(n, kind, context)
        }
    }

    private fun english(n: Long, kind: UnitKind, context: Context): String {
        val one = n == 1L
        return when (kind) {
            UnitKind.YEAR -> context.getString(if (one) R.string.year_one else R.string.year_many, n)
            UnitKind.DAY -> context.getString(if (one) R.string.day_one else R.string.day_many, n)
            UnitKind.HOUR -> context.getString(if (one) R.string.hour_one else R.string.hour_many, n)
            UnitKind.MINUTE -> context.getString(if (one) R.string.minute_one else R.string.minute_many, n)
        }
    }

    private fun slavic(lang: String, n: Long, kind: UnitKind): String {
        val forms = when (lang) {
            "uk" -> when (kind) {
                UnitKind.YEAR -> Triple("рік", "роки", "років")
                UnitKind.DAY -> Triple("день", "дні", "днів")
                UnitKind.HOUR -> Triple("година", "години", "годин")
                UnitKind.MINUTE -> Triple("хвилина", "хвилини", "хвилин")
            }
            "be" -> when (kind) {
                UnitKind.YEAR -> Triple("год", "гады", "гадоў")
                UnitKind.DAY -> Triple("дзень", "дні", "дзён")
                UnitKind.HOUR -> Triple("гадзіна", "гадзіны", "гадзін")
                UnitKind.MINUTE -> Triple("хвіліна", "хвіліны", "хвілін")
            }
            else -> when (kind) {
                UnitKind.YEAR -> Triple("год", "года", "лет")
                UnitKind.DAY -> Triple("день", "дня", "дней")
                UnitKind.HOUR -> Triple("час", "часа", "часов")
                UnitKind.MINUTE -> Triple("минута", "минуты", "минут")
            }
        }
        val abs = kotlin.math.abs(n)
        val mod100 = (abs % 100).toInt()
        val mod10 = (abs % 10).toInt()
        val word = when {
            mod100 in 11..14 -> forms.third
            mod10 == 1 -> forms.first
            mod10 in 2..4 -> forms.second
            else -> forms.third
        }
        return "$n $word"
    }
}
