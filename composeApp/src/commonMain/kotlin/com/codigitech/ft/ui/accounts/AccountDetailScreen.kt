package com.codigitech.ft.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.ui.components.AccountTypeIcon
import com.codigitech.ft.ui.components.AnimatedMoneyText
import com.codigitech.ft.ui.components.ConfirmDialog
import com.codigitech.ft.ui.components.DayHeader
import com.codigitech.ft.ui.components.EmptyState
import com.codigitech.ft.ui.components.Pill
import com.codigitech.ft.ui.components.StatColumn
import com.codigitech.ft.ui.components.SurfaceCard
import com.codigitech.ft.ui.components.SwipeToDelete
import com.codigitech.ft.ui.components.TransactionRow
import com.codigitech.ft.ui.components.VSpace
import com.codigitech.ft.ui.components.pluralize
import com.codigitech.ft.ui.theme.AppIcons
import com.codigitech.ft.ui.theme.expenseColor
import com.codigitech.ft.ui.theme.incomeColor
import com.codigitech.ft.ui.transactions.ListEvent
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Long,
    onBack: () -> Unit,
    onAddTransaction: () -> Unit,
    onOpenTransaction: (id: Long, transferId: String?) -> Unit,
    accountsViewModel: AccountsViewModel = koinViewModel(),
    viewModel: AccountDetailViewModel = koinViewModel { parametersOf(accountId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val account = state.account

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ListEvent.Deleted -> {
                    if (snackbar.showSnackbar(event.label, actionLabel = "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                }
                ListEvent.Restored -> snackbar.showSnackbar("Restored")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "Account") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (account != null) {
                        IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (account != null && !account.isArchived) {
                FloatingActionButton(onClick = onAddTransaction, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Icon(Icons.Default.Add, contentDescription = "Add transaction")
                }
            }
        },
    ) { padding ->
        if (state.loaded && account == null) {
            EmptyState("This account no longer exists.", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            item(key = "header") {
                SurfaceCard(Modifier.fillMaxWidth().padding(16.dp), containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            account?.let { AccountTypeIcon(it.type, size = 44) }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                AnimatedMoneyText(state.balance, style = MaterialTheme.typography.headlineMedium)
                            }
                            if (account?.isArchived == true) Pill("Archived", MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        VSpace(16)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatColumn("In this month", state.monthIn, incomeColor(), Modifier.weight(1f))
                            StatColumn("Out this month", state.monthOut, expenseColor(), Modifier.weight(1f))
                        }
                        account?.let {
                            VSpace(12)
                            Text(
                                "${it.type.label} · opened with ${Money.format(it.initialBalance)} · ${state.transactionCount.pluralize("entry", "entries")}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (state.loaded && state.sections.isEmpty()) {
                item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(AppIcons.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        VSpace(8)
                        Text("No transactions for this account yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            val today = state.today
            state.sections.forEach { section ->
                item(key = "day-${section.date}") { if (today != null) DayHeader(section, today, Modifier.animateItem()) }
                items(section.items, key = { it.id }) { txn ->
                    SwipeToDelete(modifier = Modifier.animateItem(), onDelete = { viewModel.deleteTransaction(txn) }) {
                        TransactionRow(
                            transaction = txn,
                            account = null,
                            counterpart = txn.counterpartAccountId?.let { state.accountsById[it] },
                            category = txn.categoryId?.let { state.categoriesById[it] },
                            onClick = { onOpenTransaction(txn.id, txn.transferId) },
                        )
                    }
                }
            }
        }
    }

    if (editing && account != null) {
        AccountEditorDialog(account = account, onDismiss = { editing = false }) { name, type, balance ->
            accountsViewModel.save(account.id, name, type, balance); editing = false
        }
    }
    if (confirmDelete) {
        val hasHistory = state.transactionCount > 0
        ConfirmDialog(
            title = if (hasHistory) "Archive account?" else "Delete account?",
            text = if (hasHistory) "This account has transactions, so it will be archived instead of deleted. Its history stays visible."
            else "This account has no transactions and will be removed.",
            confirmLabel = if (hasHistory) "Archive" else "Delete",
            destructive = !hasHistory,
            onConfirm = { confirmDelete = false; viewModel.delete { onBack() } },
            onDismiss = { confirmDelete = false },
        )
    }
}
