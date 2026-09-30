## Inspiration

I have about four hundred screenshots of lecture slides on my phone. I have opened maybe six of them.

That is the actual problem. Capturing something is easy and feels productive, and it does almost nothing for memory. The part that works — going back to the material right before you would have forgotten it — is the part nobody does, because deciding *when* to review is tedious and easy to skip.

So the app makes that decision for you. You capture something once, or describe a topic and let it build the set, and Interval brings it back on a widening schedule: 1 day, 3, 7, 30, 90. Recall it and the gap widens. Miss it and it comes straight back tomorrow.

The honest origin story: this started as a team of four. Nobody started. With three days left I decided that submitting something working alone beat submitting nothing together, and built it solo. I had never written a line of Kotlin before this week.

## What it does

**Three question types over one scheduler.** Flashcards you grade yourself, plus multiple choice and true/false that grade themselves. They share one model, so the ladder, the statistics and the storage never branch on type.

**AI in three places, each tied to a real moment:**

- **Generate** — name a topic or paste your notes and get a mixed study set back. Nothing is saved until you tick it, so a hallucinated fact costs one tap to reject rather than sitting in your rotation for a month.
- **Explain this** — appears the instant you get a card wrong, which is the one moment you actually want an explanation.
- **Coach** — a chat tab for everything else.

It works with Gemini, OpenAI or OpenRouter. You bring your own key; the app never ships one.

**Notifications that are the product, not a retention tactic.** A card coming due arms the alarm. Tapping the notification opens straight into that session.

**A reward loop with a punchline.** Correct answers pay XP, streaks pay more, and there is a milestone ladder. The top tier is at one million XP and reads: *"the developer will genuinely buy you a five dollar lunch. Anywhere you like. Just email him. Nobody has ever emailed him."* At ten to twenty XP a card that is north of fifty thousand correct answers, so it is a promise I can actually afford to keep.

## How I built it

Native Android — Kotlin, Jetpack Compose, Material 3, `minSdk 26`.

**The whole app has exactly one external dependency: the RevenueCat SDK.** Everything else is framework:

| Need | What I used | Instead of |
|---|---|---|
| Storage | `org.json` + one file, written atomically | Room |
| Background work | `AlarmManager` | WorkManager |
| Navigation | a sealed interface and `when` | navigation-compose |
| HTTP to three AI providers | `HttpURLConnection` | Retrofit / OkHttp / Ktor |
| App icon, AI orb, charts, tab icons | Compose `Canvas` | icon packs, chart libraries |

That was a deliberate constraint, not minimalism for its own sake. As someone who had never used Kotlin, I could not debug a Gradle version conflict quickly. Every dependency is a version to keep compatible and a failure mode to diagnose, and on a three-day clock the framework version wins every time. It also means anyone can clone the repo and it just builds.

**Monetization.** One entitlement, one flag. `Billing.isPro` is the only thing any gated feature reads — scattering purchase checks through the UI is how an app ends up with one screen that thinks you paid and another that doesn't. Pro is defined as *any* active entitlement rather than a hardcoded string, so renaming something in the RevenueCat dashboard cannot silently lock a paying customer out. The paywall renders whatever packages the `default` offering contains, so prices change server-side with no app update.

The detail I am most pleased with: the local profile's id is passed to `Purchases.logIn()`, so **a purchase belongs to the person, not to the install.** Delete the app, reinstall, and Pro comes back.

## Challenges I ran into

**Shipping was harder than building.** Google Play requires new personal developer accounts to run a closed test with twelve testers opted in for fourteen continuous days before you can even apply for production. The Galaxy Store now requires corporate seller status for Android content. Apple needs a Mac I don't have. Working that out early is why I targeted Next Gen and used RevenueCat's **Test Store**, which gave me a fully working purchase flow with no store account at all.

**Three AI providers, because the first two fought me.** Gemini kept rejecting model ids that the documentation said were valid, and the failure looked identical to a bad key. Rather than keep guessing names, I added a **Find models** button that asks the provider which models that specific key can actually call and lists them. That turned an opaque dead end into a working picker, and it is better for anyone cloning the repo too.

**A blurred orb, not a shader.** The loading animation I wanted was a WebGL shader orb. There is no GLSL here, so it is six coloured lobes on three co-prime orbits drawn to a `Canvas`, run through a real Gaussian blur so their edges dissolve into each other, with a crisp unblurred pass on top carrying the rim and a specular highlight. That last part matters — without it a blurred blob just reads as an out-of-focus mistake.

**Bugs that taught me something.** A card promoted to the three-day rung rendered as "Due in 2 d", because integer division floored three-days-minus-two-seconds. The undo snackbar spawned exactly on top of the grade buttons and blocked the next card. Neither is clever; both are the kind of thing you only find by using your own app.

## What I learned

That most of the difference between an app that feels amateur and one that doesn't is **motion**. Halfway through I replaced every fixed-duration animation with a spring. A tween always arrives at the same moment no matter how far it travelled, which is exactly what makes an interface feel mechanical. I also redefined the app's tap modifier so every surface dips under your finger — one edit, and the whole app stopped feeling like rectangles with listeners attached.

And that scope is a skill. I planned Kotlin Multiplatform, OneSignal push, and OCR card generation. I cut all three — each was chasing a prize category that required a store release I could not get, and cutting them is the reason the parts that shipped actually work.

## What's next

Sync, card editing from inside a review, and real PDF and image parsing — the app currently accepts those attachments and says plainly that it is only using the filename as a hint, rather than pretending it read the page.

---

## For judges: testing the in-app purchase

The app uses the **RevenueCat Test Store**, so the full purchase flow works with no Google Play account and no payment method.

1. Free tier is **5 cards**; the starter deck has 3.
2. Add cards until you hit the limit — the next attempt opens the paywall.
3. Pick a plan, tap **Continue**, then **TEST VALID PURCHASE**. Pro activates and the limit lifts.

**TEST FAILED PURCHASE** and **CANCEL** are handled too — both correctly leave you on the free tier, which is the path that usually goes untested.

Source, build instructions and architecture notes: **https://github.com/munadirkhan/Interval**
