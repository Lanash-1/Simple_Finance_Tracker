package com.codigitech.ft.platform

import com.codigitech.ft.Brand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tracks whether the current session is unlocked. Locks again whenever the app goes to background. */
class AppLockManager(private val settings: AppSettings, private val biometrics: BiometricAuthenticator) {
    private val _locked = MutableStateFlow(settings.appLockEnabled && settings.pinHash != null)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    val isEnabled: Boolean get() = settings.appLockEnabled && settings.pinHash != null
    val biometricAvailable: Boolean get() = biometrics.isAvailable()
    val biometricEnabled: Boolean get() = settings.biometricEnabled

    fun onBackground() {
        if (isEnabled) _locked.value = true
    }

    fun unlockWithPin(pin: String): Boolean {
        val salt = settings.pinSalt ?: return false
        val ok = PinHasher.hash(pin, salt) == settings.pinHash
        if (ok) _locked.value = false
        return ok
    }

    suspend fun unlockWithBiometric(): BiometricResult {
        val result = biometrics.authenticate("Unlock ${Brand.APP_NAME}")
        if (result is BiometricResult.Success) _locked.value = false
        return result
    }

    fun enable(pin: String) {
        val salt = PinHasher.newSalt()
        settings.pinSalt = salt
        settings.pinHash = PinHasher.hash(pin, salt)
        settings.appLockEnabled = true
        _locked.value = false
    }

    fun disable() {
        settings.appLockEnabled = false
        settings.pinHash = null
        settings.pinSalt = null
        _locked.value = false
    }

    fun setBiometricEnabled(enabled: Boolean) {
        settings.biometricEnabled = enabled
    }
}
