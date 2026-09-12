package com.codigitech.ft.domain.usecase

import com.codigitech.ft.domain.model.Frequency
import com.codigitech.ft.domain.model.RecurringRule
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.repository.RecurringRuleRepository
import com.codigitech.ft.domain.repository.TransactionRepository
import kotlinx.datetime.LocalDate

class AddRecurringRuleUseCase(private val rules: RecurringRuleRepository) {
    suspend operator fun invoke(
        amount: Long,
        accountId: Long,
        categoryId: Long?,
        type: TransactionType,
        frequency: Frequency,
        startDate: LocalDate,
        note: String?,
    ): Result<Long> {
        if (type.isTransfer) return Result.Failure("Recurring transfers are not supported")
        if (amount <= 0) return Result.Failure("Amount must be greater than zero")
        return Result.Success(rules.add(amount, accountId, categoryId, type, frequency, startDate, note?.trim()?.ifBlank { null }))
    }
}

class UpdateRecurringRuleUseCase(private val rules: RecurringRuleRepository) {
    suspend operator fun invoke(
        id: Long,
        amount: Long,
        accountId: Long,
        categoryId: Long?,
        type: TransactionType,
        frequency: Frequency,
        startDate: LocalDate,
        note: String?,
    ): Result<Unit> {
        if (type.isTransfer) return Result.Failure("Recurring transfers are not supported")
        if (amount <= 0) return Result.Failure("Amount must be greater than zero")
        val existing = rules.getById(id) ?: return Result.Failure("Rule not found")
        // Keep progress unless the schedule moved: if the new start date is after the
        // current next-due date, restart from the new start date.
        val nextDue = if (startDate > existing.nextDueDate || existing.startDate != startDate) startDate else existing.nextDueDate
        rules.update(id, amount, accountId, categoryId, type, frequency, startDate, nextDue, note?.trim()?.ifBlank { null })
        return Result.Success(Unit)
    }
}

/** Pausing keeps nextDueDate; resuming skips everything missed and lands on the next occurrence on/after today. */
class ToggleRecurringRuleUseCase(private val rules: RecurringRuleRepository, private val dates: DateProvider) {
    suspend operator fun invoke(id: Long, active: Boolean) {
        val rule = rules.getById(id) ?: return
        val nextDue = if (active) skipToOnOrAfter(rule.nextDueDate, rule.frequency, dates.today(), rule.startDate) else rule.nextDueDate
        rules.setActive(id, active, nextDue)
    }

    companion object {
        fun skipToOnOrAfter(from: LocalDate, frequency: Frequency, today: LocalDate, anchor: LocalDate = from): LocalDate {
            var d = from
            while (d < today) d = frequency.next(d, anchor)
            return d
        }
    }
}

class DeleteRecurringRuleUseCase(private val rules: RecurringRuleRepository) {
    suspend operator fun invoke(id: Long) = rules.delete(id)
}

/**
 * App-launch sync: for each active rule with nextDueDate <= today, generate every
 * missed occurrence and advance nextDueDate past today. Returns how many were created.
 */
class ProcessDueRecurringUseCase(
    private val rules: RecurringRuleRepository,
    private val transactions: TransactionRepository,
    private val dates: DateProvider,
) {
    data class Outcome(val generated: Int, val rulesTouched: Int)

    suspend operator fun invoke(): Outcome {
        val today = dates.today()
        val due = rules.getDue(today)
        var generated = 0
        for (rule in due) {
            generated += transactions.inTransaction { process(rule, today) }
        }
        return Outcome(generated, due.size)
    }

    private suspend fun process(rule: RecurringRule, today: LocalDate): Int {
        var count = 0
        var next = rule.nextDueDate
        // Safety cap so a rule with a start date years ago cannot loop forever on a bad clock.
        while (next <= today && count < MAX_PER_RULE) {
            transactions.add(
                accountId = rule.accountId,
                categoryId = rule.categoryId,
                type = rule.type,
                amount = rule.amount,
                date = next,
                note = rule.note,
                recurringRuleId = rule.id,
            )
            next = rule.frequency.next(next, rule.startDate)
            count++
        }
        rules.setNextDueDate(rule.id, next)
        return count
    }

    private companion object {
        const val MAX_PER_RULE = 3660
    }
}
