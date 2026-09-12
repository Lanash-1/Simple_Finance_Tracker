package com.codigitech.ft.ui.recurring

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codigitech.ft.domain.model.CategoryType
import com.codigitech.ft.domain.model.Frequency
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.domain.model.RecurringRule
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.usecase.DateProvider
import com.codigitech.ft.ui.components.CategoryDot
import com.codigitech.ft.ui.components.ConfirmDialog
import com.codigitech.ft.ui.components.DateField
import com.codigitech.ft.ui.components.DropdownField
import com.codigitech.ft.ui.components.EmptyState
import com.codigitech.ft.ui.components.SurfaceCard
import com.codigitech.ft.ui.components.VSpace
import com.codigitech.ft.ui.components.display
import com.codigitech.ft.ui.theme.AppIcons
import com.codigitech.ft.ui.theme.expenseColor
import com.codigitech.ft.ui.theme.incomeColor
import com.codigitech.ft.ui.theme.tabular
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(onBack: () -> Unit, viewModel: RecurringViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dates = koinInject<DateProvider>()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<RecurringRule?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<RecurringRule?>(null) }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Default.Add, contentDescription = "Add rule")
            }
        },
    ) { padding ->
        if (state.loaded && state.rules.isEmpty()) {
            EmptyState(
                title = "Nothing on repeat yet",
                subtitle = "Add rent, salary or subscriptions and they will be posted automatically on their due dates.",
                icon = AppIcons.Repeat,
                actionLabel = "Add rule",
                onAction = { showAdd = true },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        val today = dates.today()
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.rules, key = { it.id }) { rule ->
                val category = rule.categoryId?.let { state.categoriesById[it] }
                val account = state.accountsById[rule.accountId]
                SurfaceCard(Modifier.fillMaxWidth().animateItem(), onClick = { editing = rule }) {
                    Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CategoryDot(category)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                rule.note?.takeIf { it.isNotBlank() } ?: category?.name ?: "Recurring",
                                style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            val due = today.daysUntil(rule.nextDueDate)
                            Text(
                                "${rule.frequency.label} · ${account?.name ?: "?"} · " + when {
                                    !rule.isActive -> "paused"
                                    due <= 0 -> "due today"
                                    due == 1 -> "due tomorrow"
                                    due < 14 -> "in $due days"
                                    else -> "next ${rule.nextDueDate.display()}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            (if (rule.type == TransactionType.INCOME) "+" else "−") + Money.format(rule.amount),
                            style = MaterialTheme.typography.bodyLarge.tabular,
                            color = if (rule.type == TransactionType.INCOME) incomeColor() else expenseColor(),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Switch(checked = rule.isActive, onCheckedChange = { viewModel.toggle(rule.id, it) }, modifier = Modifier.padding(start = 8.dp))
                        IconButton(onClick = { deleting = rule }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        val rule = editing
        RuleEditorDialog(rule, state, onDismiss = { showAdd = false; editing = null }) { amount, accountId, categoryId, type, freq, start, note ->
            viewModel.save(rule?.id, amount, accountId, categoryId, type, freq, start, note)
            showAdd = false; editing = null
        }
    }
    deleting?.let { rule ->
        ConfirmDialog(
            title = "Delete rule?",
            text = "Transactions already generated by this rule are kept.",
            onConfirm = { viewModel.delete(rule.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun RuleEditorDialog(
    rule: RecurringRule?,
    state: RecurringState,
    onDismiss: () -> Unit,
    onSave: (Long, Long, Long?, TransactionType, Frequency, LocalDate, String?) -> Unit,
) {
    val dates = koinInject<DateProvider>()
    var type by remember { mutableStateOf(rule?.type ?: TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf(rule?.let { Money.toInputString(it.amount) } ?: "") }
    var accountId by remember { mutableStateOf(rule?.accountId ?: state.accounts.firstOrNull()?.id) }
    var categoryId by remember { mutableStateOf(rule?.categoryId) }
    var frequency by remember { mutableStateOf(rule?.frequency ?: Frequency.MONTHLY) }
    var startDate by remember { mutableStateOf<LocalDate?>(rule?.startDate ?: dates.today()) }
    var note by remember { mutableStateOf(rule?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val categoryType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
    val categoryOptions = state.categories.filter { it.type == categoryType }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (rule == null) "New recurring rule" else "Edit rule") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEachIndexed { i, t ->
                        SegmentedButton(
                            selected = type == t,
                            onClick = { type = t; categoryId = null },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(if (t == TransactionType.INCOME) "Income" else "Expense") }
                    }
                }
                VSpace()
                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = it }, label = { Text("Amount") }, prefix = { Text(Money.SYMBOL) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )
                VSpace()
                DropdownField("Account", state.accounts, state.accounts.firstOrNull { it.id == accountId }, { it.name }, { accountId = it.id })
                VSpace()
                DropdownField(
                    "Category", categoryOptions,
                    categoryOptions.firstOrNull { it.id == categoryId } ?: categoryOptions.firstOrNull { !it.isDefault },
                    { "${it.icon} ${it.name}" }, { categoryId = it.id },
                )
                VSpace()
                DropdownField("Frequency", Frequency.entries, frequency, { it.label }, { frequency = it })
                VSpace()
                DateField("Start date", startDate, { startDate = it })
                VSpace()
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Label / note") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small)
                error?.let { VSpace(4); Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = Money.parse(amountText)
                val acc = accountId
                val start = startDate
                when {
                    amount == null -> error = "Enter an amount greater than zero"
                    acc == null -> error = "Choose an account"
                    start == null -> error = "Choose a start date"
                    else -> onSave(
                        amount, acc,
                        categoryId ?: categoryOptions.firstOrNull { !it.isDefault }?.id ?: categoryOptions.firstOrNull()?.id,
                        type, frequency, start, note.ifBlank { null },
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
