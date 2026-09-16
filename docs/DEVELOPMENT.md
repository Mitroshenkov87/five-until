# Five Until — development notes

Package: `com.fiveuntil.app` · minSdk 26 · target/compileSdk 35 · Kotlin + AndroidX AppCompat · versionName **1.1** / versionCode **2**.

This document describes **existing** behavior only. It is a map of the codebase, not a feature backlog.

## Layout of the project

```
app/src/main/java/com/fiveuntil/app/
  MainActivity.kt        # UI + due/expiry loop
  Event.kt               # model + ReminderType
  EventStore.kt          # SharedPreferences JSON
  ReminderScheduler.kt   # AlarmManager
  ReminderReceiver.kt    # boot + fire
  MelodyPlayer.kt        # one-shot tone
  RemainingFormatter.kt  # remaining-time strings
app/src/main/res/
  values/                # EN strings + theme
  values-ru|uk|be/       # localized chrome strings
  drawable|mipmap-*/     # launcher icon
```

UI is built in code (no `res/layout`). Theme: `Theme.FiveUntil` (dark / monochrome).

## Storage

`EventStore` uses `SharedPreferences` named `five_until`:

| Key | Purpose |
| --- | --- |
| `events` | JSON array of `{id, title, at, reminder, happenedAt}` |
| `next_id` | Monotonic Long for new event ids |
| `demo_done` | Ensures the demo seed runs at most once |

`save()` keeps at most five events. There is no cloud sync.

**Demo seed:** if neither `demo_done` nor `events` exists, insert «Илон Маск 100 лет» at local midnight **28 June 2071**.

## Slot packing

`MainActivity.events` is a dense `MutableList` (0…5 items). Slot view `i` binds to `events.getOrNull(i)`.

- Add always **appends** (even if the user tapped a lower empty slot).
- Dismiss / auto-clear **removes** the item; later items shift up.
- Empty slots therefore always sit at the bottom.

## Due-state and auto-clear

`processDueAndExpiry()` (on create, resume, and every ~15s while resumed):

1. If `happenedAtMillis == null` and `atMillis <= now` → set `happenedAtMillis = now`, stop melody.
2. If `shouldAutoClear(now)` (~1 hour after mark) → cancel alarm, remove from list.
3. Persist + `ReminderScheduler.rescheduleAll`.

Grey UI uses `R.string.happened` (EN «Happened», RU «Наступило», …) and shows a **Dismiss** control. There is no swipe gesture in the current code — clear via Dismiss or auto-clear.

## Scheduling and reminders

`ReminderScheduler` sets `RTC_WAKEUP` exact alarms when possible (`setExactAndAllowWhileIdle`), falling back if exact-alarm permission is missing. `ReminderType.NONE` and already-due events are not scheduled.

`ReminderReceiver`:

- `BOOT_COMPLETED` → load + reschedule all.
- `ACTION_FIRE` → silent notification, sounding notification, or `MelodyPlayer.playOnce()`.

Marking an event “happened” in storage/UI is still the Activity’s job; the receiver only delivers the alert.

## Melody

`MelodyPlayer` synthesizes six short sine notes into a static `AudioTrack`, plays once, then `stop()`/`release()`. It is intentionally not a looping system alarm.

## Locale

- Chrome strings: `res/values*` (EN default + RU/UK/BE).
- Countdown units: `RemainingFormatter` — English via string resources; RU/UK/BE via Slavic plural triples in code (`Locale.getDefault().language`).

## Permissions (manifest)

`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`.

## Build / wrapper

Gradle **8.11.1** (`gradle/wrapper/gradle-wrapper.properties`). If `gradle-wrapper.jar` is absent:

```bash
gradle wrapper --gradle-version 8.11.1
./gradlew assembleRelease
```

Do **not** commit: `local.properties`, `*.jks`, `*.keystore`, `*.password`, `*.apk`.

Release APKs belong on **GitHub Releases**, not in git.

## Signing

Official release APKs are signed as **Aleksandr Mitroshenkov**. Contributors rebuild with their own keystore.

## Related docs

See the root [README.md](../README.md) for product overview, install link, and a short Russian section.
