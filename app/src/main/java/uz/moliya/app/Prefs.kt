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
    /** Skrinshot va oxirgi ilovalar ko'rinishini yashirish */
    val secureScreen: Boolean = false,
    /** Birinchi sozlash takliflari ko'rsatilganmi */
    val onboarded: Boolean = false,
    /** Android 12+: ilova rangi fon rasmiga moslashadi */
    val dynamicColor: Boolean = false,
    val userName: String = "",
    /** Fonda qancha turgach qayta qulflanadi (soniya) */
    val lockDelaySec: Int = 15,
    val backupReminder: Boolean = true,
    val dailyReminder: Boolean = false,
    val dailyHour: Int = 21,
    val dailyMinute: Int = 0,
    /** 0 oddiy, 1 katta, 2 juda katta */
    val textScale: Int = 0,
    val compactNumbers: Boolean = false,
    /** Bosh sahifa kartalari: 1 grafik, 2 kategoriyalar, 4 jamg'arma darajasi, 8 jamg'arma/qarz, 16 tahlil */
    val homeCards: Int = 31,
    /** Hisob oyi boshlanish kuni (1..28) */
    val monthStartDay: Int = 1,
    val backupKeep: Int = 4,
    /** 0 = yoqilgan paytdan 7 kun; 1..7 = Calendar.DAY_OF_WEEK */
    val backupWeekday: Int = 0,
    /** Birinchi yozuv kiritilgan vaqt (zaxira eslatmasi shundan hisoblanadi) */
    val firstEntryAt: Long = 0L,
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
            secureScreen = sp.getBoolean("secureScreen", false),
            onboarded = sp.getBoolean("onboarded", false),
            dynamicColor = sp.getBoolean("dynamicColor", false),
            userName = sp.getString("userName", "") ?: "",
            lockDelaySec = sp.getInt("lockDelaySec", 15),
            backupReminder = sp.getBoolean("backupReminder", true),
            dailyReminder = sp.getBoolean("dailyReminder", false),
            dailyHour = sp.getInt("dailyHour", 21),
            dailyMinute = sp.getInt("dailyMinute", 0),
            textScale = sp.getInt("textScale", 0),
            compactNumbers = sp.getBoolean("compactNumbers", false),
            homeCards = sp.getInt("homeCards", 31),
            monthStartDay = sp.getInt("monthStartDay", 1),
            backupKeep = sp.getInt("backupKeep", 4),
            backupWeekday = sp.getInt("backupWeekday", 0),
            firstEntryAt = sp.getLong("firstEntryAt", 0L),
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
            .putBoolean("secureScreen", s.secureScreen)
            .putBoolean("onboarded", s.onboarded)
            .putBoolean("dynamicColor", s.dynamicColor)
            .putString("userName", s.userName)
            .putInt("lockDelaySec", s.lockDelaySec)
            .putBoolean("backupReminder", s.backupReminder)
            .putBoolean("dailyReminder", s.dailyReminder)
            .putInt("dailyHour", s.dailyHour)
            .putInt("dailyMinute", s.dailyMinute)
            .putInt("textScale", s.textScale)
            .putBoolean("compactNumbers", s.compactNumbers)
            .putInt("homeCards", s.homeCards)
            .putInt("monthStartDay", s.monthStartDay)
            .putInt("backupKeep", s.backupKeep)
            .putInt("backupWeekday", s.backupWeekday)
            .putLong("firstEntryAt", s.firstEntryAt)
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
