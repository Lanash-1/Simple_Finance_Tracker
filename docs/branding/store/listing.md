# Play Store listing — Pocketsum

## Store presence
- **App name (30 max):** `Pocketsum: Offline Money Tracker` (30)
- **Short description (80 max):** `Simple, offline tracking of cash, bank and card. Your money, summed up.` (72)
- **Developer name:** Codigitech
- **Category:** Finance  **Tags:** expense tracker, budget, personal finance
- **Contact email:** (fill in)  **Privacy policy URL:** (host `privacy-policy.md`, see below)

## Full description (4000 max)

Pocketsum is a simple, private money tracker that works completely offline. Log what you spend and earn across cash, bank accounts and cards, and always know where you stand.

**Everything stays on your phone**
No account, no sign-up, no cloud, no ads. Your data never leaves the device. Lock the app with your fingerprint, face, or a PIN.

**Accounts that stay in balance**
Track cash, bank, card and custom accounts side by side. Transfers between your own accounts move money without ever counting as income or expense, so totals never drift.

**Fast entry**
Add a transaction in seconds: amount, category, account, date, optional note. Recent transactions and this month's income, expense and net are right on the home screen.

**Recurring bills, handled**
Set up rent, subscriptions or salary once. Pocketsum adds them on schedule and shows what's coming up next.

**Budgets and insights**
Set monthly budgets per category, see a six-month income vs expense chart, and find out where the money went.

**History you can search**
Filter by account, category and date range, search by note or amount, and undo an accidental delete.

**Your data, yours to keep**
Export every transaction as a CSV file whenever you like.

Pocketsum is single-currency (₹) and built for people who want an honest, uncomplicated record of their money.

## Release notes (en-IN, 500 max)

```
Pocketsum 1.0 – simple, offline money tracking.

• Track cash, bank and card accounts side by side, in ₹
• Log expenses, income and transfers in seconds
• Recurring bills and salary post themselves on schedule
• Monthly budgets, a six-month cash-flow chart and spending insights
• Search and filter your history, undo accidental deletes
• Lock the app with fingerprint, face or PIN
• Export everything as CSV

No account, no ads, no internet. Your data never leaves your phone.
```

## Graphics checklist
- [x] App icon 512×512 PNG: `icon-512.png`
- [x] Feature graphic 1024×500 PNG: `feature-graphic-1024x500.png`
- [x] Phone screenshots: `shots/01..06-*.png`, 1080×1920 (9:16), framed from the raw 1080×2400 captures in `shots/raw/`
      by `java docs/branding/store/shots/Frame.java docs/branding/store/shots`. Demo data only (seeded database, not real figures).
      Re-capture with `~/Library/Android/sdk/platform-tools/adb -s emulator-5556 exec-out screencap -p > shots/raw/<name>.png`.
- [ ] Optional 7" / 10" tablet screenshots.

## Data safety form
- Does the app collect or share user data? **No.** All data is stored locally; there is no network access.
- Encryption in transit: N/A (no transmission). Data deletion: uninstalling removes all data.
- Permissions to declare: notifications (recurring reminders), biometrics (app lock), boot completed (re-schedule reminders).

## Content rating (IARC)
Utility / productivity questionnaire; no user-generated content shared, no purchases, no ads → expect "Everyone".

## Release checklist
Full walkthrough: `play-store-publishing.md`.
- [x] Version 1.0.0 / versionCode 3 in `composeApp/build.gradle.kts`, `Brand.VERSION`, and `Info.plist`.
- [x] R8 + resource shrinking on for release (`composeApp/proguard-rules.pro`); smoke-tested on the emulator.
- [x] Signing reads `keystore.properties` (see `keystore.properties.example`).
- [ ] Generate the upload keystore and fill in `keystore.properties`; enrol in Play App Signing on first upload.
- [ ] Host the privacy policy at a public URL, paste it into the listing and into `Brand.PRIVACY_POLICY_URL`.
- [ ] Fill in the contact email here and in `privacy-policy.md`.
- [x] Phone screenshots captured and framed (see Graphics checklist).
- [ ] Internal testing track first, then closed testing (12 testers / 14 days for new personal accounts), then production.
