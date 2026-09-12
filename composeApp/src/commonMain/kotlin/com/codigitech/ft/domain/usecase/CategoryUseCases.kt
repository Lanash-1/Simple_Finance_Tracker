package com.codigitech.ft.domain.usecase

import com.codigitech.ft.domain.model.CategoryType
import com.codigitech.ft.domain.repository.BudgetRepository
import com.codigitech.ft.domain.repository.CategoryRepository
import com.codigitech.ft.domain.repository.RecurringRuleRepository
import com.codigitech.ft.domain.repository.TransactionRepository

class AddCategoryUseCase(private val categories: CategoryRepository) {
    suspend operator fun invoke(name: String, icon: String, colorHex: String, type: CategoryType): Result<Long> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.Failure("Name is required")
        return Result.Success(categories.add(trimmed, icon, colorHex, type, isCustom = true, isDefault = false))
    }
}

class UpdateCategoryUseCase(private val categories: CategoryRepository) {
    suspend operator fun invoke(id: Long, name: String, icon: String, colorHex: String): Result<Unit> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.Failure("Name is required")
        categories.update(id, trimmed, icon, colorHex)
        return Result.Success(Unit)
    }
}

/** Deletes a category; its transactions and recurring rules move to the built-in Uncategorized of the same type. Never blocked. */
class DeleteCategoryUseCase(
    private val categories: CategoryRepository,
    private val transactions: TransactionRepository,
    private val budgets: BudgetRepository,
    private val rules: RecurringRuleRepository,
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        val category = categories.getById(id) ?: return Result.Success(Unit)
        if (category.isDefault) return Result.Failure("The Uncategorized category cannot be deleted")
        val fallback = categories.getDefault(category.type)
            ?: return Result.Failure("Missing default category for ${category.type.label}")
        transactions.inTransaction {
            transactions.reassignCategory(id, fallback.id)
            rules.reassignCategory(id, fallback.id)
            budgets.remove(id)
            categories.delete(id)
        }
        return Result.Success(Unit)
    }
}
