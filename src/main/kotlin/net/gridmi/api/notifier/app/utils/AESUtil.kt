package net.gridmi.api.notifier.app.utils

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object AESUtil {

    private const val ALGORITHM_CIPHER = "AES"
    private const val ALGORITHM_FACTORY = "PBKDF2WithHmacSHA256"

    fun getKeyFrom(pass: String, salt: String): SecretKey {
        val factory = SecretKeyFactory.getInstance(ALGORITHM_FACTORY)
        return SecretKeySpec(factory.generateSecret(PBEKeySpec(
            pass.toCharArray(),
            salt.toByteArray(),
            65536,
            256
        )).encoded, ALGORITHM_CIPHER
        )
    }

    private val cipher: Cipher = Cipher.getInstance(ALGORITHM_CIPHER)

    @Synchronized
    fun encode(k:SecretKey, i: ByteArray): ByteArray = with(cipher) {
        init(Cipher.ENCRYPT_MODE, k)
        doFinal(i)
    }

    @Synchronized
    fun decode(k: SecretKey, i: ByteArray): ByteArray = with(cipher) {
        init(Cipher.DECRYPT_MODE, k)
        doFinal(i)
    }

}