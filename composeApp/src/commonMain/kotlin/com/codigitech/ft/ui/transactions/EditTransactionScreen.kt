package com.codigitech.ft.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.domain.usecase.DateProvider
import com.codigitech.ft.ui.components.ConfirmDialog
import com.codigitech.ft.ui.components.DatePickerSheet
import com.codigitech.ft.ui.components.DropdownField
import com.codigitech.ft.ui.components.VSpace
import com.codigitech.ft.ui.components.display
import com.codigitech.ft.ui.theme.expenseColor
import com.codigitech.ft.ui.theme.incomeColor
import com.codigitech.ft.ui.theme.parseHexColor
import com.codigitech.ft.ui.theme.tabular
import com.codigitech.ft.ui.theme.transferColor
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionScreen(
    transactionId: Long?,
    transferId: String?,
    presetAccountId: Long?,
    onDone: () -> Unit,
    /** False while the screen is still animating in; autofocus (and the keyboard) waits for it. */
    settled: Boolean = true,
    viewModel: EditTransactionViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.load(transactionId, transferId, presetAccountId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dates = koinInject<DateProvider>()
    val amountFocus = remember { FocusRequester() }
    var pickingDate by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    // Open the keyboard only once the sheet has finished rising, so the IME slide doesn't fight the enter transition.
    var focusedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(state.loading, settled) {
        if (!state.loading && !state.isEdit && settled && !focusedOnce) {
            focusedOnce = true
            amountFocus.requestFocus()
        }
    }

    val accent by animateColorAsState(
        when (state.kind) {
            EntryKind.EXPENSE -> expenseColor()
            EntryKind.INCOME -> incomeColor()
            EntryKind.TRANSFER -> transferColor()
        },
        label = "accent",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Edit ${state.kind.label.lowercase()}" else "New ${state.kind.label.lowercase()}") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Default.Close, contentDescription = "Close") } },
                actions = {
                    if (state.isEdit) IconButton(onClick = { viewModel.toggleDeleteConfirm(true) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            // safeDrawing.only(Bottom) is the union of the IME and navigation-bar insets, so the button clears
            // the 3-button nav bar when the keyboard is closed and rides above the keyboard when it is open.
            Column(
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                AnimatedVisibility(state.formError != null) {
                    Text(
                        state.formError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isEdit) "Save changes" else "Add ${state.kind.label.lowercase()}", style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (state.canSwitchKind) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val kinds = if (state.isEdit) listOf(EntryKind.EXPENSE, EntryKind.INCOME) else EntryKind.entries
                    kinds.forEachIndexed { index, kind ->
                        SegmentedButton(
                            selected = state.kind == kind,
                            onClick = { viewModel.setKind(kind) },
                            shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = accent.copy(alpha = 0.16f),
                                activeContentColor = accent,
                            ),
                        ) { Text(kind.label) }
                    }
                }
                VSpace(20)
            }

            // Amount ---------------------------------------------------------------------------
            OutlinedTextField(
                value = state.amountText,
                onValueChange = { if (it.length <= 16) viewModel.setAmount(it) },
                placeholder = { Text("0", style = MaterialTheme.typography.displaySmall.tabular, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                prefix = { Text(Money.SYMBOL, style = MaterialTheme.typography.headlineMedium, color = accent) },
                singleLine = true,
                isError = state.amountError != null,
                supportingText = state.amountError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.displaySmall.tabular.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().focusRequester(amountFocus),
            )
            VSpace(20)

            // Accounts -------------------------------------------------------------------------
            DropdownField(
                label = if (state.kind == EntryKind.TRANSFER) "From account" else "Account",
                options = state.accounts,
                selected = state.accounts.firstOrNull { it.id == state.accountId },
                optionLabel = { it.name },
                onSelect = { viewModel.setAccount(it.id) },
            )
            if (state.kind == EntryKind.TRANSFER) {
                VSpace(12)
                DropdownField(
                    label = "To account",
                    options = state.accounts.filter { it.id != state.accountId },
                    selected = state.accounts.firstOrNull { it.id == state.toAccountId },
                    optionLabel = { it.name },
                    onSelect = { viewModel.setToAccount(it.id) },
                )
            }

            // Category -------------------------------------------------------------------------
            if (state.kind != EntryKind.TRANSFER) {
                VSpace(20)
                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(8)
                CategoryChips(state.categoriesForKind, state.categoryId, viewModel::setCategory)
            }

            // Date -----------------------------------------------------------------------------
            VSpace(20)
            Text("Date", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            VSpace(8)
            val today = dates.today()
            val yesterday = today.minus(1, DateTimeUnit.DAY)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.date == today, onClick = { viewModel.setDate(today) }, label = { Text("Today") })
                FilterChip(selected = state.date == yesterday, onClick = { viewModel.setDate(yesterday) }, label = { Text("Yesterday") })
                val custom = state.date != null && state.date != today && state.date != yesterday
                FilterChip(
                    selected = custom,
                    onClick = { pickingDate = true },
                    label = { Text(if (custom) state.date!!.display() else "Pick a date") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.width(18.dp)) },
                )
            }

            // Note -----------------------------------------------------------------------------
            VSpace(20)
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Note (optional)") },
                placeholder = { Text(if (state.kind == EntryKind.TRANSFER) "e.g. Moving savings" else "e.g. Groceries at Nilgiris") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = MaterialTheme.shapes.small,
            )
            VSpace(24)
        }
    }

    if (pickingDate) {
        DatePickerSheet(initial = state.date, onDismiss = { pickingDate = false }) { viewModel.setDate(it); pickingDate = false }
    }
    if (state.confirmingDelete) {
        ConfirmDialog(
            title = "Delete this ${state.kind.label.lowercase()}?",
            text = if (state.kind == EntryKind.TRANSFER) "Both sides of this transfer will be removed." else "This cannot be undone.",
            onConfirm = { viewModel.toggleDeleteConfirm(false); viewModel.delete() },
            onDismiss = { viewModel.toggleDeleteConfirm(false) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(categories: List<Category>, selectedId: Long?, onSelect: (Long) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { category ->
            val selected = category.id == selectedId
            val tint = parseHexColor(category.colorHex)
            FilterChip(
                selected = selected,
                onClick = { onSelect(category.id) },
                label = { Text(category.name) },
                leadingIcon = { Text(category.icon) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tint.copy(alpha = 0.18f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    selectedBorderColor = tint,
                    selectedBorderWidth = 1.5.dp,
                ),
            )
        }
    }
}
