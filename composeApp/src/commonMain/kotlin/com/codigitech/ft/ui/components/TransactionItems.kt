package com.codigitech.ft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codigitech.ft.domain.model.Account
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.domain.model.Transaction
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.ui.theme.AppIcons
import com.codigitech.ft.ui.theme.expenseColor
import com.codigitech.ft.ui.theme.finance
import com.codigitech.ft.ui.theme.incomeColor
import com.codigitech.ft.ui.theme.parseHexColor
import com.codigitech.ft.ui.theme.tabular
import com.codigitech.ft.ui.theme.transferColor
import kotlinx.datetime.LocalDate

/** Transactions of one calendar day plus that day's net of income and expense (transfers excluded). */
data class DaySection(val date: LocalDate, val items: List<Transaction>) {
    val net: Long = items.sumOf {
        when (it.type) {
            TransactionType.INCOME -> it.amount
            TransactionType.EXPENSE -> -it.amount
            else -> 0L
        }
    }
}

fun List<Transaction>.groupByDay(): List<DaySection> =
    groupBy { it.date }.entries.sortedByDescending { it.key }.map { DaySection(it.key, it.value) }

@Composable
fun CategoryDot(category: Category?, size: Int = 40, transfer: Boolean = false) {
    val color = when {
        transfer -> transferColor()
        category != null -> parseHexColor(category.colorHex)
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier.size(size.dp).background(color.copy(alpha = 0.16f), MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        if (transfer) {
            Icon(AppIcons.SwapHoriz, contentDescription = null, tint = color, modifier = Modifier.size((size * 0.55f).dp))
        } else {
            Text(category?.icon ?: "?", style = if (size >= 40) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun DayHeader(section: DaySection, today: LocalDate, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            section.date.relativeLabel(today),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (section.net != 0L) {
            Text(
                Money.format(section.net, withSign = true),
                style = MaterialTheme.typography.labelMedium.tabular,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    account: Account?,
    counterpart: Account?,
    category: Category?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = false,
) {
    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> incomeColor()
        TransactionType.EXPENSE -> expenseColor()
        else -> transferColor()
    }
    val title = when (transaction.type) {
        TransactionType.TRANSFER_OUT -> "To ${counterpart?.name ?: "?"}"
        TransactionType.TRANSFER_IN -> "From ${counterpart?.name ?: "?"}"
        else -> transaction.note?.takeIf { it.isNotBlank() } ?: category?.name ?: "Uncategorized"
    }
    val subtitleParts = buildList {
        if (transaction.type.isTransfer) add("Transfer") else if (transaction.note?.isNotBlank() == true) add(category?.name ?: "Uncategorized")
        account?.let { add(it.name) }
        if (showDate) add(transaction.date.display())
    }
    val prefix = when (transaction.type) {
        TransactionType.INCOME, TransactionType.TRANSFER_IN -> "+"
        else -> "−"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryDot(category, transfer = transaction.type.isTransfer)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (transaction.recurringRuleId != null) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Refresh, contentDescription = "Recurring", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                }
                if (subtitleParts.isNotEmpty()) {
                    Text(
                        subtitleParts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "$prefix${Money.format(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge.tabular,
                color = amountColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Small coloured pill: "Expense", "Income", "Transfer". */
@Composable
fun TypePill(type: TransactionType, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (type) {
        TransactionType.INCOME -> Triple(MaterialTheme.finance.incomeContainer, MaterialTheme.finance.onIncomeContainer, "Income")
        TransactionType.EXPENSE -> Triple(MaterialTheme.finance.expenseContainer, MaterialTheme.finance.onExpenseContainer, "Expense")
        else -> Triple(MaterialTheme.finance.transferContainer, MaterialTheme.finance.onTransferContainer, "Transfer")
    }
    Pill(label, bg, fg, modifier)
}

@Composable
fun Pill(label: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Text(
        label,
        modifier = modifier.background(background, MaterialTheme.shapes.extraSmall).padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = foreground,
    )
}
