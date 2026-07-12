# CLAUDE.md

This file gives Claude Code context for working on this project.

## Project

**SmsSender** — a personal Android app (sideloaded, not published to Play Store)
that listens for incoming SMS and forwards matching ones to an email address
via Gmail SMTP.

- Package name: `com.example.smsforwarder` (project/app display name is
  "SmsSender" — these were intentionally kept different; don't try to make
  them match)
- Language: Kotlin
- Min SDK: 24 (Android 7.0), Target/Compile SDK: 34
- Build system: Gradle with Kotlin DSL (`build.gradle.kts`)
- IDE: Android Studio
- Explain Android-specific concepts (manifest, activities, broadcast
  receivers, gradle) rather than assuming familiarity.

## Architecture

- `MainActivity.kt` — settings screen (EditTexts for Gmail address, Gmail
  App Password, destination email, comma-separated keyword filter) +
  requests RECEIVE_SMS/READ_SMS runtime permissions. Saves to SharedPreferences.
- `Prefs.kt` — thin SharedPreferences wrapper for the above settings.
- `SmsReceiver.kt` — BroadcastReceiver for `android.provider.Telephony.SMS_RECEIVED`.
  Uses `goAsync()` + a background Thread to send email without blocking the
  main thread. Filters by keyword match (blank filter = forward everything).
- `EmailSender.kt` — sends email via Gmail SMTP (smtp.gmail.com:587, STARTTLS)
  using JavaMail (`com.sun.mail:android-mail` + `android-activation`), authenticated
  with the Gmail App Password (NOT the real Gmail password). Subject/body
  include both the SMS sender and which of the phone's own lines received it
  (see `SmsReceiver.resolveReceivingLine()` — best-effort, falls back to SIM
  slot + carrier name since many carriers don't expose the actual number).
- `res/layout/activity_main.xml` — settings form using Material
  `TextInputLayout`/`TextInputEditText` (outlined, floating labels) inside a
  `ScrollView`.
- Manifest: needs `RECEIVE_SMS`, `READ_SMS`, `READ_PHONE_STATE`,
  `READ_PHONE_NUMBERS`, `INTERNET` permissions, and a `<receiver>` block for
  `.SmsReceiver` with priority 999 on the `SMS_RECEIVED` action, protected by
  `android.permission.BROADCAST_SMS`.
- `app/build.gradle.kts` needs a `packaging { resources { excludes += ... } }`
  block excluding `META-INF/NOTICE.md` and `META-INF/LICENSE.md` — the two
  JavaMail jars both ship a file at that path and Gradle's resource merge
  fails without it. Known issue with this library pair, not a real conflict.
- Launcher icon (`ic_launcher_foreground.xml`/`ic_launcher_background.xml`)
  is hand-authored (envelope + forward-arrow, white on `#6750A4`) — not
  Android Studio's default placeholder. Legacy pre-API26 raster mipmaps were
  left untouched (irrelevant for any real device here).

## Known non-issues (don't "fix" these)

- Lint warning "Only default handlers can use SMS/Call Log access" — this is
  a Google Play publishing policy warning. Irrelevant since this app is
  sideloaded only, never published. Ignore it.
- "Namespace declaration is never used" in the manifest — harmless, ignore.
- Credentials (Gmail App Password) are stored in plain SharedPreferences,
  not encrypted. Acceptable tradeoff for a personal single-device app —
  don't "helpfully" swap this for EncryptedSharedPreferences or similar
  unless explicitly asked, since it adds complexity/dependencies for no
  real benefit here.

## Conventions / preferences

- Keep solutions minimal and appropriate for a personal single-user hobby
  project — don't over-engineer with things like WorkManager, DI frameworks,
  or multi-module architecture unless there's a concrete reason.
- Prefer explaining *why* something works the way it does for Android,
  since the author is coming from non-Android backgrounds.
- The author values direct, technically honest answers and will push back
  on errors — if unsure about an Android API detail, say so rather than
  guessing confidently.

## Current status

See `PROGRESS.md` for the running log of what's done and what's next.
