package com.codigitech.ft.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.codigitech.ft.data.db.DbExecutor
import com.codigitech.ft.db.FinanceDatabase
import com.codigitech.ft.domain.model.Budget
import com.codigitech.ft.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(private val db: FinanceDatabase, private val exec: DbExecutor) : BudgetRepository {
    private val q get() = db.budgetQueries

    override fun observeAll(): Flow<List<Budget>> =
        q.selectAll().asFlow().mapToList(exec.dispatcher).map { rows -> rows.map { Budget(it.categoryId, it.monthlyLimit) } }

    override suspend fun set(categoryId: Long, monthlyLimit: Long) = exec.exec { q.upsert(categoryId, monthlyLimit) }

    override suspend fun remove(categoryId: Long) = exec.exec { q.deleteByCategory(categoryId) }
}
