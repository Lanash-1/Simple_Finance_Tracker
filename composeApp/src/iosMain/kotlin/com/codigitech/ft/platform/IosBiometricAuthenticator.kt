package com.codigitech.ft.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosBiometricAuthenticator : BiometricAuthenticator {
    override fun isAvailable(): Boolean =
        LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)

    override suspend fun authenticate(reason: String): BiometricResult {
        if (!isAvailable()) return BiometricResult.Unavailable
        val context = LAContext().apply { localizedFallbackTitle = "Use PIN" }
        return suspendCancellableCoroutine { cont ->
            context.evaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, reason) { success, error ->
                if (!cont.isActive) return@evaluatePolicy
                if (success) cont.resume(BiometricResult.Success)
                else cont.resume(BiometricResult.Failed(error?.localizedDescription ?: "Authentication failed"))
            }
        }
    }
}
