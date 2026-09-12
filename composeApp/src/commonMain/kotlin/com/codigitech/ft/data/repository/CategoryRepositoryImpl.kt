package com.codigitech.ft.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.codigitech.ft.data.db.DbExecutor
import com.codigitech.ft.data.db.toDb
import com.codigitech.ft.data.db.toDomain
import com.codigitech.ft.db.FinanceDatabase
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.CategoryType
import com.codigitech.ft.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(private val db: FinanceDatabase, private val exec: DbExecutor) : CategoryRepository {
    private val q get() = db.categoryQueries

    override fun observeAll(): Flow<List<Category>> =
        q.selectAll().asFlow().mapToList(exec.dispatcher).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAll(): List<Category> = exec.run { q.selectAll().executeAsList().map { it.toDomain() } }

    override suspend fun getById(id: Long): Category? = exec.run { q.selectById(id).executeAsOneOrNull()?.toDomain() }

    override suspend fun getDefault(type: CategoryType): Category? =
        exec.run { q.selectDefaultForType(type.name).executeAsOneOrNull()?.toDomain() }

    override suspend fun add(name: String, icon: String, colorHex: String, type: CategoryType, isCustom: Boolean, isDefault: Boolean): Long =
        exec.run {
            q.insert(name, icon, colorHex, type.name, isCustom.toDb(), isDefault.toDb())
            q.lastInsertRowId().executeAsOne()
        }

    override suspend fun update(id: Long, name: String, icon: String, colorHex: String) = exec.exec { q.update(name, icon, colorHex, id) }

    override suspend fun delete(id: Long) = exec.exec { q.deleteById(id) }

    override suspend fun count(): Long = exec.run { q.countAll().executeAsOne() }
}
