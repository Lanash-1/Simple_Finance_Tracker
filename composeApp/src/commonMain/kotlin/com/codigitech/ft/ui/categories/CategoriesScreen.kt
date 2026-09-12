package com.codigitech.ft.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.CategoryType
import com.codigitech.ft.ui.components.CategoryDot
import com.codigitech.ft.ui.components.ConfirmDialog
import com.codigitech.ft.ui.components.DropdownField
import com.codigitech.ft.ui.components.SectionHeader
import com.codigitech.ft.ui.components.SurfaceCard
import com.codigitech.ft.ui.components.VSpace
import com.codigitech.ft.ui.theme.parseHexColor
import org.koin.compose.viewmodel.koinViewModel

private val PALETTE = listOf(
    "#EF6C00", "#1976D2", "#8E24AA", "#00897B", "#5D4037", "#D32F2F",
    "#F9A825", "#607D8B", "#2E7D32", "#00838F", "#C2185B", "#558B2F",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit, viewModel: CategoriesViewModel = koinViewModel()) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<Category?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Default.Add, contentDescription = "Add category") }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)) {
            for (type in CategoryType.entries) {
                val ofType = categories.filter { it.type == type }
                item(key = "h-" + type.name) { SectionHeader("${type.label} (${ofType.size})") }
                item(key = "c-" + type.name) {
                    SurfaceCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            ofType.forEachIndexed { index, category ->
                                Row(
                                    Modifier.fillMaxWidth().clickable(enabled = !category.isDefault) { editing = category }
                                        .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CategoryDot(category)
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(category.name, style = MaterialTheme.typography.bodyLarge)
                                        val tag = when {
                                            category.isDefault -> "Built-in fallback for deleted categories"
                                            category.isCustom -> "Custom"
                                            else -> "Predefined"
                                        }
                                        Text(tag, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (!category.isDefault) {
                                        IconButton(onClick = { deleting = category }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    } else {
                                        Spacer(Modifier.width(48.dp))
                                    }
                                }
                                if (index < ofType.lastIndex) HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        CategoryEditorDialog(null, onDismiss = { showAdd = false }) { name, icon, color, type ->
            viewModel.save(null, name, icon, color, type); showAdd = false
        }
    }
    editing?.let { category ->
        CategoryEditorDialog(category, onDismiss = { editing = null }) { name, icon, color, _ ->
            viewModel.save(category.id, name, icon, color, category.type); editing = null
        }
    }
    deleting?.let { category ->
        ConfirmDialog(
            title = "Delete ${category.name}?",
            text = "Transactions in this category will move to Uncategorized.",
            onConfirm = { viewModel.delete(category.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CategoryEditorDialog(category: Category?, onDismiss: () -> Unit, onSave: (String, String, String, CategoryType) -> Unit) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "🏷️") }
    var color by remember { mutableStateOf(category?.colorHex ?: PALETTE.first()) }
    var type by remember { mutableStateOf(category?.type ?: CategoryType.EXPENSE) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "New category" else "Edit category") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = icon, onValueChange = { if (it.length <= 4) icon = it },
                        label = { Text("Emoji") }, singleLine = true, modifier = Modifier.width(96.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                VSpace()
                if (category == null) {
                    DropdownField("Type", CategoryType.entries, type, { it.label }, { type = it })
                    VSpace()
                }
                Text("Colour", style = MaterialTheme.typography.labelMedium)
                VSpace(4)
                PALETTE.chunked(6).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { hex ->
                            val selected = hex == color
                            androidx.compose.foundation.layout.Box(
                                Modifier.size(36.dp)
                                    .background(parseHexColor(hex), CircleShape)
                                    .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface, CircleShape)
                                    .clickable { color = hex },
                            )
                        }
                    }
                    VSpace(6)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) error = "Name is required" else onSave(name, icon.ifBlank { "🏷️" }, color, type)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
