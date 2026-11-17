package uz.moliya.app

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object Biometrics {
    private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_WEAK

    /** Qurilmada barmoq izi/yuz sozlangan va ishlatish mumkinmi. */
    fun available(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    fun prompt(activity: FragmentActivity, onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Moliya")
            .setSubtitle("Barmoq izi yoki yuz bilan oching")
            .setNegativeButtonText("PIN kiritish")
            .setAllowedAuthenticators(AUTH)
            .build()
        BiometricPrompt(activity, executor, callback).authenticate(info)
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is FragmentActivity) return c
        c = c.baseContext
    }
    return null
}
