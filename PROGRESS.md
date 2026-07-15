# PROGRESS.md

Running log for the SmsSender project, so a new Claude Code session can pick
up where the last one left off.

## Status as of this writing

**Done:**
- Created Android Studio project "SmsSender" (package `com.example.smsforwarder`,
  min SDK 24, Kotlin, Empty Views Activity template).
- Added/replaced `MainActivity.kt`, `SmsReceiver.kt`, `EmailSender.kt`, `Prefs.kt`.
- Replaced `activity_main.xml` with the settings-form layout.
- Merged manifest additions: `RECEIVE_SMS`, `READ_SMS`, `INTERNET` permissions
  added; `<uses-feature android:name="android.hardware.telephony" android:required="false" />`
  added to silence a hardware-feature lint warning.
- Manifest currently builds with only 3 harmless warnings (Play Store policy
  warning about SMS access + unused namespace declaration) — no red errors.

**Confirmed:**
- [x] `<receiver>` block for `.SmsReceiver` (priority 999, action
      `android.provider.Telephony.SMS_RECEIVED`, permission
      `android.permission.BROADCAST_SMS`) is present in the manifest.
- [x] `build.gradle.kts (:app)` has both JavaMail dependencies:
      `implementation("com.sun.mail:android-mail:1.6.7")` and
      `implementation("com.sun.mail:android-activation:1.6.7")`.
- [x] `./gradlew assembleDebug` builds successfully. Had to add a
      `packaging { resources { excludes += ... } }` block to
      `app/build.gradle.kts` to exclude `META-INF/NOTICE.md` and
      `META-INF/LICENSE.md` — both JavaMail jars (`android-mail` and
      `android-activation`) ship a file at the same `META-INF/NOTICE.md`
      path, which fails Gradle's resource merge step otherwise. Common,
      known issue with this library pair.

- [x] Ran on-device via wireless ADB on a Samsung Galaxy A56 (SM-A566B) —
      USB not actually needed since wireless debugging was already enabled.
- [x] Granted SMS permissions in-app; confirmed via `dumpsys package`.
- [x] Gmail App Password + settings entered and saved (verified by reading
      the app's `sms_forwarder_prefs.xml` via `adb shell run-as`).
- [x] Sent a real test SMS ("GOMO" keyword) and confirmed the forwarded
      email arrived.

**UI redesign (this session):**
- [x] `activity_main.xml` rebuilt with Material `TextInputLayout` (outlined,
      floating labels) instead of separate TextView+EditText pairs; wrapped
      in a `ScrollView` (fixes keyboard covering the form); added a title +
      subtitle header; helper text under App Password and Keywords fields;
      filled primary "Save Settings" button + outlined secondary "Grant
      Permissions" button for visual hierarchy.
- [x] Increased top padding (`paddingTop="56dp"`) so the header isn't
      crammed against the status bar on edge-to-edge displays.
- [x] Hand-authored a new adaptive launcher icon (envelope + forward-arrow
      motif, white on `#6750A4` purple matching the Material3 baseline
      theme) replacing Android Studio's default placeholder icon in
      `ic_launcher_foreground.xml` / `ic_launcher_background.xml`. Note:
      the legacy pre-API26 raster mipmap icons (`mipmap-*dpi/ic_launcher.webp`)
      were NOT regenerated — only devices below Android 8 would still see
      the old icon, not a concern for any real device in use here.
- [x] Added "which of my phone numbers received this" to the forwarded
      email (subject + body). Added `READ_PHONE_STATE` + `READ_PHONE_NUMBERS`
      permissions (requested via the same "Grant Permissions" button flow).
      `SmsReceiver.resolveReceivingLine()` reads the subscription's phone
      number via `SubscriptionManager`, falling back to "SIM slot + carrier
      name" if the number itself isn't populated (common — many carriers,
      including PH telcos like Globe/GOMO, don't provision the number onto
      the SIM). Confirmed working on-device: the user's own number showed
      up correctly in a real forwarded email.

- [x] Added an in-app "Ignore Battery Optimization" button (third button,
      outlined style) using `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
      — one tap instead of manual settings navigation. Needs
      `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` in the manifest (normal
      permission, no runtime prompt). On Samsung specifically, this alone
      isn't always enough — One UI has a separate "sleeping apps"/"deep
      sleeping apps" list under Device care > Battery that also needs
      manual checking; no public API for that layer.
- [x] Samsung's Device Care "App protection" (McAfee-powered) flags
      SmsSender as a "1 malware app" false positive — expected, since
      RECEIVE_SMS + READ_SMS + INTERNET + non-Play-Store install matches
      the classic SMS-stealer heuristic pattern. Confirmed via the actual
      device UI that there is NO per-app allowlist/trust option in this
      McAfee integration (unlike Google Play Protect) — only a blanket
      Uninstall action or an all-or-nothing App protection on/off toggle.
      Decision: leave App protection on, ignore the badge. Does not affect
      app functionality.
- [x] Keyword filter (`SmsReceiver.shouldForward()`) now matches against
      **sender OR body**, not body-only. Needed because real GOMO SMS
      (balance/expiry notices) don't always contain the literal word
      "GOMO" in the message text, only in the sender ID ("GOMO-AIS") —
      body-only matching would've silently dropped those. Confirmed via a
      real screenshot of GOMO OTP + promo messages.

**Deployed to parents' phone (Samsung, same One UI as the dev device):**
- [x] Debug APK (`app-debug.apk`, built 2026-07-10 22:21, reflects the
      sender-or-body keyword fix) sideloaded onto parents' phone.
- [x] Settings configured: same Gmail App Password reused (not
      device-locked), forward-to = user's own email, keyword = "GOMO".
- [x] Permissions granted, Play Protect/App protection false-positive
      handled (same as dev device — no per-app allowlist exists, ignored).
- [x] Battery optimization handled both ways: in-app "Ignore Battery
      Optimization" button AND the Samsung-specific manual check (Settings →
      Battery and device care → Battery → Background usage limits →
      Sleeping/Deep sleeping apps lists; also verified
      Settings → Apps → SmsSender → Battery = Unrestricted).

**Multi-part SMS fix (2026-07-11):**
- [x] Fixed a bug in `SmsReceiver.kt`: long SMS arrive as multiple
      concatenated PDU segments in one broadcast, but the old code looped
      over each PDU and sent/filtered it as its own separate message — a
      long SMS could get split into several emails (each just a ~150-char
      fragment), and the keyword filter running per-segment could silently
      drop segments where the keyword didn't appear. Fixed to
      `.map` all segments to `SmsMessage`, take the sender from the first
      segment, `joinToString("")` the bodies into one full message, then
      filter/send once. Hasn't bitten anyone yet since GOMO texts are short
      (single segment), but was a real latent bug.
- [x] **Build-verified (2026-07-11).** Android Studio was found at
      `D:\Program Files\Android\Android Studio` (earlier session only
      checked C: paths); its bundled JBR (JDK 21) works for command-line
      builds via `$env:JAVA_HOME = "D:\Program Files\Android\Android
      Studio\jbr"` then `gradlew.bat assembleDebug`. Build succeeded with
      only one pre-existing deprecation warning (`bundle.get()` in
      SmsReceiver.kt:18 — harmless, standard PDU extraction pattern).
- [ ] **Not yet device-tested.** Install the fresh `app-debug.apk` on the
      user's own phone and test with a >160 character SMS containing
      "GOMO" (confirm ONE email with the full text arrives, not several
      fragments).
- [ ] Once verified on the user's own phone, re-sideload the updated APK
      onto the parents' phone (same process as before — see "Deployed to
      parents' phone" above) so they get the fix too.

**Receiving-line fix (2026-07-12):** parents' phone showed "SIM 1 ( )"
instead of the receiving number in forwarded emails.
- [x] Root cause: `resolveReceivingLine()` fell back to
      `info.carrierName ?: info.displayName` — but on that device those are
      *empty strings*, not null, so the fallback chain produced an empty
      label. Now filters blanks properly ("unknown carrier" as last resort).
- [x] Added `SubscriptionManager.getPhoneNumber(subId)` (API 33+) as the
      preferred number source — it can pull from carrier/IMS sources, not
      just the SIM record, so it may find numbers the legacy `info.number`
      field misses. Falls back to the legacy field below API 33.
- [x] Added an optional "This phone's number" settings field (new
      `line_label` pref, field in activity_main.xml + MainActivity). If
      set, it's used verbatim as the receiving line in the email — the
      reliable fix when the carrier never provisions the number anywhere.
      Existing saved settings are unaffected (new pref just defaults to "").
- [x] Build-verified via command-line gradlew.
- [ ] On parents' phone: install updated APK, and either verify the number
      now auto-detects (API 33+ path) or type their number into the new
      optional field and save.

**GitHub + Obtainium update channel (2026-07-12):**
- [x] Project is now a git repo, pushed to the public repo
      https://github.com/phinnite/SmsSender (gh CLI authenticated as
      phinnite; git identity set to the GitHub noreply email so the
      Gmail address isn't exposed in public commits).
- [x] Version bumped to versionCode 2 / versionName "1.1" and released:
      https://github.com/phinnite/SmsSender/releases/tag/v1.1 with
      `SmsSender-1.1.apk` attached (debug-signed, same keystore as the
      installs on both phones — all future builds must come from this PC
      or updates will be rejected for signature mismatch).
- [x] Added README.md; `.claude/settings.local.json` gitignored.
- [ ] Install Obtainium (github.com/ImranR98/Obtainium) on both phones,
      grant it "Install unknown apps", add https://github.com/phinnite/SmsSender
      as a tracked app, and update both phones to v1.1 through it (this
      v1.1 install also covers the pending multi-part-SMS and
      receiving-line on-device verifications above).
- **Release process for future changes:** bump `versionCode` (+1) and
  `versionName` in app/build.gradle.kts, commit + push, build
  (`assembleDebug -q`), then
  `gh release create vX.Y <apk> --title vX.Y --notes-file <file>`
  (rename the apk to SmsSender-X.Y.apk first; use --notes-file, inline
  --notes with parentheses hit PowerShell quoting issues). Obtainium
  picks it up from there — phones get an update notification.

**Debug logging + UI polish (2026-07-15):**
- [x] Added `FileLog.kt` — a `Log`-alike helper that mirrors every log call to
      both Logcat AND a capped (~200KB) `debug_log.txt` in app-private
      `filesDir`. Needed because Logcat is only visible while a PC is actively
      watching via adb at the moment an SMS arrives — useless for a phone in
      daily use elsewhere (e.g. parents' phone). `SmsReceiver` (previously had
      ZERO logging) and `EmailSender` now log the full path through
      `FileLog.*`: broadcast received, missing pdus/extras, sender + body
      length, receiving-line resolution, permission-missing, keyword
      pass/fail, forward decision, send success/failure.
- [x] Added `LogActivity.kt` + `activity_log.xml` — full-screen "Debug Log"
      page (opened from a new "View Debug Log" button on the settings screen),
      readable on the phone itself with NO adb needed. Has a bold "Debug Log"
      header (the app theme is `...NoActionBar`, so a TextView header is used
      instead of an action-bar title — `supportActionBar` is null here),
      monospace 15sp selectable text, and side-by-side Back + Clear Log
      buttons. Registered in manifest (`exported="false"`).
- [x] Keywords field: added `inputType="text"` + `imeOptions="actionNext"` so
      Enter advances to the next field instead of inserting a newline (it had
      no inputType, which defaulted to multi-line). Parsing unchanged.
- [x] Merged the two email fields ("Gmail address (sender)" + "Forward to
      email address") into one "Your Gmail address" field — the user always
      uses the same address for both. `MainActivity` writes the single value
      to BOTH the `gmail_address` and `dest_email` prefs keys; `Prefs.kt` and
      `EmailSender.kt` untouched (still conceptually sender vs. destination,
      just identical values), so re-adding a separate destination later is a
      one-field change. NOTE: an old separately-saved `dest_email` persists
      until the next Save re-converges both keys.
- [x] "Grant Permissions" button now handles the permanently-denied case:
      once a permission is denied for good, `requestPermissions()` is a silent
      no-op, so the button deep-links to app settings
      (`ACTION_APPLICATION_DETAILS_SETTINGS`) instead. Uses a new
      `perms_requested` pref flag to disambiguate "never asked" from
      "permanently denied" (both make `shouldShowRequestPermissionRationale`
      return false). Startup auto-request behavior unchanged.
- [x] Last field ("This phone's number") was hidden behind the keyboard:
      added an `onFocusChange` listener that `smoothScrollTo`s the bottom
      after a 100ms delay (lets the keyboard settle so the resize is applied),
      plus 60dp bottom padding on the form to give the scroll room to lift the
      field + its helper text clear of the keyboard.
- [x] All build-verified on the user's own phone this session (installed via
      `gradlew.bat installDebug`); debug-log path confirmed working end-to-end
      (real GOMO-AIS SMS logged received → keyword match → email sent).
- [ ] Not yet deployed to parents' phone. Next session: install this build
      there, reproduce the failure, and read the on-device Debug Log to see
      where it breaks (never fires = OS killing it / stopped-state / battery
      restriction; fires but no send = permission or SMTP issue). NOT yet
      released via GitHub/Obtainium — versionCode still 2 / "1.1"; bump +
      release if pushing to parents' phone through Obtainium.

**Next steps:**
- [ ] Keep an eye on whether forwarding silently stops after long idle
      periods — Samsung can re-add an app to the sleeping-apps list over
      time even after manual removal; if a real GOMO SMS doesn't forward,
      re-check that list first.
- [x] Folder rename done: project now lives at `D:\Projects\SmsSender`
      (was `D:\Android Projects\SmsSender`). No doc updates were needed;
      Gradle build confirmed working from the new path.

## Design decisions already made (don't relitigate without reason)

- SMTP direct-send chosen over a webhook relay (Zapier/Make/n8n) — simplicity
  over keeping credentials off-device.
- Package name intentionally kept as `com.example.smsforwarder` while the
  project/app display name was changed to "SmsSender" — this was deliberate
  to avoid a naming collision with an earlier different project, done without
  needing to touch any file contents.
- Credentials stored in plain (unencrypted) SharedPreferences — acceptable
  for personal single-device use.
