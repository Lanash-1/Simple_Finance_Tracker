package com.codigitech.ft.platform

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/** Thin wrapper over multiplatform-settings for the handful of preferences the app has. */
class AppSettings(private val settings: Settings) {
    var appLockEnabled: Boolean
        get() = settings.getBoolean(KEY_LOCK_ENABLED, false)
        set(value) = settings.putBoolean(KEY_LOCK_ENABLED, value)

    /** Salted hash of the PIN; never the PIN itself. */
    var pinHash: String?
        get() = settings.getStringOrNull(KEY_PIN_HASH)
        set(value) = if (value == null) settings.remove(KEY_PIN_HASH) else settings.putString(KEY_PIN_HASH, value)

    var pinSalt: String?
        get() = settings.getStringOrNull(KEY_PIN_SALT)
        set(value) = if (value == null) settings.remove(KEY_PIN_SALT) else settings.putString(KEY_PIN_SALT, value)

    var biometricEnabled: Boolean
        get() = settings.getBoolean(KEY_BIOMETRIC, true)
        set(value) = settings.putBoolean(KEY_BIOMETRIC, value)

    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.firstOrNull { it.name == settings.getStringOrNull(KEY_THEME) } ?: ThemeMode.SYSTEM,
    )

    /** Observed by the root composable so a change in Settings re-themes the whole app at once. */
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        settings.putString(KEY_THEME, mode.name)
        _themeMode.value = mode
    }

    /** Whether the user has dismissed the "swipe to delete" hint on the history list. */
    var swipeHintSeen: Boolean
        get() = settings.getBoolean(KEY_SWIPE_HINT, false)
        set(value) = settings.putBoolean(KEY_SWIPE_HINT, value)

    private companion object {
        const val KEY_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_BIOMETRIC = "biometric_enabled"
        const val KEY_THEME = "theme_mode"
        const val KEY_SWIPE_HINT = "swipe_hint_seen"
    }
}
