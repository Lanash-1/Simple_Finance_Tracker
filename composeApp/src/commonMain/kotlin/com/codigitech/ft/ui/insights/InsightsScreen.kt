package com.codigitech.ft.ui.insights

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codigitech.ft.domain.model.BudgetProgress
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.ui.components.AmountField
import com.codigitech.ft.ui.components.BarSegment
import com.codigitech.ft.ui.components.BreakdownRow
import com.codigitech.ft.ui.components.BudgetBar
import com.codigitech.ft.ui.components.CategoryDot
import com.codigitech.ft.ui.components.DropdownField
import com.codigitech.ft.ui.components.EmptyState
import com.codigitech.ft.ui.components.MonthBar
import com.codigitech.ft.ui.components.MonthSwitcher
import com.codigitech.ft.ui.components.MonthlyBarChart
import com.codigitech.ft.ui.components.SectionHeader
import com.codigitech.ft.ui.components.SegmentedBar
import com.codigitech.ft.ui.components.StatColumn
import com.codigitech.ft.ui.components.SurfaceCard
import com.codigitech.ft.ui.components.VSpace
import com.codigitech.ft.ui.components.monthShort
import com.codigitech.ft.ui.dashboard.CategoryShare
import com.codigitech.ft.ui.theme.AppIcons
import com.codigitech.ft.ui.theme.expenseColor
import com.codigitech.ft.ui.theme.incomeColor
import com.codigitech.ft.ui.theme.parseHexColor
import com.codigitech.ft.ui.theme.tabular
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: InsightsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingBudget by remember { mutableStateOf<BudgetProgress?>(null) }
    var addingBudget by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Insights") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        },
    ) { padding ->
        val month = state.month
        if (state.loading || month == null) return@Scaffold
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item(key = "switcher") {
                MonthSwitcher(month, viewModel::previousMonth, viewModel::nextMonth, canGoNext = !state.isCurrentMonth, onReset = viewModel::resetMonth, modifier = Modifier.padding(horizontal = 8.dp))
            }

            item(key = "trend") {
                SurfaceCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Cash flow", style = MaterialTheme.typography.titleMedium)
                        Text("Last ${InsightsState.TREND_MONTHS} months · tap a month to inspect it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        VSpace(16)
                        MonthlyBarChart(
                            state.trend.map { MonthBar(it.month.monthShort(), it.income, it.expense, highlighted = it.month == month) },
                            onSelect = viewModel::selectTrendIndex,
                        )
                        VSpace(16)
                        val t = state.totals
                        Row(Modifier.fillMaxWidth()) {
                            StatColumn("Income", t?.income ?: 0, incomeColor(), Modifier.weight(1f))
                            StatColumn("Expense", t?.expense ?: 0, expenseColor(), Modifier.weight(1f))
                            StatColumn("Net", t?.net ?: 0, if ((t?.net ?: 0) < 0) expenseColor() else MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                        }
                    }
                }
            }

            item(key = "budgets-header") {
                SectionHeader("Budgets", action = {
                    if (state.unbudgeted.isNotEmpty()) TextButton(onClick = { addingBudget = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Add")
                    }
                })
            }
            item(key = "budgets") {
                SurfaceCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = if (state.budgets.isEmpty()) ({ addingBudget = true }) else null) {
                    if (state.budgets.isEmpty()) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(AppIcons.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            VSpace(8)
                            Text("Set a monthly limit", style = MaterialTheme.typography.titleSmall)
                            Text("Pick a category and we'll track how much of the limit you have used.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    } else {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            state.budgets.forEach { b -> BudgetRow(b, onClick = { editingBudget = b }) }
                        }
                    }
                }
            }

            item(key = "spending-header") { SectionHeader("Where it went") }
            item(key = "spending") {
                BreakdownCard(state.spending, emptyText = "No expenses in ${month.monthShort()}")
            }

            if (state.income.isNotEmpty()) {
                item(key = "income-header") { SectionHeader("Where it came from") }
                item(key = "income") { BreakdownCard(state.income, emptyText = "") }
            }

            if (state.highlights.isNotEmpty()) {
                item(key = "highlights-header") { SectionHeader("Highlights") }
                item(key = "highlights") {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.highlights.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                pair.forEach { h ->
                                    SurfaceCard(Modifier.weight(1f)) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text(h.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            VSpace(4)
                                            Text(h.value, style = MaterialTheme.typography.titleLarge.tabular, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            h.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                        }
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    editingBudget?.let { b ->
        BudgetDialog(category = b.category, current = b.limit, onDismiss = { editingBudget = null }) { limit ->
            viewModel.saveBudget(b.category.id, limit); editingBudget = null
        }
    }
    if (addingBudget) {
        var chosen by remember { mutableStateOf(state.unbudgeted.firstOrNull()) }
        val category = chosen
        if (category == null) {
            addingBudget = false
        } else {
            BudgetDialog(
                category = category,
                current = null,
                onDismiss = { addingBudget = false },
                categoryPicker = { DropdownField("Category", state.unbudgeted, category, { "${it.icon} ${it.name}" }, { chosen = it }) },
            ) { limit -> viewModel.saveBudget(category.id, limit); addingBudget = false }
        }
    }
}

@Composable
private fun BudgetRow(b: BudgetProgress, onClick: () -> Unit) {
    val tint = parseHexColor(b.category.colorHex)
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryDot(b.category, size = 32)
            Spacer(Modifier.width(10.dp))
            Text(b.category.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (b.isOver) "${Money.format(-b.remaining)} over" else "${Money.format(b.remaining)} left",
                style = MaterialTheme.typography.labelMedium.tabular,
                color = if (b.isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        VSpace(8)
        BudgetBar(b.fraction, tint)
        VSpace(4)
        Text(
            "${Money.format(b.spent)} of ${Money.format(b.limit)}",
            style = MaterialTheme.typography.labelSmall.tabular,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BreakdownCard(shares: List<CategoryShare>, emptyText: String) {
    SurfaceCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (shares.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val neutral = MaterialTheme.colorScheme.outline
            Column(Modifier.padding(16.dp)) {
                SegmentedBar(shares.map { BarSegment(it.category?.name ?: "Uncategorized", it.total, it.category?.let { c -> parseHexColor(c.colorHex) } ?: neutral) })
                VSpace(10)
                shares.forEach { s ->
                    BreakdownRow(
                        color = s.category?.let { parseHexColor(it.colorHex) } ?: neutral,
                        icon = s.category?.icon,
                        name = s.category?.name ?: "Uncategorized",
                        amount = s.total,
                        share = s.share,
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetDialog(
    category: Category,
    current: Long?,
    onDismiss: () -> Unit,
    categoryPicker: (@Composable () -> Unit)? = null,
    onSave: (Long) -> Unit,
) {
    var text by remember { mutableStateOf(current?.let { Money.toInputString(it) } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (current == null) "New budget" else "${category.icon} ${category.name}") },
        text = {
            Column {
                categoryPicker?.invoke()
                if (categoryPicker != null) VSpace(12)
                AmountField(text, { text = it; error = null }, label = "Monthly limit", error = error, supporting = "Resets on the 1st of every month")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minor = Money.parse(text)
                if (minor == null) error = "Enter an amount greater than zero" else onSave(minor)
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (current != null) TextButton(onClick = { onSave(0) }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
