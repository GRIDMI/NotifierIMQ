package net.gridmi.api.notifier.app.utils

import java.nio.charset.Charset
import java.util.*

object Base64Utils {

    private val base64Decoder: Base64.Decoder = Base64.getDecoder()

    @Synchronized
    fun ByteArray.decodeFromBase64(): ByteArray = base64Decoder.decode(this)

    private val base64Encoder: Base64.Encoder = Base64.getEncoder()

    @Synchronized
    fun ByteArray.encodeToBase64(): ByteArray = base64Encoder.encode(this)

    fun String.toBase64(charset: Charset = Charsets.UTF_8): String {
        return toByteArray(charset).encodeToBase64().decodeToString()
    }

    fun String.fromBase64(charset: Charset = Charsets.UTF_8): String {
        return String(toByteArray(Charsets.UTF_8).decodeFromBase64(), charset)
    }

}