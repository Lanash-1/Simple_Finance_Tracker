package com.codigitech.ft.data.db

import app.cash.sqldelight.Transacter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Serialises all database access onto one thread. SQLDelight transactions are
 * thread-bound, so nested calls made inside [transaction] must stay on that thread:
 * the [InTransaction] marker tells [run] to execute inline instead of re-dispatching.
 */
class DbExecutor(private val transacter: Transacter) {
    // Dispatchers.IO is JVM/Android-only in common code; Default is available on every target.
    val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)

    private class InTransaction : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<InTransaction>
    }

    suspend fun <T> run(block: () -> T): T =
        if (coroutineContext[InTransaction] != null) block() else withContext(dispatcher) { block() }

    /** For statements whose SQLDelight return value (QueryResult) the caller does not care about. */
    suspend fun exec(block: () -> Any?) {
        run(block)
    }

    suspend fun <T> transaction(block: suspend () -> T): T =
        if (coroutineContext[InTransaction] != null) {
            block()
        } else {
            withContext(dispatcher) {
                transacter.transactionWithResult {
                    runBlocking(InTransaction()) { block() }
                }
            }
        }
}
