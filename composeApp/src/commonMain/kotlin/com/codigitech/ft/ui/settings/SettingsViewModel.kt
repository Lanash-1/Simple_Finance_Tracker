package com.codigitech.ft.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigitech.ft.domain.usecase.ExportTransactionsCsvUseCase
import com.codigitech.ft.platform.AppLockManager
import com.codigitech.ft.platform.AppSettings
import com.codigitech.ft.platform.FileSharer
import com.codigitech.ft.platform.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val lockEnabled: Boolean,
    val biometricAvailable: Boolean,
    val biometricEnabled: Boolean,
    val themeMode: ThemeMode,
    val exporting: Boolean = false,
)

class SettingsViewModel(
    private val settings: AppSettings,
    private val lock: AppLockManager,
    private val exportCsv: ExportTransactionsCsvUseCase,
    private val sharer: FileSharer,
) : ViewModel() {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    private fun read() = SettingsState(
        lockEnabled = lock.isEnabled,
        biometricAvailable = lock.biometricAvailable,
        biometricEnabled = settings.biometricEnabled,
        themeMode = settings.themeMode.value,
    )

    fun enableLock(pin: String) { lock.enable(pin); _state.update { read() } }
    fun disableLock() { lock.disable(); _state.update { read() } }
    fun setBiometric(enabled: Boolean) { lock.setBiometricEnabled(enabled); _state.update { read() } }
    fun setTheme(mode: ThemeMode) { settings.setThemeMode(mode); _state.update { read() } }

    fun exportTransactions() {
        if (_state.value.exporting) return
        _state.update { it.copy(exporting = true) }
        viewModelScope.launch {
            try {
                val export = exportCsv()
                if (export.rowCount == 0) {
                    _messages.tryEmit("Nothing to export yet")
                } else {
                    sharer.shareText(export.fileName, "text/csv", export.csv)
                }
            } catch (e: Exception) {
                _messages.tryEmit("Export failed: ${e.message ?: "unknown error"}")
            } finally {
                _state.update { it.copy(exporting = false) }
            }
        }
    }
}
