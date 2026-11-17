package uz.moliya.app

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Ilova sozlamalari (moliyaviy ma'lumotdan alohida saqlanadi va Excel'ga tushmaydi). */
data class AppSettings(
    val pinHash: String = "",
    val pinSalt: String = "",
    val biometric: Boolean = false,
    /** 0 = tizim, 1 = yorug', 2 = qorong'i */
    val theme: Int = 0,
    val reminders: Boolean = false,
    /** Haftalik avtomatik Excel zaxira */
    val autoBackup: Boolean = false,
    val backupFolder: String = "",
    val lastAutoBackup: Long = 0L,
) {
    val hasPin: Boolean get() = pinHash.isNotEmpty()
}

object Prefs {
    private const val NAME = "moliya_prefs"

    fun load(context: Context): AppSettings {
        val sp = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return AppSettings(
            pinHash = sp.getString("pinHash", "") ?: "",
            pinSalt = sp.getString("pinSalt", "") ?: "",
            biometric = sp.getBoolean("biometric", false),
            theme = sp.getInt("theme", 0),
            reminders = sp.getBoolean("reminders", false),
            autoBackup = sp.getBoolean("autoBackup", false),
            backupFolder = sp.getString("backupFolder", "") ?: "",
            lastAutoBackup = sp.getLong("lastAutoBackup", 0L),
        )
    }

    fun save(context: Context, s: AppSettings) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("pinHash", s.pinHash)
            .putString("pinSalt", s.pinSalt)
            .putBoolean("biometric", s.biometric)
            .putInt("theme", s.theme)
            .putBoolean("reminders", s.reminders)
            .putBoolean("autoBackup", s.autoBackup)
            .putString("backupFolder", s.backupFolder)
            .putLong("lastAutoBackup", s.lastAutoBackup)
            .apply()
    }
}

/** Worker faqat shu maydonni yangilaydi (sozlamalarning qolgan qismiga tegmaydi). */
fun Prefs.setLastAutoBackup(context: Context, millis: Long) {
    context.getSharedPreferences("moliya_prefs", Context.MODE_PRIVATE).edit()
        .putLong("lastAutoBackup", millis).apply()
}

object Pin {
    const val LENGTH = 4

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { b -> "%02x".format(b) }
    }

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt.toByteArray(Charsets.UTF_8), 20000, 256)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return key.joinToString("") { b -> "%02x".format(b) }
    }

    fun verify(pin: String, s: AppSettings): Boolean {
        if (!s.hasPin) return true
        val candidate = hash(pin, s.pinSalt).toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(candidate, s.pinHash.toByteArray(Charsets.UTF_8))
    }
}
