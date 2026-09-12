package com.codigitech.ft.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.codigitech.ft.data.db.DbExecutor
import com.codigitech.ft.data.db.toDb
import com.codigitech.ft.data.db.toDomain
import com.codigitech.ft.data.db.toEpochDay
import com.codigitech.ft.db.FinanceDatabase
import com.codigitech.ft.domain.model.Account
import com.codigitech.ft.domain.model.AccountType
import com.codigitech.ft.domain.model.AccountWithBalance
import com.codigitech.ft.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class AccountRepositoryImpl(private val db: FinanceDatabase, private val exec: DbExecutor) : AccountRepository {
    private val q get() = db.accountQueries

    override fun observeAll(): Flow<List<Account>> =
        q.selectAll().asFlow().mapToList(exec.dispatcher).map { rows -> rows.map { it.toDomain() } }

    override fun observeActive(): Flow<List<Account>> =
        q.selectActive().asFlow().mapToList(exec.dispatcher).map { rows -> rows.map { it.toDomain() } }

    override fun observeBalances(): Flow<List<AccountWithBalance>> {
        val balances = q.selectBalances().asFlow().mapToList(exec.dispatcher)
        return combine(observeAll(), balances) { accounts, bal ->
            val byId = bal.associate { it.accountId to it.balance.toLong() }
            accounts.map { AccountWithBalance(it, byId[it.id] ?: it.initialBalance) }
        }
    }

    override suspend fun getById(id: Long): Account? = exec.run { q.selectById(id).executeAsOneOrNull()?.toDomain() }

    override suspend fun add(name: String, type: AccountType, initialBalance: Long, createdAt: LocalDate): Long = exec.run {
        q.insert(name, type.name, initialBalance, createdAt.toEpochDay())
        q.lastInsertRowId().executeAsOne()
    }

    override suspend fun update(id: Long, name: String, type: AccountType, initialBalance: Long) = exec.exec {
        q.update(name, type.name, initialBalance, id)
    }

    override suspend fun setArchived(id: Long, archived: Boolean) = exec.exec { q.setArchived(archived.toDb(), id) }

    override suspend fun deleteHard(id: Long) = exec.exec { q.deleteById(id) }

    override suspend fun count(): Long = exec.run { q.countAll().executeAsOne() }
}
