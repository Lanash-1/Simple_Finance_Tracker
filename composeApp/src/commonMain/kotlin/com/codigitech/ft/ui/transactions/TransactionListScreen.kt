package com.codigitech.ft.ui.transactions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codigitech.ft.domain.model.Account
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.domain.model.TransactionKind
import com.codigitech.ft.ui.components.DateField
import com.codigitech.ft.ui.components.DayHeader
import com.codigitech.ft.ui.components.DropdownField
import com.codigitech.ft.ui.components.EmptyState
import com.codigitech.ft.ui.components.SwipeToDelete
import com.codigitech.ft.ui.components.TransactionRow
import com.codigitech.ft.ui.components.VSpace
import com.codigitech.ft.ui.components.pluralize
import com.codigitech.ft.ui.theme.AppIcons
import com.codigitech.ft.ui.theme.expenseColor
import com.codigitech.ft.ui.theme.incomeColor
import com.codigitech.ft.ui.theme.tabular
import org.koin.compose.viewmodel.koinViewModel

private data class AccountOption(val account: Account?) { val label get() = account?.name ?: "All accounts" }
private data class CategoryOption(val category: Category?) { val label get() = category?.let { "${it.icon} ${it.name}" } ?: "All categories" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    presetAccountId: Long?,
    onAddTransaction: () -> Unit,
    onOpenTransaction: (id: Long, transferId: String?) -> Unit,
    viewModel: TransactionListViewModel = koinViewModel(),
) {
    LaunchedEffect(presetAccountId) { viewModel.presetAccount(presetAccountId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    var searching by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ListEvent.Deleted -> {
                    val result = snackbar.showSnackbar(event.label, actionLabel = "Undo", duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                }
                ListEvent.Restored -> snackbar.showSnackbar("Restored")
            }
        }
    }
    LaunchedEffect(searching) { if (searching) searchFocus.requestFocus() }

    Scaffold(
        topBar = {
            AnimatedContent(searching, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "appbar") { isSearching ->
                if (isSearching) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = filter.query,
                                onValueChange = viewModel::setQuery,
                                placeholder = { Text("Search notes or amounts") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { searching = false; viewModel.setQuery("") }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search") }
                        },
                        actions = {
                            if (filter.query.isNotEmpty()) IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Default.Clear, contentDescription = "Clear") }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    )
                } else {
                    TopAppBar(
                        title = { Text("History") },
                        actions = {
                            IconButton(onClick = { searching = true }) { Icon(Icons.Default.Search, contentDescription = "Search") }
                            IconButton(onClick = { showSheet = true }) {
                                BadgedBox(badge = { if (filter.hasAdvanced) Badge() }) {
                                    Icon(AppIcons.Tune, contentDescription = "Filters")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            KindChips(filter.kind, viewModel::setKind)

            AnimatedVisibility(!filter.isEmpty && state.loaded) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.count.pluralize("result"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.income > 0) Text("+${Money.format(state.income)}", style = MaterialTheme.typography.labelMedium.tabular, color = incomeColor())
                    if (state.income > 0 && state.expense > 0) Spacer(Modifier.width(10.dp))
                    if (state.expense > 0) Text("−${Money.format(state.expense)}", style = MaterialTheme.typography.labelMedium.tabular, color = expenseColor())
                    TextButton(onClick = { viewModel.clear(); searching = false }) { Text("Clear") }
                }
            }

            if (state.loaded && state.sections.isEmpty()) {
                if (filter.isEmpty) {
                    EmptyState(
                        title = "No transactions yet",
                        subtitle = "Everything you record shows up here, newest first.",
                        icon = AppIcons.Receipt,
                        actionLabel = "Add transaction",
                        onAction = onAddTransaction,
                    )
                } else {
                    EmptyState(title = "Nothing matches", subtitle = "Try a different search or clear the filters.", icon = Icons.Default.Search)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    val today = state.today
                    state.sections.forEach { section ->
                        item(key = "day-${section.date}") {
                            if (today != null) DayHeader(section, today, Modifier.animateItem())
                        }
                        items(section.items, key = { it.id }) { txn ->
                            SwipeToDelete(modifier = Modifier.animateItem(), onDelete = { viewModel.delete(txn) }) {
                                TransactionRow(
                                    transaction = txn,
                                    account = state.accountsById[txn.accountId],
                                    counterpart = txn.counterpartAccountId?.let { state.accountsById[it] },
                                    category = txn.categoryId?.let { state.categoriesById[it] },
                                    onClick = { onOpenTransaction(txn.id, txn.transferId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Filters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (filter.hasAdvanced) TextButton(onClick = viewModel::clearAdvanced) { Text("Reset") }
                }
                VSpace(12)
                val accountOptions = listOf(AccountOption(null)) + state.accounts.map { AccountOption(it) }
                DropdownField(
                    "Account", accountOptions,
                    accountOptions.firstOrNull { it.account?.id == filter.accountId } ?: accountOptions.first(),
                    { it.label }, { viewModel.setAccount(it.account?.id) },
                )
                VSpace(12)
                val categoryOptions = listOf(CategoryOption(null)) + state.categories.map { CategoryOption(it) }
                DropdownField(
                    "Category", categoryOptions,
                    categoryOptions.firstOrNull { it.category?.id == filter.categoryId } ?: categoryOptions.first(),
                    { it.label }, { viewModel.setCategory(it.category?.id) },
                )
                VSpace(12)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField("From", filter.fromDate, viewModel::setFrom, allowClear = true, modifier = Modifier.weight(1f))
                    DateField("To", filter.toDate, viewModel::setTo, allowClear = true, modifier = Modifier.weight(1f))
                }
                VSpace(20)
                Button(onClick = { showSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Show ${state.count.pluralize("result")}")
                }
            }
        }
    }
}

@Composable
private fun KindChips(selected: TransactionKind?, onSelect: (TransactionKind?) -> Unit) {
    val options = listOf<TransactionKind?>(null) + TransactionKind.entries
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options, key = { it?.name ?: "all" }) { kind ->
            val isSelected = kind == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(kind) },
                label = { Text(kind?.label ?: "All") },
                leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.width(16.dp)) } } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}
