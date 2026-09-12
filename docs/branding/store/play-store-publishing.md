# Publishing Pocketsum to Google Play

Everything needed to take the Android build from this repository to a live Play Store listing. Items marked
**you** need a human, an account, or a secret and cannot be done from the codebase. Copy-paste values are in
`listing.md`; the policy text is `privacy-policy.md`.

## 1. What is already in place

| Item | State | Where |
|---|---|---|
| Application ID | `com.codigitech.ft` (cannot change after first upload) | `composeApp/build.gradle.kts` |
| Version | `versionCode 3`, `versionName 1.0.0`; `Brand.VERSION` and the iOS `CFBundleShortVersionString` match | `build.gradle.kts`, `Brand.kt`, `Info.plist` |
| SDK levels | minSdk 26 (Android 8.0), targetSdk 36, compileSdk 37 | `gradle/libs.versions.toml` |
| Release build | R8 minify + resource shrink on, rules in `composeApp/proguard-rules.pro`; smoke-tested on the emulator | `composeApp/build.gradle.kts` |
| Signing hook | Reads `keystore.properties` (git-ignored); falls back to the debug key when absent | `composeApp/build.gradle.kts`, `keystore.properties.example` |
| Permissions | `POST_NOTIFICATIONS`, `USE_BIOMETRIC`, `RECEIVE_BOOT_COMPLETED`; no `INTERNET` | `AndroidManifest.xml` |
| Adaptive + themed launcher icon, splash | Done | `composeApp/src/androidMain/res/` |
| Store icon 512, feature graphic 1024x500 | Done | `docs/branding/store/` |
| Listing copy (title, short + full description) | Done, within Play length limits | `listing.md` |
| Privacy policy text | Done, needs a contact email and a public URL | `privacy-policy.md` |
| Backups | `android:allowBackup="true"`, so Android auto-backup includes the SQLite DB and preferences (PIN hash) | `AndroidManifest.xml` |

## 2. One-time setup (**you**)

1. **Google Play Console account.** Register at play.google.com/console as an individual or an organisation
   (one-off USD 25 fee). Identity verification takes 1 to 3 days. Personal accounts created after November 2023
   must run a closed test with at least 12 testers for 14 days before production access is granted; plan for it.
2. **Upload keystore.** Generate it once and store it somewhere safe (password manager plus an offline copy).
   Losing it does not lock you out because Play App Signing holds the real app-signing key, but you would have to
   request an upload-key reset.
   ```sh
   keytool -genkeypair -v -keystore pocketsum-upload.jks -alias pocketsum \
     -keyalg RSA -keysize 2048 -validity 10000
   cp keystore.properties.example keystore.properties   # then fill in the passwords
   ```
   `keystore.properties`, `*.jks` and `*.keystore` are git-ignored. Never commit them.
3. **Privacy policy URL.** Host `privacy-policy.md` somewhere public and stable (GitHub Pages of this repo,
   a Notion public page, or your site). Fill in the contact email first. Then put the same URL in
   `Brand.PRIVACY_POLICY_URL` so Settings > About links to it, and rebuild.
4. **Contact email.** Play shows a developer email on the listing. A dedicated address (for example
   `pocketsum@codigitech.com`) keeps support out of your personal inbox.

## 3. Build the release bundle

Play accepts only Android App Bundles (`.aab`) for new apps.

```sh
./gradlew :composeApp:testDebugUnitTest          # 31 tests, all must pass
./gradlew :composeApp:bundleRelease              # -> composeApp/build/outputs/bundle/release/composeApp-release.aab
./gradlew :composeApp:assembleRelease            # optional APK for side-load testing on a device
```

Check the bundle is signed with the upload key, not the debug key:

```sh
~/Library/Android/sdk/build-tools/37.0.0/apksigner verify --print-certs \
  composeApp/build/outputs/apk/release/composeApp-release.apk | head -3
```

The `CN` must be the one you typed into `keytool`, not `CN=Android Debug`. (Adjust the build-tools version to
whatever is installed under `~/Library/Android/sdk/build-tools/`.)

Every upload needs a higher `versionCode`. Bump `versionCode`, `versionName`, `Brand.VERSION` and the iOS
`CFBundleShortVersionString`/`CFBundleVersion` together.

## 4. Screenshots (**you**, 15 minutes)

Play requires 2 to 8 phone screenshots, 16:9 or 9:16, each side between 320 and 3840 px. Capture from the
emulator after entering a few realistic transactions:

```sh
~/Library/Android/sdk/emulator/emulator -avd FinanceTracker -port 5556 &
~/Library/Android/sdk/platform-tools/adb -s emulator-5556 install -r composeApp/build/outputs/apk/release/composeApp-release.apk
~/Library/Android/sdk/platform-tools/adb -s emulator-5556 exec-out screencap -p > docs/branding/store/shots/01-home.png
```

Suggested set: Home (dark), Add transaction, History with filter sheet, Insights, Accounts, Home (light). Use the
in-app theme switch in Settings to get both themes. A 7-inch and a 10-inch tablet screenshot are optional; the
layout already scales.

## 5. Play Console: create the app

Console > All apps > Create app.

| Field | Value |
|---|---|
| App name | `Pocketsum: Offline Money Tracker` |
| Default language | English (India) or English (United Kingdom) |
| App or game | App |
| Free or paid | Free (cannot be changed to paid later) |
| Declarations | Tick the Developer Program Policies and US export laws boxes |

## 6. Dashboard: "Set up your app" checklist

Work down the list in the Console dashboard. Answers for Pocketsum:

- **App access.** All functionality is available without special access. (The optional PIN lock is set by the
  user, not required to review the app.)
- **Ads.** No, the app does not contain ads.
- **Content rating.** Start the IARC questionnaire, category "Utility, Productivity, Communication, or Other".
  Answer No to everything (no violence, no user-generated content, no purchases, no location sharing, no
  personal data sharing). Expected rating: Everyone / 3+.
- **Target audience.** 18 and over (avoids the Families policy requirements). Do not tick that the app may
  appeal to children.
- **News app.** No.
- **COVID-19 contact tracing.** No.
- **Data safety.** See section 7.
- **Government apps.** No.
- **Financial features.** Pocketsum is a personal budgeting tool; it does not provide loans, payments, banking,
  investment or crypto services. Choose "My app doesn't provide any financial features" if that option is
  offered; if the form insists on a category, "Personal finance management" with no third-party integrations.
- **Health apps.** No.
- **Privacy policy.** Paste the public URL from section 2.
- **App category.** Finance. Tags: Expense tracker, Personal finance, Budget.
- **Store listing.** Copy from `listing.md`: short description (72 chars), full description, upload
  `icon-512.png`, `feature-graphic-1024x500.png`, and the screenshots.
- **Contact details.** Email is required; website and phone optional.

## 7. Data safety form answers

The app has no network permission and no analytics SDK. Answer exactly this:

- Does your app collect or share any of the required user data types? **No.**
- Is all of the user data collected by your app encrypted in transit? Not applicable (nothing is transmitted).
- Do you provide a way for users to request that their data is deleted? Not applicable. Uninstalling removes
  everything; Settings > Data > Export lets users take a CSV copy first.

Because the answer to the first question is No, the rest of the form collapses and the listing shows
"No data collected". If Google's automated check flags the `androidx.biometric` or notification libraries, keep
the answer: biometric data never reaches the app and notifications are local.

## 8. Permissions declaration

None of the three permissions is "sensitive" in Play's sense, so no declaration form appears. Notes for the
review notes box if you want to pre-empt questions:

> Notifications: local reminders when a recurring transaction is posted. Biometrics: optional app lock using the
> OS prompt; the app never sees biometric data. Boot completed: re-arms the local reminder after a reboot. The
> app has no INTERNET permission and no server.

## 9. Testing tracks and rollout

1. **Internal testing** (up to 100 testers, instant). Upload the AAB from section 3, add your own Gmail
   address as a tester, open the opt-in link on your phone and install from Play. Check: install from a clean
   device, first-launch seed, add/edit/delete, transfers, recurring sync, app lock, notification permission
   prompt, CSV export share sheet, dark/light theme.
2. **Closed testing** (required for personal accounts created after Nov 2023): 12 opted-in testers for 14
   continuous days before you can apply for production. Friends and family count.
3. **Production.** Apply for production access when the closed test is done, then create a production release
   with the same AAB. First review usually takes 1 to 7 days. Choose a staged rollout (say 20%) only if you
   expect device-specific issues; for a single-currency personal tracker a full rollout is fine.

## 10. After launch

- **Crashes.** Play Console > Quality > Android vitals. Stack traces are readable because
  `proguard-rules.pro` keeps line numbers; upload `composeApp/build/outputs/mapping/release/mapping.txt` for
  each release under Release > App bundle explorer > Downloads so Play de-obfuscates them.
- **Updates.** Bump `versionCode` (any increase) and the four version strings, rebuild the AAB, upload to the
  same track. Play App Signing re-signs it.
- **Schema changes** still follow the SQLDelight migration flow in `CLAUDE.md`. A failed migration on a user's
  phone loses their data, so test each migration against a copy of a real database before shipping.
- **Target API deadline.** Google requires new updates to target the latest API within a year of its release;
  `targetSdk = 36` satisfies the 2026 deadline. Re-check every August.

## 11. iOS parity note

The iOS app is at feature parity with the Android build (same shared UI and data layer; platform pieces are
native SQLite, `UNUserNotificationCenter`, `LocalAuthentication`, `UIActivityViewController` for the CSV
export). `Info.plist` already carries the version, Face ID usage string and `ITSAppUsesNonExemptEncryption =
false`. It is not being submitted to the App Store; when that changes, the extra work is an Apple Developer
account, `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`, App Store Connect screenshots at 6.7-inch and
6.1-inch sizes, and the App Privacy questionnaire (answer "Data Not Collected").
