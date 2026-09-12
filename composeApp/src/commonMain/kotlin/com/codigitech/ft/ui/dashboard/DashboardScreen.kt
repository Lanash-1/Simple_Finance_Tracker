package com.codigitech.ft.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.ui.components.AccountCarouselCard
import com.codigitech.ft.ui.components.AnimatedMoneyText
import com.codigitech.ft.ui.components.BarSegment
import com.codigitech.ft.ui.components.BreakdownRow
import com.codigitech.ft.ui.components.CategoryDot
import com.codigitech.ft.ui.components.DayHeader
import com.codigitech.ft.ui.components.MonthSwitcher
import com.codigitech.ft.ui.components.SectionHeader
import com.codigitech.ft.ui.components.SegmentedBar
import com.codigitech.ft.ui.components.StatColumn
import com.codigitech.ft.ui.components.SurfaceCard
import com.codigitech.ft.ui.components.TransactionRow
import com.codigitech.ft.ui.components.VSpace
import com.codigitech.ft.ui.components.groupByDay
import com.codigitech.ft.ui.components.pluralize
import com.codigitech.ft.ui.theme.AppIcons
import com.codigitech.ft.ui.theme.expenseColor
import com.codigitech.ft.ui.theme.finance
import com.codigitech.ft.ui.theme.incomeColor
import com.codigitech.ft.ui.theme.parseHexColor
import com.codigitech.ft.ui.theme.tabular
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddTransaction: () -> Unit,
    onOpenTransaction: (id: Long, transferId: String?) -> Unit,
    onOpenAccount: (Long) -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenRecurring: () -> Unit,
    onSeeAll: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overview") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            item(key = "hero") { HeroCard(state, viewModel::previousMonth, viewModel::nextMonth, viewModel::resetMonth) }

            if (state.budgetsOver > 0) {
                item(key = "budget-alert") {
                    SurfaceCard(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        onClick = onOpenInsights,
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "${state.budgetsOver.pluralize("budget")} over the limit this month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(AppIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            item(key = "accounts-header") {
                SectionHeader("Accounts", action = { TextButton(onClick = onOpenAccounts) { Text("See all") } })
            }
            item(key = "accounts") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.accounts, key = { it.account.id }) { item ->
                        AccountCarouselCard(item, onClick = { onOpenAccount(item.account.id) }, modifier = Modifier.animateItem())
                    }
                }
            }

            item(key = "spending") {
                AnimatedVisibility(state.spending.isNotEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column {
                        SectionHeader("Spending", action = { TextButton(onClick = onOpenInsights) { Text("Insights") } })
                        SpendingCard(state)
                    }
                }
            }

            if (state.upcoming.isNotEmpty()) {
                item(key = "upcoming") {
                    Column {
                        SectionHeader("Upcoming", action = { TextButton(onClick = onOpenRecurring) { Text("Manage") } })
                        SurfaceCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Column(Modifier.padding(vertical = 6.dp)) {
                                state.upcoming.forEach { u ->
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        CategoryDot(u.category, size = 36)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(u.rule.note?.takeIf { it.isNotBlank() } ?: u.category?.name ?: "Recurring", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                            Text(
                                                when (u.daysAway) { 0 -> "Due today"; 1 -> "Tomorrow"; else -> "In ${u.daysAway} days" } + " · ${u.account?.name ?: ""}",
                                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(
                                            (if (u.rule.type == TransactionType.INCOME) "+" else "−") + Money.format(u.rule.amount),
                                            style = MaterialTheme.typography.bodyMedium.tabular,
                                            color = if (u.rule.type == TransactionType.INCOME) incomeColor() else expenseColor(),
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "recent-header") {
                SectionHeader("Recent", action = { TextButton(onClick = onSeeAll) { Text("See all") } })
            }
            if (!state.loading && state.recent.isEmpty()) {
                item(key = "recent-empty") {
                    SurfaceCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(AppIcons.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            VSpace(8)
                            Text("No transactions yet", style = MaterialTheme.typography.titleSmall)
                            Text("Tap + to record your first expense or income.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            state.today?.let { today ->
                state.recent.groupByDay().forEach { section ->
                    item(key = "day-${section.date}") { DayHeader(section, today, Modifier.animateItem()) }
                    items(section.items, key = { "t" + it.id }) { txn ->
                        TransactionRow(
                            transaction = txn,
                            account = state.accountsById[txn.accountId],
                            counterpart = txn.counterpartAccountId?.let { state.accountsById[it] },
                            category = txn.categoryId?.let { state.categoriesById[it] },
                            onClick = { onOpenTransaction(txn.id, txn.transferId) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(state: DashboardState, onPrev: () -> Unit, onNext: () -> Unit, onReset: () -> Unit) {
    val fc = MaterialTheme.finance
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Brush.linearGradient(listOf(fc.heroStart, fc.heroEnd)), MaterialTheme.shapes.large),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text("Total balance", style = MaterialTheme.typography.labelLarge, color = fc.onHeroMuted)
            AnimatedMoneyText(state.totalBalance, style = MaterialTheme.typography.displayMedium, color = fc.onHero)
            VSpace(8)
            state.month?.let { month ->
                MonthSwitcher(
                    month = month,
                    onPrevious = onPrev,
                    onNext = onNext,
                    canGoNext = !state.isCurrentMonth,
                    onReset = onReset,
                    tint = fc.onHero,
                    modifier = Modifier.padding(horizontal = 0.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn("Income", state.totals.income, fc.onHero, Modifier.weight(1f), labelColor = fc.onHeroMuted)
                StatColumn("Expense", state.totals.expense, fc.onHero, Modifier.weight(1f), labelColor = fc.onHeroMuted)
                StatColumn("Net", state.totals.net, fc.onHero, Modifier.weight(1f), labelColor = fc.onHeroMuted)
            }
            VSpace(14)
            val income = state.totals.income
            val expense = state.totals.expense
            if (income + expense > 0) {
                SegmentedBar(
                    listOf(
                        BarSegment("Income", income, fc.onHero),
                        BarSegment("Expense", expense, fc.onHero.copy(alpha = 0.35f)),
                    ),
                    height = 6,
                )
                VSpace(6)
                Text(
                    if (expense <= income) "You kept ${((income - expense).toFloat() / income.coerceAtLeast(1) * 100).toInt()}% of what came in"
                    else "Spent ${Money.format(expense - income)} more than you earned",
                    style = MaterialTheme.typography.labelSmall,
                    color = fc.onHeroMuted,
                )
            }
        }
    }
}

@Composable
private fun SpendingCard(state: DashboardState) {
    SurfaceCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            val neutral = MaterialTheme.colorScheme.outline
            val segments = state.spending.map { s ->
                BarSegment(s.category?.name ?: "Uncategorized", s.total, s.category?.let { parseHexColor(it.colorHex) } ?: neutral)
            }
            SegmentedBar(segments)
            VSpace(10)
            state.spending.take(4).forEach { s ->
                BreakdownRow(
                    color = s.category?.let { parseHexColor(it.colorHex) } ?: neutral,
                    icon = s.category?.icon,
                    name = s.category?.name ?: "Uncategorized",
                    amount = s.total,
                    share = s.share,
                )
            }
            if (state.spending.size > 4) {
                Text(
                    "+ ${state.spending.size - 4} more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
