# Five Until

A tiny Android countdown app with exactly **five slots**. Black and grey. No clutter.

## Idea

Most reminder apps bury you in lists, calendars, and settings. **Five Until** is the opposite: five things you care about, each with a clear “how long until…”, always visible on one screen.

You type a title in any language, pick a date and time, and the slot shows the remaining time in plain words — years, days, hours, minutes — whichever makes sense. When the moment arrives, the slot greys out and says it happened. Dismiss it, or leave it; after about an hour it clears itself and the others climb up.

## Why five?

Five is enough for what is actually on your mind and small enough that the list stays honest. Slots are always packed to the top: no empty holes in the middle. Fill a lower empty slot and it moves as high as it can. Clear one and everything below slides up.

## How it works

1. Tap an empty slot → enter a title → pick date and time → optional reminder.
2. Tap a title to edit the text; tap the countdown to change date/time.
3. Optional reminder per event:
   - notification
   - notification with sound
   - play a short melody once (not a repeating OS alarm)
4. When due: on next open (or immediately if the app is already open) the slot shows **Happened** and turns grey. Dismiss anytime; otherwise it auto-clears after ~1 hour.

First launch seeds slot 1 with a demo event (overwritable): «Илон Маск 100 лет» on 28 June 2071, 00:00 local time.

## Look & language

- Monochrome UI: black background, grey text and accents only.
- UI strings follow the system locale for **English**, **Russian**, **Ukrainian**, and **Belarusian** (fallback: English).
- Event titles are unrestricted — any language, any script.

## Permissions

- **Notifications** — for optional reminder alerts (Android 13+).
- **Exact alarms** — so reminders can fire near the chosen time when the system allows it. If exact alarms are denied, in-app “happened” detection still works when you open the app or while it is open.
- **Boot completed** — to reschedule reminders after reboot.

No accounts, no network, no ads. Data stays on device.

## Install

Build from source (below), or check [Releases](https://github.com/Mitroshenkov87/five-until/releases) if a packaged APK has been published. **APK binaries are not stored in this repository.**

## Build

Needs Android SDK and JDK 17+.

The Gradle wrapper scripts are in the repo, but `gradle/wrapper/gradle-wrapper.jar` is a binary and is **not** committed. If `./gradlew` fails with a missing-jar error, generate it once:

```bash
gradle wrapper --gradle-version 8.7
./gradlew assembleRelease
```

Or copy `gradle-wrapper.jar` from another Android project that uses the same wrapper version as `gradle/wrapper/gradle-wrapper.properties`.

Anyone may take this project, change it, and rebuild it for their own needs under the MIT License.

## License

[MIT](LICENSE) — Copyright (c) 2026 Aleksandr Mitroshenkov.
