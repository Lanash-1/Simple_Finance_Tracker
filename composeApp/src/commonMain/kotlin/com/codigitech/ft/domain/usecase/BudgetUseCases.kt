package com.codigitech.ft.domain.usecase

import com.codigitech.ft.domain.model.Budget
import com.codigitech.ft.domain.model.BudgetProgress
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.CategoryTotal
import com.codigitech.ft.domain.model.CategoryType
import com.codigitech.ft.domain.repository.BudgetRepository

/** A limit of zero (or less) clears the budget. */
class SetBudgetUseCase(private val budgets: BudgetRepository) {
    suspend operator fun invoke(categoryId: Long, monthlyLimit: Long) {
        if (monthlyLimit <= 0) budgets.remove(categoryId) else budgets.set(categoryId, monthlyLimit)
    }
}

/** Pure join of budgets, categories and this month's spend. Over-budget first, then by share used. */
object BudgetProgressCalculator {
    fun calculate(budgets: List<Budget>, categories: List<Category>, spend: List<CategoryTotal>): List<BudgetProgress> {
        val spent = spend.filter { it.categoryId != null }.associate { it.categoryId!! to it.total }
        val byId = categories.filter { it.type == CategoryType.EXPENSE }.associateBy { it.id }
        return budgets.mapNotNull { b ->
            val category = byId[b.categoryId] ?: return@mapNotNull null
            BudgetProgress(category, b.monthlyLimit, spent[b.categoryId] ?: 0L)
        }.sortedWith(compareByDescending<BudgetProgress> { it.isOver }.thenByDescending { it.fraction })
    }
}
