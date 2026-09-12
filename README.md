# Pocketsum

Pocketsum (formerly "Finance Tracker") is an offline, single-currency (₹) personal finance tracker for Android and iOS, built with Kotlin Multiplatform and
Compose Multiplatform. See `finance-tracker-prd.md` for the full requirements and `docs/branding/` for the brand, icon sources and store listing.

## Layout

```
composeApp/
  src/commonMain/kotlin/com/codigitech/ft/
    domain/      models, repository interfaces, use cases (pure Kotlin, unit-tested)
    data/        SQLDelight repositories, mappers, seeder, DbExecutor (single-thread DB access)
    platform/    Notifier / RecurringScheduler / BiometricAuthenticator contracts, AppSettings, AppLockManager
    ui/          Compose screens + ViewModels, navigation, theme
      components/  shared building blocks (money text, transaction rows, charts, swipe-to-delete, inputs)
      theme/       Material scheme, FinanceColors composition local, AppIcons, Motion specs
    di/          Koin modules
  src/commonMain/sqldelight/   schema (.sq), migrations/*.sqm and the versioned schema snapshots in databases/
  src/androidMain/             Android actuals: SQLite driver, AlarmManager reminder, NotificationManager, BiometricPrompt
  src/iosMain/                 iOS actuals: native SQLite driver, UNUserNotificationCenter, LocalAuthentication
  src/commonTest/              use-case tests with in-memory fakes
  src/androidUnitTest/         SQLDelight query tests on the JVM sqlite driver
iosApp/                        Xcode project; a Run Script phase builds the Kotlin framework
```

## Screens

- **Home** - hero balance with month switcher and animated totals, account carousel, spending breakdown,
  over-budget alert, upcoming recurring rules, recent transactions grouped by day.
- **History** - day-grouped list, Expense/Income/Transfer chips, search in the app bar, account/category/date
  filters in a bottom sheet, swipe either way to delete with Undo.
- **Insights** - six-month income vs expense chart (tap a month to inspect it), per-category budgets with
  progress bars, where money went / came from, highlights (average per day, largest expense, busiest day).
- **Accounts** - card list with per-type icons, collapsible archived section; detail shows this month's
  in/out and the account's history with swipe-to-delete.
- **Settings** - recurring rules, categories, theme (System/Light/Dark, applied live), app lock, CSV export via the
  system share sheet, about (with a privacy-policy link once `Brand.PRIVACY_POLICY_URL` is set).
- **Editor** - type-tinted accent, large amount field, category chip grid, Today/Yesterday/pick-a-date chips.

## Key decisions

- Amounts are `Long` minor units (paise). `Money` handles parsing and Indian-grouped formatting.
- A transfer is a linked pair of `TransactionEntity` rows (`TRANSFER_OUT` + `TRANSFER_IN`) sharing a `transferId`.
  Transfers never count toward income/expense. Lists that span all accounts (History, Home recent, CSV export) show
  a transfer once (the outgoing leg); an account's own history shows its leg.
- Balances and monthly totals are computed by SQL on the fly, never stored.
- Deleting an account with history archives it and pauses its recurring rules; deleting an empty account removes
  it and its rules. Deleting a category moves its transactions and rules to the built-in "Uncategorized" of the
  same type and drops its budget.
- Recurring rules are synced every time the app comes to the foreground (`RecurringSyncCoordinator`, on
  `ON_START`). OS alarms/local notifications are best-effort reminders only. Resuming a paused rule skips the
  dates it missed. Monthly rules are anchored to the start date's day of month, so a rule that started on the
  31st posts on Feb 28 and then Mar 31 rather than drifting to the 28th.
- App lock: biometric first, in-app PIN fallback. The PIN is stored as a salted hash in preferences. The
  biometric prompt is triggered on `ON_RESUME` so it works when the lock engaged in the background.
- Budgets (schema v2) are one row per expense category; progress is computed by joining this month's
  category totals, never stored. Deleting a category removes its budget.
- Deleting a transaction returns a `DeletedEntry` snapshot so the UI can offer Undo; restoring inserts
  fresh rows with the same content.
- Income/expense/transfer colours come from `LocalFinanceColors`, so they follow the in-app theme override
  and not just the system setting. Navigation transitions live in `ui/theme/Motion.kt`.
- Icons beyond `material-icons-core` are hand-drawn path data in `AppIcons` instead of pulling the
  extended icon pack.

## Build

Requirements: JDK 17+ (Gradle is pinned to the Android Studio JBR 21 in `gradle.properties`; change the path if
yours differs), Android SDK with platform 37, Xcode 16+.

```sh
./gradlew :composeApp:assembleDebug          # Android APK -> composeApp/build/outputs/apk/debug/
./gradlew :composeApp:testDebugUnitTest      # unit + database tests
./gradlew :composeApp:bundleRelease          # Play Store AAB -> composeApp/build/outputs/bundle/release/
open iosApp/iosApp.xcodeproj                 # iOS: pick a simulator and Run
```

Release builds are minified with R8 (`composeApp/proguard-rules.pro`) and signed with the upload key described in
`keystore.properties.example`. Without a `keystore.properties` the release build type is signed with the debug key so
it can still be installed for smoke-testing. The step-by-step Play Store guide is
`docs/branding/store/play-store-publishing.md`.

Command-line iOS build:

```sh
cd iosApp && xcodebuild -scheme iosApp -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' CODE_SIGNING_ALLOWED=NO build
```

Set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig` to run on a physical iPhone.

## Notes

- AGP 9 refuses `com.android.application` together with the KMP plugin unless
  `android.builtInKotlin=false` and `android.newDsl=false` are set (see `gradle.properties`). Compose
  Multiplatform 1.12 requires AGP 9.1+ and compileSdk 37, so this is the supported escape hatch for a
  single-module app.
- Schema changes: bump the SQLDelight version, add a `.sqm` migration, run
  `./gradlew :composeApp:generateCommonMainFinanceDatabaseSchema` and commit the new `databases/N.db`.
  `verifyMigrations` is on, so the build fails if a migration does not reproduce the snapshot.
