# Interval

**Remember what you learn.**

> **RevenueCat Shipaton 2026 — Next Gen Award submission.**
> Built in three days by a first-time Kotlin developer.

An Android study app built around spaced repetition. You capture something you want to
remember — or describe a topic and let AI build the set — and Interval brings it back at
widening intervals: tomorrow, three days, a week, a month, three months. Recall it and it moves
further out. Miss it and it comes straight back.

[View the hackathon on Devpost](https://revenuecat-shipaton-2026.devpost.com/)

---

## What it does

**Three question types, one scheduler.** Flashcards you grade yourself, plus multiple choice and
true/false that grade themselves. All three share one model, so the ladder, the statistics and
the storage never branch on type.

**AI that earns its place.** Three surfaces, each tied to a real moment:

| Surface | When it fires |
|---|---|
| **Generate** | Name a topic or paste notes, get a mixed study set back |
| **Explain this** | The instant you get a card wrong, when you actually want the answer |
| **Coach** | A chat tab for whatever else you're stuck on |

Nothing the model produces is saved until you accept it — a hallucinated fact costs one tap to
reject rather than sitting in your rotation for a month.

**A reward loop.** Correct answers pay XP, consecutive answers pay more, and a streak survives
until the end of the following day so one late night doesn't erase a month.

**Notifications that are the product.** A card coming due schedules the alarm; tapping the
notification opens straight into that session.

---

## For judges: testing the in-app purchase

This app uses the **RevenueCat Test Store**, so the full purchase flow works with no Google Play
account and no real payment method.

1. Build and run (below), or install the APK from Releases.
2. Free tier is **5 cards**. The starter deck has 3.
3. Add cards until you hit the limit — the next attempt opens the paywall.
4. Pick a plan, tap **Continue**. RevenueCat's Test Store sheet appears.
5. Tap **TEST VALID PURCHASE**. Pro activates and the limit lifts.

**TEST FAILED PURCHASE** and **CANCEL** are handled too — both leave you on the free tier, which
is the path that usually goes untested.

To reset: Settings → Apps → Interval → Storage → Clear data. Or `adb shell pm clear com.munadir.interval`.

A debug build also has **Settings → Debug → Grant Pro**, a local-only override for screenshots
that never touches RevenueCat.

---

## Build

Android Studio, and an emulator or device on **API 26+**.

```
git clone https://github.com/<your-username>/interval.git
```

Open the folder, let Gradle sync, press Run. No API keys to supply, no local config file to
create — the committed RevenueCat key is a **Test Store** key that can only ever produce
simulated purchases, so the paywall works the moment you clone.

### Turning on the AI (optional, 2 minutes)

The AI features need a key, which is **never committed**. Any one of three providers:

| Provider | Where | Notes |
|---|---|---|
| **Gemini** | aistudio.google.com → Get API key | Free tier, no credit card |
| **OpenAI** | platform.openai.com → API keys | Uses account credit |
| **OpenRouter** | openrouter.ai → Keys | One key, many models |

Then **You → Settings → AI coach**: pick the provider, paste the key, **Save key**. Tap **Find
models** to list what that key can actually call and pick one.

Keys are stored per provider in `SharedPreferences` on the device and are sent only to the
provider you selected. Without a key the AI screens show a designed "connect a key" state rather
than appearing broken.

---

## How it works

### Scheduling

Each card holds a `stage`, an index into `INTERVALS = [1, 3, 7, 30, 90]` days. A correct answer
advances the stage and pushes `dueAt` out by the new interval; a miss resets to zero. That is the
entire algorithm — deliberately simpler than SM-2, because the widening gap does the work and the
extra parameters would not be visible in a three-day build.

An `AlarmManager` alarm is armed for the earliest `dueAt` across all cards. When it fires,
`ReminderReceiver` posts a notification and re-arms for whatever is next. A second alarm handles
the daily reminder at a time the user picks.

Both are **inexact** on purpose: exact alarms need a special permission on Android 12+ and exist
for calendar events and timers. A study reminder landing a few minutes late is fine, and letting
the OS batch it spares the battery.

### Monetization

One entitlement, one flag. `Billing.isPro` is the only thing any gated feature reads —
scattering purchase checks through the UI is how an app ends up with one screen that thinks you
paid and another that doesn't.

Pro is defined as *any* active entitlement rather than a hardcoded identifier, so renaming things
in the RevenueCat dashboard cannot silently lock a paying user out. The paywall renders whatever
packages the `default` offering contains, so prices change in the dashboard without an app update.

The local profile's id is passed to `Purchases.logIn()`, so **a purchase belongs to the person,
not the install** — reinstall and Pro comes back.

### One dependency, on purpose

The only external library is the RevenueCat SDK. Everything else is framework:

| Need | Used | Instead of |
|---|---|---|
| Storage | `org.json` + a file in `filesDir`, written atomically | Room |
| Background work | `AlarmManager` | WorkManager |
| Navigation | a sealed interface and `when` | navigation-compose |
| HTTP | `HttpURLConnection` | Retrofit / OkHttp / Ktor |
| Settings | `SharedPreferences` | DataStore |
| Icons, orb, charts | Compose `Canvas` | icon packs, chart libraries |

Written by someone who had never touched Kotlin a week earlier, on a three-day clock. Every
dependency is a version to keep compatible and a failure mode to debug; at that pace the
framework version wins every time.

---

## Project layout

```
com.munadir.interval
├── Config.kt              constants: RevenueCat key, free-tier limit
├── IntervalApp.kt         Application — configures RevenueCat, seeds storage
├── MainActivity.kt        routing, attachments, AI state, wiring
├── ai/
│   ├── AiClient.kt        three providers behind one interface
│   └── Attachment.kt      what a picked file actually gives us
├── billing/Billing.kt     the single isPro source of truth
├── data/
│   ├── Card.kt            model, card types, the interval ladder
│   ├── CardStore.kt       persistence, review log, streaks, retention
│   └── Settings.kt        preferences, profile, per-provider keys
├── notifications/         Notifier, Scheduler, ReminderReceiver
└── ui/                    screens, theme, the orb, the reward burst
```

---

## Known limits

Honest about what three days bought:

- **No sync.** Cards live on one device.
- **PDFs and photos attach but are not parsed.** On-device PDF extraction and OCR are each a
  dependency and a failure mode; the UI says "used as a topic hint" rather than pretending it
  read the page. Plain text files *are* read in full.
- **Android only.** A Kotlin Multiplatform build was scoped and dropped — without a Mac the iOS
  half could not be compiled, let alone tested.
- **The AI needs your own key.** Committing one to a public repo gets it scraped within hours.

## License

MIT — see [LICENSE](LICENSE).
