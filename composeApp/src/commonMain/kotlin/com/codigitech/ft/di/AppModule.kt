package com.codigitech.ft.di

import com.codigitech.ft.data.db.DatabaseDriverFactory
import com.codigitech.ft.data.db.DatabaseSeeder
import com.codigitech.ft.data.db.DbExecutor
import com.codigitech.ft.data.repository.AccountRepositoryImpl
import com.codigitech.ft.data.repository.BudgetRepositoryImpl
import com.codigitech.ft.data.repository.CategoryRepositoryImpl
import com.codigitech.ft.data.repository.RecurringRuleRepositoryImpl
import com.codigitech.ft.data.repository.TransactionRepositoryImpl
import com.codigitech.ft.db.FinanceDatabase
import com.codigitech.ft.domain.repository.AccountRepository
import com.codigitech.ft.domain.repository.BudgetRepository
import com.codigitech.ft.domain.repository.CategoryRepository
import com.codigitech.ft.domain.repository.RecurringRuleRepository
import com.codigitech.ft.domain.repository.TransactionRepository
import com.codigitech.ft.domain.usecase.AddAccountUseCase
import com.codigitech.ft.domain.usecase.AddCategoryUseCase
import com.codigitech.ft.domain.usecase.AddRecurringRuleUseCase
import com.codigitech.ft.domain.usecase.AddTransactionUseCase
import com.codigitech.ft.domain.usecase.CalculateBalanceUseCase
import com.codigitech.ft.domain.usecase.DateProvider
import com.codigitech.ft.domain.usecase.DeleteAccountUseCase
import com.codigitech.ft.domain.usecase.DeleteCategoryUseCase
import com.codigitech.ft.domain.usecase.DeleteRecurringRuleUseCase
import com.codigitech.ft.domain.usecase.DeleteTransactionUseCase
import com.codigitech.ft.domain.usecase.ExportTransactionsCsvUseCase
import com.codigitech.ft.domain.usecase.ProcessDueRecurringUseCase
import com.codigitech.ft.domain.usecase.RestoreAccountUseCase
import com.codigitech.ft.domain.usecase.RestoreTransactionUseCase
import com.codigitech.ft.domain.usecase.SetBudgetUseCase
import com.codigitech.ft.domain.usecase.SystemDateProvider
import com.codigitech.ft.domain.usecase.ToggleRecurringRuleUseCase
import com.codigitech.ft.domain.usecase.TransferUseCase
import com.codigitech.ft.domain.usecase.UpdateAccountUseCase
import com.codigitech.ft.domain.usecase.UpdateCategoryUseCase
import com.codigitech.ft.domain.usecase.UpdateRecurringRuleUseCase
import com.codigitech.ft.domain.usecase.UpdateTransactionUseCase
import com.codigitech.ft.platform.AppLockManager
import com.codigitech.ft.platform.AppSettings
import com.codigitech.ft.platform.RecurringSyncCoordinator
import com.codigitech.ft.ui.accounts.AccountDetailViewModel
import com.codigitech.ft.ui.accounts.AccountsViewModel
import com.codigitech.ft.ui.categories.CategoriesViewModel
import com.codigitech.ft.ui.dashboard.DashboardViewModel
import com.codigitech.ft.ui.insights.InsightsViewModel
import com.codigitech.ft.ui.lock.LockViewModel
import com.codigitech.ft.ui.recurring.RecurringViewModel
import com.codigitech.ft.ui.settings.SettingsViewModel
import com.codigitech.ft.ui.transactions.EditTransactionViewModel
import com.codigitech.ft.ui.transactions.TransactionListViewModel
import com.russhwolf.settings.Settings
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val dataModule = module {
    single { FinanceDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single { DbExecutor(get<FinanceDatabase>()) }
    single<AccountRepository> { AccountRepositoryImpl(get(), get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get(), get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get(), get()) }
    single<RecurringRuleRepository> { RecurringRuleRepositoryImpl(get(), get()) }
    single<BudgetRepository> { BudgetRepositoryImpl(get(), get()) }
    single { DatabaseSeeder(get(), get(), get()) }
    single { Settings() }
    single { AppSettings(get()) }
    single { AppLockManager(get(), get()) }
    single { RecurringSyncCoordinator(get(), get(), get(), get()) }
}

val domainModule = module {
    single<DateProvider> { SystemDateProvider() }
    factory { AddTransactionUseCase(get()) }
    factory { UpdateTransactionUseCase(get()) }
    factory { DeleteTransactionUseCase(get()) }
    factory { RestoreTransactionUseCase(get()) }
    factory { TransferUseCase(get()) }
    factory { AddAccountUseCase(get(), get()) }
    factory { UpdateAccountUseCase(get()) }
    factory { DeleteAccountUseCase(get(), get(), get()) }
    factory { RestoreAccountUseCase(get()) }
    factory { AddCategoryUseCase(get()) }
    factory { UpdateCategoryUseCase(get()) }
    factory { DeleteCategoryUseCase(get(), get(), get(), get()) }
    factory { AddRecurringRuleUseCase(get()) }
    factory { UpdateRecurringRuleUseCase(get()) }
    factory { ToggleRecurringRuleUseCase(get(), get()) }
    factory { DeleteRecurringRuleUseCase(get()) }
    factory { ProcessDueRecurringUseCase(get(), get(), get()) }
    factory { CalculateBalanceUseCase() }
    factory { SetBudgetUseCase(get()) }
    factory { ExportTransactionsCsvUseCase(get(), get(), get()) }
}

val presentationModule = module {
    viewModel { DashboardViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { InsightsViewModel(get(), get(), get(), get(), get()) }
    viewModel { AccountsViewModel(get(), get(), get(), get(), get()) }
    viewModel { (accountId: Long) -> AccountDetailViewModel(accountId, get(), get(), get(), get(), get(), get(), get()) }
    viewModel { EditTransactionViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { TransactionListViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { CategoriesViewModel(get(), get(), get(), get()) }
    viewModel { RecurringViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
    viewModel { LockViewModel(get()) }
}

/** Platform provides its own module with DatabaseDriverFactory, Notifier, RecurringScheduler, BiometricAuthenticator, FileSharer. */
fun initKoin(platformModule: Module, appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(platformModule, dataModule, domainModule, presentationModule)
}
