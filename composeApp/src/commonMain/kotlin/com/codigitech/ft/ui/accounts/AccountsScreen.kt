package com.codigitech.ft.ui.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codigitech.ft.domain.model.Account
import com.codigitech.ft.domain.model.AccountType
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.ui.components.AccountListRow
import com.codigitech.ft.ui.components.AnimatedMoneyText
import com.codigitech.ft.ui.components.DropdownField
import com.codigitech.ft.ui.components.EmptyState
import com.codigitech.ft.ui.components.SectionHeader
import com.codigitech.ft.ui.components.VSpace
import com.codigitech.ft.ui.components.pluralize
import com.codigitech.ft.ui.theme.AppIcons
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(onOpenAccount: (Long) -> Unit, viewModel: AccountsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Accounts") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Default.Add, contentDescription = "Add account")
            }
        },
    ) { padding ->
        if (state.active.isEmpty() && state.archived.isEmpty()) {
            EmptyState(
                title = "No accounts yet",
                subtitle = "Add your cash, bank and card accounts to see everything in one place.",
                icon = AppIcons.Wallet,
                actionLabel = "Add account",
                onAction = { showAdd = true },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "total") {
                Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                    Text("Across ${state.active.size.pluralize("active account")}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimatedMoneyText(state.total, style = MaterialTheme.typography.headlineLarge)
                }
            }
            items(state.active, key = { it.account.id }) { item ->
                AccountListRow(item, onClick = { onOpenAccount(item.account.id) }, modifier = Modifier.animateItem())
            }
            if (state.archived.isNotEmpty()) {
                item(key = "archived-header") {
                    val rotation by animateFloatAsState(if (showArchived) 180f else 0f, label = "chevron")
                    SectionHeader("Archived (${state.archived.size})", Modifier.animateItem(), action = {
                        IconButton(onClick = { showArchived = !showArchived }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = if (showArchived) "Collapse" else "Expand", modifier = Modifier.rotate(rotation))
                        }
                    })
                }
                items(state.archived, key = { it.account.id }) { item ->
                    AnimatedVisibility(showArchived, modifier = Modifier.animateItem()) {
                        AccountListRow(item, onClick = { onOpenAccount(item.account.id) }, trailing = {
                            TextButton(onClick = { viewModel.restore(item.account.id) }) { Text("Restore") }
                        })
                    }
                }
            }
        }
    }

    if (showAdd) {
        AccountEditorDialog(account = null, onDismiss = { showAdd = false }) { name, type, balance ->
            viewModel.save(null, name, type, balance); showAdd = false
        }
    }
}

@Composable
fun AccountEditorDialog(account: Account?, onDismiss: () -> Unit, onSave: (String, AccountType, Long) -> Unit) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var type by remember { mutableStateOf(account?.type ?: AccountType.CASH) }
    var balanceText by remember { mutableStateOf(account?.let { Money.toInputString(it.initialBalance) } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "New account" else "Edit account") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small)
                VSpace()
                DropdownField("Type", AccountType.entries, type, { it.label }, { type = it })
                VSpace()
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Opening balance") },
                    prefix = { Text(Money.SYMBOL) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    supportingText = { Text("Use a minus sign for money you owe, e.g. -2500") },
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val balance = parseSignedAmount(balanceText)
                when {
                    name.isBlank() -> error = "Name is required"
                    balance == null -> error = "Enter a valid amount"
                    else -> onSave(name, type, balance)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Opening balances may be zero or negative (card debt). */
fun parseSignedAmount(text: String): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed == "0") return 0L
    val negative = trimmed.startsWith("-")
    val body = trimmed.removePrefix("-")
    if (body == "0" || body == "0.0" || body == "0.00") return 0L
    val minor = Money.parse(body) ?: return null
    return if (negative) -minor else minor
}
