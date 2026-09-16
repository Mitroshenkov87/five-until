# Five Until

A tiny Android countdown app with exactly **five slots**. Package `com.fiveuntil.app`. Black and grey. No clutter.

**Version:** 1.1 (`versionCode` 2) · Signed release builds as Aleksandr Mitroshenkov · [MIT License](LICENSE)

## Idea

Most reminder apps bury you in lists, calendars, and settings. **Five Until** is the opposite: five things you care about, each with a clear “how long until…”, always visible on one screen.

You type a title in any language, pick a date and time, and the slot shows the remaining time in plain words — years, days, hours, minutes — whichever makes sense. When the moment arrives, the slot greys out and says it happened («Happened» / «Наступило» / …). Dismiss it, or leave it; after about an hour it clears itself and the others climb up.

## Why five?

Five is enough for what is actually on your mind and small enough that the list stays honest. Slots are always **packed to the top**: no empty holes in the middle. New events append; clearing one slides the rest up.

## How it works

1. Tap an empty slot → enter a title → pick date and time → optional reminder.
2. Tap a title to edit the text; tap the countdown to change date/time; tap the reminder line to change alert type.
3. Optional reminder per event:
   - notification
   - notification with sound
   - play a short melody once (not a repeating OS alarm)
4. When due: on next open (or within ~15s if the app is already open) the slot shows **Happened** and turns grey. Tap **Dismiss** anytime; otherwise it auto-clears after ~1 hour.

First launch seeds slot 1 with a demo event (overwritable): «Илон Маск 100 лет» on 28 June 2071, 00:00 local time.

## Look & language

- Monochrome UI: black background, grey text and accents only.
- UI strings follow the system locale for **English**, **Russian**, **Ukrainian**, and **Belarusian** (fallback: English).
- Event titles are unrestricted — any language, any script.
- Countdown phrases use English resources or Slavic plural rules (RU/UK/BE) in code.

## Architecture (main classes)

| Class | Role |
| --- | --- |
| `MainActivity` | Programmatic five-slot UI, due/expiry tick, add/edit dialogs, packing |
| `Event` / `ReminderType` | Slot model; `MAX_SLOTS=5`, `AUTO_CLEAR_MS` ≈ 1h |
| `EventStore` | SharedPreferences JSON persistence + one-time demo seed |
| `ReminderScheduler` | Exact (or fallback) `AlarmManager` alarms per event |
| `ReminderReceiver` | Boot reschedule + notification / melody delivery |
| `MelodyPlayer` | One-shot synthesized AudioTrack melody |
| `RemainingFormatter` | Locale-aware remaining-time wording |

More detail: [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

## Permissions

- **Notifications** — optional reminder alerts (Android 13+).
- **Exact alarms** — so reminders can fire near the chosen time when the system allows it. If denied, in-app “happened” detection still works when you open the app or while it is open.
- **Boot completed** — to reschedule reminders after reboot.

No accounts, no network, no ads. Data stays on device.

## Install (APK)

Download the signed release APK from **[GitHub Releases](https://github.com/Mitroshenkov87/five-until/releases)** (see tag **v1.1** when published). Install on Android (allow unknown sources if needed).

**APK binaries are not stored in this git repository** (see `.gitignore`).

## Build

Needs Android SDK and JDK 17+.

Wrapper scripts (`gradlew` / `gradlew.bat`) and `gradle/wrapper/gradle-wrapper.properties` (Gradle **8.11.1**) are in the repo. The binary `gradle/wrapper/gradle-wrapper.jar` may be missing from git. If `./gradlew` fails with a missing-jar error, generate the wrapper once:

```bash
# Install a system Gradle first, then:
gradle wrapper --gradle-version 8.11.1
./gradlew assembleRelease
```

Or copy `gradle-wrapper.jar` from another Android project that matches `gradle-wrapper.properties`.

Unsigned release APK lands at:

`app/build/outputs/apk/release/app-release-unsigned.apk`

Sign with `apksigner` using your own keystore. **Do not commit** `*.jks`, `*.keystore`, `*.password`, or `local.properties`.

Anyone may take this project, change it, and rebuild it under the MIT License.

---

## На русском

**Five Until** — минималистичный Android-таймер на **пять слотов**. Чёрно-серый экран, без аккаунтов и рекламы.

- Заполните слот: название → дата/время → напоминание (по желанию).
- Когда время наступило, слот сереет и пишет **«Наступило»**; можно закрыть вручную или подождать ~1 час — слот исчезнет, остальные поднимутся вверх.
- Демо при первом запуске: «Илон Маск 100 лет», 28 июня 2071.
- Языки интерфейса: EN / RU / UK / BE.

Готовый APK: [Releases](https://github.com/Mitroshenkov87/five-until/releases). Сборка из исходников — см. раздел **Build** выше. Подробнее для разработчиков: [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

## License

[MIT](LICENSE) — Copyright (c) 2026 Aleksandr Mitroshenkov.
