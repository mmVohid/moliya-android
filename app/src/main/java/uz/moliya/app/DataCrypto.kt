package uz.moliya.app

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Ma'lumot faylini AES-256-GCM bilan shifrlaydi. Kalit Android Keystore ichida, telefondan chiqmaydi. */
object DataCrypto {
    private const val ALIAS = "moliya_aes256"
    private val MAGIC = byteArrayOf(77, 69, 78, 67, 1) // "MENC" + versiya
    private const val IV_SIZE = 12

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size > MAGIC.size + IV_SIZE && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return MAGIC + cipher.iv + cipher.doFinal(plain)
    }

    fun decrypt(bytes: ByteArray): ByteArray {
        val iv = bytes.copyOfRange(MAGIC.size, MAGIC.size + IV_SIZE)
        val body = bytes.copyOfRange(MAGIC.size + IV_SIZE, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return cipher.doFinal(body)
    }
}

/** Excel zaxirani parol bilan shifrlaydi (PBKDF2 + AES-256-GCM). Shifrlangan fayl Excelda ochilmaydi, faqat Moliya'da tiklanadi. */
object BackupCrypto {
    private val MAGIC = byteArrayOf(77, 66, 75, 67, 1) // "MBKC" + versiya
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val ITERATIONS = 120_000

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size > MAGIC.size + SALT_SIZE + IV_SIZE && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    private fun derive(password: String, salt: ByteArray): javax.crypto.spec.SecretKeySpec {
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        val bytes = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return javax.crypto.spec.SecretKeySpec(bytes, "AES")
    }

    fun encrypt(plain: ByteArray, password: String): ByteArray {
        val random = java.security.SecureRandom()
        val salt = ByteArray(SALT_SIZE).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, derive(password, salt))
        return MAGIC + salt + cipher.iv + cipher.doFinal(plain)
    }

    /** Parol noto'g'ri bo'lsa xatolik tashlaydi. */
    fun decrypt(bytes: ByteArray, password: String): ByteArray {
        val salt = bytes.copyOfRange(MAGIC.size, MAGIC.size + SALT_SIZE)
        val iv = bytes.copyOfRange(MAGIC.size + SALT_SIZE, MAGIC.size + SALT_SIZE + IV_SIZE)
        val body = bytes.copyOfRange(MAGIC.size + SALT_SIZE + IV_SIZE, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(password, salt), GCMParameterSpec(128, iv))
        return cipher.doFinal(body)
    }

    /** Parol Keystore kaliti bilan shifrlanib saqlanadi (avto-zaxira fonda ishlashi uchun). */
    fun sealPassword(password: String): String =
        android.util.Base64.encodeToString(DataCrypto.encrypt(password.toByteArray(Charsets.UTF_8)), android.util.Base64.NO_WRAP)

    fun openPassword(sealed: String): String? = try {
        String(DataCrypto.decrypt(android.util.Base64.decode(sealed, android.util.Base64.NO_WRAP)), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }

    /** Zaxira baytlari va shifrlanganmi. */
    fun export(context: android.content.Context, data: FinanceData): Pair<ByteArray, Boolean> {
        val buffer = java.io.ByteArrayOutputStream()
        XlsxExporter.write(data, buffer)
        val settings = Prefs.load(context)
        val password = if (settings.backupEncrypted) openPassword(settings.backupPwd) else null
        return if (!password.isNullOrEmpty()) Pair(encrypt(buffer.toByteArray(), password), true) else Pair(buffer.toByteArray(), false)
    }
}
