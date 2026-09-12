package com.codigitech.ft.platform

import com.codigitech.ft.Brand
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Needs the current FragmentActivity; MainActivity registers itself via [ActivityHolder]. */
object ActivityHolder {
    var current: FragmentActivity? = null
}

class AndroidBiometricAuthenticator : BiometricAuthenticator {
    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    override fun isAvailable(): Boolean {
        val activity = ActivityHolder.current ?: return false
        return BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun authenticate(reason: String): BiometricResult {
        val activity = ActivityHolder.current ?: return BiometricResult.Unavailable
        if (!isAvailable()) return BiometricResult.Unavailable
        return suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (cont.isActive) cont.resume(BiometricResult.Success)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (cont.isActive) cont.resume(BiometricResult.Failed(errString.toString()))
                    }

                    override fun onAuthenticationFailed() {
                        // Called on a single bad attempt; the prompt stays open, so keep waiting.
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(Brand.APP_NAME)
                .setSubtitle(reason)
                .setNegativeButtonText("Use PIN")
                .setAllowedAuthenticators(authenticators)
                .build()
            prompt.authenticate(info)
            cont.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }
}
