package com.codigitech.ft.platform

sealed interface BiometricResult {
    data object Success : BiometricResult
    data object Unavailable : BiometricResult
    data class Failed(val message: String) : BiometricResult
}

interface BiometricAuthenticator {
    fun isAvailable(): Boolean
    suspend fun authenticate(reason: String): BiometricResult
}
