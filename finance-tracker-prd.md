# Product Requirements Document: Personal Finance Tracker

**Status:** Draft v3
**Owner:** Lan
**Type:** Personal project (learning-focused)

---

## 1. Overview

A cross-platform personal finance tracker for manual expense/income tracking across multiple accounts (cash, bank, card), built with Kotlin Multiplatform and Compose Multiplatform for Android and iOS. Fully offline, single-currency, no cloud dependency.

## 2. Background & Problem Statement

Manually tracking money across multiple physical/logical accounts (cash on hand, bank, credit card) is hard to keep in sync using notes or spreadsheets — balances drift, transfers get double-counted, and recurring bills are easy to forget. This project builds a lightweight, private, offline tracker that keeps balances accurate and reminds the owner about recurring commitments, while doubling as a hands-on Kotlin Multiplatform / Compose Multiplatform learning project.

## 3. Goals

- Track income, expenses, and transfers across multiple personal accounts
- Provide a clear at-a-glance view of balances and monthly totals
- Support recurring transactions (rent, subscriptions) with reminders
- Serve as a learning project for KMP/Compose Multiplatform architecture
- Keep the UI minimal and clean — design polish is not a priority

## 4. Non-Goals (Out of Scope for v1)

- Budgets or spending goals
- Cloud sync / backup
- Multi-currency support
- Receipt/photo attachments
- Bank account integration (Plaid or similar)
- Deep analytics (trends, comparisons, forecasting)

## 5. Target Users

- Primary and only user: the app owner (personal-use project, not intended for public distribution)
- Single-user app — no concept of multiple user accounts or shared data within the app itself

## 6. Target Platforms

- Android and iOS, both from v1
- Fully offline — no network dependency at any point

## 7. Tech Stack

| Concern | Choice |
|---|---|
| UI framework | Compose Multiplatform |
| Local database | SQLDelight |
| Dependency injection | Koin |
| Async | Kotlin Coroutines + Flow |
| Navigation | Compose Multiplatform Navigation |
| Notifications | expect/actual: AlarmManager + NotificationManager (Android), UNUserNotificationCenter (iOS) |
| App lock | expect/actual: BiometricPrompt (Android), LocalAuthentication (iOS) |
| Date/time | kotlinx-datetime |
| Local settings | multiplatform-settings |

## 8. User Stories

- As the user, I want to add an account so I can track balances for cash, bank, and card separately.
- As the user, I want to log an income or expense transaction so my account balance stays accurate.
- As the user, I want to transfer money between my own accounts so moving cash to bank doesn't get miscounted as income/expense.
- As the user, I want to assign categories to transactions so I can see where money goes (even without charts in v1, the data should be structured for it later).
- As the user, I want to define a recurring transaction (e.g. rent) so I don't have to manually re-enter it every month.
- As the user, I want a notification when a recurring transaction is generated so I'm aware it happened.
- As the user, I want to optionally lock the app with a PIN/biometric so my financial data isn't casually visible if someone picks up my phone.
- As the user, I want to search/filter my transaction history so I can find a specific past entry quickly.

## 9. Screens / UX Flow

1. **Dashboard** — combined balance across accounts, per-account balances, current month income/expense/net totals, recent transactions
2. **Accounts** — list accounts; add/edit/delete; tap into an account to see its balance and transaction history
3. **Add/Edit Transaction** — amount, type (income/expense/transfer), account(s), category, date, optional note
4. **Transaction List** — full history; filter by account, category, date range; search by note/amount
5. **Categories** — view predefined categories; add/edit/delete custom ones
6. **Recurring Transactions** — list active rules; add/edit/pause/delete; shows next due date
7. **Settings** — toggle app lock, theme, (future: currency symbol display)

**Primary flow:** Dashboard → Add Transaction → back to Dashboard (updated totals). This should be the fastest path in the app since it's the most frequent action.

## 10. Functional Requirements

### 10.1 Accounts
- User can create, edit, and delete accounts (cash, bank, card, or custom type)
- Each account has an initial balance and a computed running balance
- Dashboard shows per-account balances and a combined total
- Deleting an account with existing transactions is a soft delete: the account is archived (hidden from active account list and new-transaction entry) but its transaction history remains intact and viewable

### 10.2 Transactions
- User can add income or expense transactions with: amount, account, category, date, optional note
- User can edit and delete transactions
- Transaction list supports filtering by account, category, and date range, plus search

### 10.3 Transfers
- User can transfer funds between their own accounts (e.g., cash → bank)
- Transfers must not be counted as income or expense in totals, but must correctly adjust both account balances

### 10.4 Categories
- App ships with predefined income/expense categories
- User can create, edit, and delete custom categories
- Each category has a name, icon, color, and type (income/expense)
- Deleting a category reassigns any transactions using it to a built-in default "Uncategorized" category; deletion is never blocked

### 10.5 Recurring Transactions
- User can define a recurring rule (amount, account, category, type, frequency: daily/weekly/monthly, start date)
- On every app launch, the app syncs all recurring rules: for each rule with a `nextDueDate` on or before today, it generates the missed transaction(s), advances `nextDueDate` forward until it's caught up to today, and repeats for all rules
- If more than one transaction is generated during a sync, the user gets a single summary notification (e.g., "3 recurring transactions added") rather than one per transaction
- OS-level background scheduling (AlarmManager / UNUserNotificationCenter) is a secondary, best-effort trigger only — the app-launch sync is the primary mechanism, since background reliability (especially on iOS) can't be guaranteed for a project with no push infrastructure
- User can pause/resume or delete a recurring rule

### 10.6 Reporting (v1 scope)
- Dashboard shows total balance, and monthly income/expense/net totals
- No charts, trends, or category breakdowns in v1

### 10.7 Security
- Optional PIN or biometric app lock, toggled in Settings
- Disabled by default

### 10.8 Settings
- Enable/disable app lock
- Theme (light/dark, following Material 3 minimal styling)

## 11. Data Model

**Account**: id, name, type, initialBalance, createdAt, isArchived

**Category**: id, name, icon, colorHex, type (income/expense), isCustom, isDefault *(a built-in "Uncategorized" category exists per type and cannot be deleted)*

**Transaction**: id, accountId, categoryId (nullable for transfers), type (income/expense/transfer), amount, date, note, createdAt

**Transfer**: id, fromAccountId, toAccountId, amount, date, note *(or modeled as a linked transaction pair)*

**RecurringRule**: id, amount, accountId, categoryId, type, frequency, startDate, nextDueDate, isActive

*Balances and monthly totals are computed on the fly from transactions/transfers, not stored redundantly.*

## 12. Architecture

- **data layer**: SQLDelight queries, repository implementations, mappers
- **domain layer**: repository interfaces, use cases (`AddTransactionUseCase`, `CalculateBalanceUseCase`, `ProcessDueRecurringUseCase`, etc.)
- **presentation layer**: shared ViewModels (commonMain) + Compose UI shared across platforms; expect/actual only for notifications and biometrics

## 13. Non-Functional Requirements

- Fully offline — no data leaves the device
- Responsive on both phone form factors (Android + iOS)
- Balance calculations must remain consistent across transfers (no double-counting)
- App should remain usable with a few thousand transactions without noticeable UI lag

## 14. Assumptions & Constraints

- Single device usage per install — no expectation of multi-device consistency since there's no sync
- User is the sole developer and sole end user, so onboarding/tutorial flows are not required
- No backend/server component — 100% client-side app
- Development time is bounded by personal availability, not a fixed deadline

## 15. Dependencies

- SQLDelight (local persistence)
- Koin (DI)
- kotlinx-datetime (date handling)
- multiplatform-settings (lightweight key-value storage for settings/app-lock flag)
- Platform notification and biometric APIs (accessed via expect/actual, no external library required beyond platform SDKs)

## 16. Error Handling & Edge Cases

- **Deleting an account with existing transactions:** soft delete (archive) — account is hidden from active lists and new-transaction entry, history remains intact and viewable
- **Deleting a category in use:** transactions are reassigned to a built-in "Uncategorized" default; deletion is never blocked
- **Negative balances:** allowed (e.g., credit card debt) — no validation error, but UI should visually distinguish negative balances
- **Invalid transaction input:** amount must be a positive number; date required; validation errors shown inline before save
- **Missed recurring due dates** (app not opened for a while): resolved via app-launch sync — see section 10.5
- **Transfer to the same account:** disallowed, validation error shown

## 17. Data Migration & Versioning Strategy

- SQLDelight schema changes must ship with explicit migration scripts (SQLDelight supports versioned `.sqm` migration files)
- Since there's no cloud backup, a local schema migration failure risks data loss — migrations should be tested against a copy of real local data before release
- App version and DB schema version should be tracked independently to catch mismatches

## 18. Release Plan

- No public app store distribution planned for v1 — sideloaded/local builds (Android APK, iOS via Xcode/TestFlight for personal device)
- Versioning: simple semantic versioning (e.g., 0.1.0) for personal tracking of progress, no formal changelog required
- No staged rollout needed given single-user, non-store distribution

## 19. Success Metrics

Since this is a personal/learning project rather than a product with users to measure, success is defined by:
- Functional completeness against the v1 scope in this PRD
- Actually using the app for real personal tracking for at least one full month without falling back to a spreadsheet
- Achieving a working shared Compose Multiplatform codebase across Android and iOS with minimal platform-specific code outside the expect/actual boundaries

## 20. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| KMP/Compose Multiplatform learning curve slows progress | Build phases start with a minimal foundation/proof-of-concept before full feature work |
| Balance/transfer logic bugs cause incorrect totals | Prioritize unit tests on balance calculation and transfer logic early |
| Local-only storage means a lost/reset device loses all data | Accepted risk for v1 given "no cloud" decision; revisit if data loss becomes a real concern |
| iOS-specific issues discovered late (notifications, biometrics) | Include an explicit iOS verification phase rather than treating iOS as an afterthought |

## 21. Accessibility & Localization

- Localization: out of scope for v1 — single language (assumed English), no i18n infrastructure planned
- Accessibility: basic Compose accessibility semantics (content descriptions, adequate touch targets) should be followed as good practice, but no dedicated accessibility testing pass is planned given single-user scope

## 22. Milestones / Build Phases

1. **Foundation** — KMP project setup, SQLDelight schema, Koin DI, shared ViewModel proof-of-concept on both platforms
2. **Accounts + Transactions core** — CRUD for accounts and transactions, dashboard totals
3. **Transfers** — implement inter-account transfer logic
4. **Categories** — predefined seed data + custom category CRUD
5. **Recurring transactions** — rule engine + due-date processing
6. **Notifications** — platform-specific reminder scheduling
7. **App lock** — optional PIN/biometric via expect/actual
8. **Polish** — minimal Material 3 theming, empty states, validation, dark mode
9. **iOS verification pass** — confirm rendering and platform-specific features (notifications, biometrics) on real iOS device/simulator

## 23. Testing Plan

- Unit tests (commonMain): use cases, balance calculations, recurring due-date logic
- SQLDelight query tests
- Manual QA on both Android and iOS, especially platform-specific features

## 24. Open Questions

All prior open questions have been resolved (see sections 10.1, 10.4, 10.5, 11, and 16). None outstanding as of v3.

## 25. Revision History

| Version | Change |
|---|---|
| v1 | Initial draft: overview, goals, tech stack, functional requirements, data model, architecture, phases |
| v2 | Added background/problem statement, user stories, screens/UX flow, target users, assumptions & constraints, dependencies, error handling & edge cases, data migration strategy, release plan, success metrics, risks & mitigations, accessibility/localization stance, revision history |
| v3 | Resolved all open questions: balance computed live (not cached); recurring transactions synced on app launch with summary notification; account deletion is soft-delete/archive; category deletion reassigns to built-in "Uncategorized" |
