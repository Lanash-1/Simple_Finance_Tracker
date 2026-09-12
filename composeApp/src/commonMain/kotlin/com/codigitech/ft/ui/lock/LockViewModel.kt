package com.codigitech.ft.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigitech.ft.platform.AppLockManager
import com.codigitech.ft.platform.BiometricResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockState(val pin: String = "", val error: String? = null, val biometricOffered: Boolean = false, val errorCount: Int = 0)

class LockViewModel(private val lock: AppLockManager) : ViewModel() {
    private val _state = MutableStateFlow(LockState(biometricOffered = lock.biometricAvailable && lock.biometricEnabled))
    val state: StateFlow<LockState> = _state.asStateFlow()

    fun press(digit: Char) {
        val next = _state.value.pin + digit
        if (next.length > 6) return
        _state.update { it.copy(pin = next, error = null) }
        if (next.length >= 4 && lock.unlockWithPin(next)) {
            _state.update { LockState(biometricOffered = it.biometricOffered) }
        } else if (next.length == 6) {
            _state.update { it.copy(pin = "", error = "Wrong PIN, try again", errorCount = it.errorCount + 1) }
        }
    }

    fun backspace() = _state.update { it.copy(pin = it.pin.dropLast(1), error = null) }

    fun biometric() {
        viewModelScope.launch {
            when (val r = lock.unlockWithBiometric()) {
                BiometricResult.Success -> _state.update { LockState(biometricOffered = it.biometricOffered) }
                BiometricResult.Unavailable -> _state.update { it.copy(biometricOffered = false) }
                is BiometricResult.Failed -> _state.update { it.copy(error = null) }
            }
        }
    }
}
