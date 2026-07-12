# SmsSender

A personal Android app that listens for incoming SMS and forwards matching
messages to an email address via Gmail SMTP.

Built for private sideloaded use (not on Google Play). Releases are published
here so [Obtainium](https://github.com/ImranR98/Obtainium) can keep the
installed phones up to date.

## What it does

- Listens for incoming SMS via a broadcast receiver — the app does not need
  to be running.
- Filters by comma-separated keywords matched against the sender **or** the
  message body (blank filter = forward everything).
- Forwards matching messages to a configured email address using Gmail SMTP
  (requires a [Gmail App Password](https://myaccount.google.com/apppasswords),
  not your real password).
- The forwarded email includes which of the phone's lines received the SMS
  (auto-detected where the carrier allows, with a manual override field).

## Setup

1. Install the APK from the latest release.
2. Open the app, grant SMS/phone permissions, and tap "Ignore Battery
   Optimization".
3. Enter the sending Gmail address, its App Password, the destination email,
   and optional keywords. Save.
4. On Samsung phones, also make sure the app isn't in Settings → Battery →
   Background usage limits → Sleeping/Deep sleeping apps.

## Notes

- Min SDK 24 (Android 7.0). Kotlin, single module.
- Credentials are stored in plain SharedPreferences — fine for a personal
  single-device app; don't use this pattern for anything distributed.
- Security apps may flag the SMS + internet permission combination as
  suspicious. That's a heuristic false positive, but you should read the
  code yourself before trusting any app with your messages.
