package com.codigitech.ft.ui.settings

import com.codigitech.ft.Brand
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codigitech.ft.platform.ThemeMode
import com.codigitech.ft.ui.components.SectionHeader
import com.codigitech.ft.ui.components.SurfaceCard
import com.codigitech.ft.ui.components.VSpace
import com.codigitech.ft.ui.theme.AppIcons
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenCategories: () -> Unit,
    onOpenRecurring: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPinSetup by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { SectionHeader("Manage") }
            item {
                SurfaceCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SettingsRow(AppIcons.Repeat, "Recurring rules", "Rent, salary, subscriptions", onClick = onOpenRecurring)
                    HorizontalDivider(Modifier.padding(start = 56.dp))
                    SettingsRow(AppIcons.Category, "Categories", "Add, rename or remove categories", onClick = onOpenCategories)
                }
            }

            item { SectionHeader("Appearance") }
            item {
                SurfaceCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Theme", style = MaterialTheme.typography.bodyLarge)
                        VSpace(10)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { i, mode ->
                                SegmentedButton(
                                    selected = state.themeMode == mode,
                                    onClick = { viewModel.setTheme(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                                ) { Text(mode.label) }
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Security") }
            item {
                SurfaceCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        headlineContent = { Text("App lock") },
                        supportingContent = { Text(if (state.lockEnabled) "Locks when the app goes to the background" else "Off") },
                        trailingContent = {
                            Switch(checked = state.lockEnabled, onCheckedChange = { on -> if (on) showPinSetup = true else viewModel.disableLock() })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    AnimatedVisibility(state.lockEnabled) {
                        Column {
                            HorizontalDivider(Modifier.padding(start = 56.dp))
                            ListItem(
                                leadingContent = { Icon(AppIcons.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                headlineContent = { Text("Unlock with biometrics") },
                                supportingContent = { Text(if (state.biometricAvailable) "Falls back to your PIN" else "Not available on this device") },
                                trailingContent = {
                                    Switch(checked = state.biometricEnabled && state.biometricAvailable, enabled = state.biometricAvailable, onCheckedChange = viewModel::setBiometric)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                            HorizontalDivider(Modifier.padding(start = 56.dp))
                            SettingsRow(null, "Change PIN", null, onClick = { showPinSetup = true })
                        }
                    }
                }
            }

            item { SectionHeader("Data") }
            item {
                SurfaceCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SettingsRow(
                        AppIcons.Share,
                        if (state.exporting) "Preparing export…" else "Export transactions",
                        "Share a CSV of every transaction. Pocketsum keeps no copy anywhere else.",
                        onClick = viewModel::exportTransactions,
                    )
                }
            }

            item { SectionHeader("About") }
            item {
                SurfaceCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        headlineContent = { Text("${Brand.APP_NAME} ${Brand.VERSION}") },
                        supportingContent = { Text("Offline. Single currency (₹). All data stays on this device.") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    if (Brand.PRIVACY_POLICY_URL.isNotBlank()) {
                        HorizontalDivider(Modifier.padding(start = 56.dp))
                        SettingsRow(null, "Privacy policy", null, onClick = { uriHandler.openUri(Brand.PRIVACY_POLICY_URL) })
                    }
                }
            }
        }
    }

    if (showPinSetup) {
        PinSetupDialog(onDismiss = { showPinSetup = false }) { pin -> viewModel.enableLock(pin); showPinSetup = false }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector?, title: String, subtitle: String?, onClick: () -> Unit) {
    ListItem(
        leadingContent = if (icon != null) { { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } } else { { Modifier.size(24.dp) } },
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Icon(AppIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("PIN (4-6 digits)") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
                )
                OutlinedTextField(
                    value = confirm, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirm = it },
                    label = { Text("Confirm PIN") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    pin.length < 4 -> error = "PIN must be at least 4 digits"
                    pin != confirm -> error = "PINs do not match"
                    else -> onSet(pin)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
