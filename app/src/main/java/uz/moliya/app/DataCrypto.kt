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
