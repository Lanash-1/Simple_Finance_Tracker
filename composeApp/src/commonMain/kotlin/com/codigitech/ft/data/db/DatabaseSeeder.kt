package com.codigitech.ft.data.db

import com.codigitech.ft.domain.model.AccountType
import com.codigitech.ft.domain.model.CategoryType
import com.codigitech.ft.domain.model.DefaultCategories
import com.codigitech.ft.domain.repository.AccountRepository
import com.codigitech.ft.domain.repository.CategoryRepository
import com.codigitech.ft.domain.usecase.DateProvider

/** Seeds predefined categories (plus Uncategorized per type) and one Cash account on first launch. */
class DatabaseSeeder(
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val dates: DateProvider,
) {
    suspend fun seedIfEmpty() {
        if (categories.count() == 0L) {
            for (type in CategoryType.entries) {
                categories.add(
                    DefaultCategories.UNCATEGORIZED_NAME,
                    DefaultCategories.UNCATEGORIZED_ICON,
                    DefaultCategories.UNCATEGORIZED_COLOR,
                    type,
                    isCustom = false,
                    isDefault = true,
                )
            }
            DefaultCategories.expense.forEach { (name, icon, color) ->
                categories.add(name, icon, color, CategoryType.EXPENSE, isCustom = false, isDefault = false)
            }
            DefaultCategories.income.forEach { (name, icon, color) ->
                categories.add(name, icon, color, CategoryType.INCOME, isCustom = false, isDefault = false)
            }
        }
        if (accounts.count() == 0L) {
            accounts.add("Cash", AccountType.CASH, 0L, dates.today())
        }
    }
}
