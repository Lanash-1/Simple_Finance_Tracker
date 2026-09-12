# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Pocketsum: an offline, single-currency (INR, ₹) personal finance tracker for Android and iOS. Kotlin Multiplatform +
Compose Multiplatform, one Gradle module (`:composeApp`), package `com.codigitech.ft`. Requirements live in
`finance-tracker-prd.md`; brand/icon/store assets in `docs/branding/`. `README.md` has the screen list and key
product decisions; do not duplicate them here, read it.

## Commands

```sh
./gradlew :composeApp:assembleDebug            # Android APK -> composeApp/build/outputs/apk/debug/
./gradlew :composeApp:installDebug             # install on the running emulator/device
./gradlew :composeApp:testDebugUnitTest        # ALL tests: commonTest (use cases, in-memory fakes) + androidUnitTest (SQLDelight on JDBC sqlite)
./gradlew :composeApp:testDebugUnitTest --tests 'com.codigitech.ft.domain.MoneyTest'                  # one class
./gradlew :composeApp:testDebugUnitTest --tests 'com.codigitech.ft.domain.MoneyTest.parsesUserInput'  # one test
./gradlew :composeApp:generateCommonMainFinanceDatabaseSchema   # after a schema change; run ALONE (see below)
./gradlew :composeApp:assembleRelease :composeApp:bundleRelease  # R8-minified APK + Play AAB (debug-signed unless keystore.properties exists)
./gradlew :composeApp:compileKotlinIosSimulatorArm64            # quick iOS compile check without Xcode
```

iOS: `open iosApp/iosApp.xcodeproj` and Run, or

```sh
cd iosApp && xcodebuild -scheme iosApp -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' CODE_SIGNING_ALLOWED=NO build
```

The Xcode Run Script phase calls `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`, so a Kotlin change
needs no separate Gradle step for iOS. There is no lint task configured.

### Toolchain constraints (all deliberate, do not "clean up")

- Gradle 9.5, AGP 9.3.1, Kotlin 2.4.10, Compose Multiplatform 1.12.0, compileSdk 37, minSdk 26.
- `gradle.properties` pins `org.gradle.java.home` to the Android Studio JBR 21 because the machine default JDK
  is 25, which Gradle cannot run on. Adjust the path if it differs; do not remove it.
- `android.builtInKotlin=false` and `android.newDsl=false` are required for `com.android.application` + KMP in
  one module on AGP 9. Removing them breaks the build.
- Emulator: use port 5556 (`~/Library/Android/sdk/emulator/emulator -avd Pixel_10a -port 5556`; the old
  `FinanceTracker` AVD no longer exists, `emulator -list-avds` shows what is available). `adb` is not on PATH,
  call `~/Library/Android/sdk/platform-tools/adb -s emulator-5556`. `emulator-5554` and any attached physical
  device belong to other work; do not drive them.
- Release: `keystore.properties` (git-ignored, see `keystore.properties.example`) supplies the upload key. R8 is
  on for release with `composeApp/proguard-rules.pro`; enum names are kept because they are persisted in the DB.
- iOS static framework needs `-lsqlite3`, set both in `composeApp/build.gradle.kts` (`linkerOpts`) and in the
  pbxproj `OTHER_LDFLAGS`. Kotlin top-level functions exported to Swift must not start with `init`
  (Swift imports them as initializers); the entry point is `startIosApp()`.
- Version is duplicated in `Brand.VERSION`, `versionName` in `composeApp/build.gradle.kts`, and
  `CFBundleShortVersionString`. Bump all three together.

## Architecture

Clean-ish layering inside `composeApp/src/commonMain/kotlin/com/codigitech/ft/`:

`ui` (Compose + ViewModels) -> `domain/usecase` -> `domain/repository` interfaces -> `data/repository` (SQLDelight)

- **domain** is pure Kotlin: models, repository interfaces (`domain/repository/Repositories.kt`), use cases as
  classes with `suspend operator fun invoke`, returning `Result<T>` (`Success`/`Failure(message)`) for validated
  writes. `DateProvider` is injected everywhere "today" matters so tests can pin the date (`FixedDate` in
  `commonTest/fakes/Fakes.kt`). New use cases get a `factory` in `di/AppModule.kt`.
- **data**: one `*RepositoryImpl` per table, each taking `FinanceDatabase` + `DbExecutor`. Reads return
  `Flow` via `asFlow().mapToList(exec.dispatcher)`; writes go through `exec.run { }` / `exec.exec { }`;
  multi-statement writes through `exec.transaction { }`. `DbExecutor` serialises all DB access onto one
  thread and lets nested calls inside a transaction run inline. Never touch `db.*Queries` outside a repository.
  `data/db/Mappers.kt` converts rows <-> domain (dates are epoch days, booleans are 0/1, enums are `name`).
- **platform** (common): `expect`-free contracts as plain interfaces (`Notifier`, `RecurringScheduler`,
  `BiometricAuthenticator`, `FileSharer` for the CSV export share sheet) plus `AppSettings`
  (multiplatform-settings wrapper), `AppLockManager`, `RecurringSyncCoordinator`. Each platform supplies its own Koin module with the implementations:
  `androidMain/FinanceApp.kt` and `iosMain/MainViewController.kt`. Only `DatabaseDriverFactory` is expect/actual.
- **DI**: Koin. `initKoin(platformModule)` composes `platformModule + dataModule + domainModule +
  presentationModule`. ViewModels are `viewModel { }` entries and obtained in screens with `koinViewModel()`.
- **UI root** `ui/App.kt`: `KoinContext` -> `FinanceTheme(themeMode)` -> `MainNavigation()` with `LockScreen`
  crossfaded on top when `AppLockManager.locked` is true. Bottom tabs are Home / History / Insights / Accounts /
  Settings; Recurring and Categories are pushed from Settings. String routes live in `ui/navigation/Routes.kt`
  (helpers build the arg strings; `-1` / empty string mean "absent"). Transitions in `ui/theme/Motion.kt` pick
  sheet / tab / push animations based on `Routes.isEditor` / `Routes.isTab`. Inner screens own their own
  `Scaffold`; the outer one only hosts the bottom bar.
- **Startup flow**: `App()` calls `RecurringSyncCoordinator.sync()` on every `ON_START` (launch and every
  return from background). That seeds the DB if empty (`DatabaseSeeder`: one "Cash" account + predefined
  categories), generates missed recurring transactions, posts one summary notification, and reschedules the OS
  reminder. OS alarms are best-effort only; the foreground sync is the source of truth. `LockScreen` fires the
  biometric prompt on `ON_RESUME` (once per lock), not in a `LaunchedEffect`.
- **Theme**: `FinanceTheme` provides Material 3 plus `LocalFinanceColors` (income/expense/transfer colours via
  `MaterialTheme.finance`). Use those, not hardcoded greens/reds. Extra icons are hand-drawn paths in
  `ui/theme/AppIcons.kt`; do not add the extended material-icons pack.

### Data model rules that affect any change

- Amounts are `Long` minor units (paise), always positive in the DB; sign comes from `type`. `Money` does
  parsing and Indian-grouped formatting.
- A transfer is two `TransactionEntity` rows (`TRANSFER_OUT` + `TRANSFER_IN`) sharing `transferId`, each with
  `counterpartAccountId`. Transfers never count toward income/expense; SQL totals exclude them explicitly.
  `observeFiltered` without an account filter and `observeRecent` drop the `TRANSFER_IN` leg so a transfer is
  listed once; `observeByAccount` and account-filtered lists keep that account's leg.
- Balances, monthly totals, category totals and budget progress are computed in SQL, never stored.
- Delete semantics: account with history -> archived and its recurring rules paused, empty account -> hard
  deleted with its rules; category -> transactions and rules moved to that type's "Uncategorized" and its budget
  removed; transaction -> returns a `DeletedEntry` snapshot for Undo, restore inserts fresh rows.
- Monthly recurrence uses `Frequency.next(from, anchor = rule.startDate)` so the day of month never drifts.

### Schema changes (SQLDelight, `verifyMigrations = true`)

Schema files: `composeApp/src/commonMain/sqldelight/com/codigitech/ft/db/*.sq`, migrations in
`db/migrations/N.sqm`, snapshots in `sqldelight/databases/N.db`. Current version is 2.

1. Edit the `.sq` table definition and add `migrations/<current>.sqm` with the ALTER/CREATE statements.
2. Run `./gradlew :composeApp:generateCommonMainFinanceDatabaseSchema` in its own Gradle invocation (Gradle 9
   reports an implicit-dependency error if it is combined with verify/compile tasks).
3. Commit the new `databases/<current+1>.db`. The build fails if the migration does not reproduce the snapshot.
4. Add a mapper in `Mappers.kt`, update the repository, and cover it in `androidUnitTest/.../DatabaseTest.kt`,
   which runs the real schema on an in-memory JDBC driver.

## Testing conventions

- `commonTest`: use-case tests against `Fake*Repository` classes in `fakes/Fakes.kt` (StateFlow-backed lists)
  with `FixedDate`. Add fake methods when you extend a repository interface, or the fakes stop compiling.
- `androidUnitTest`: `DatabaseTest` builds `FinanceDatabase` on `JdbcSqliteDriver.IN_MEMORY` and exercises the
  real repository impls. Put SQL-behaviour tests (totals, transfers, filters, cascades) here.
- No UI tests exist. Verify screens on the `FinanceTracker` emulator.
