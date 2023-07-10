package net.gridmi.api.notifier.app

import net.gridmi.api.notifier.app.utils.AESUtil

class Coder(pass: String, salt: String) {

    private val key = AESUtil.getKeyFrom(pass, salt)

    fun decode(byteArray: ByteArray): ByteArray {
        return AESUtil.decode(key, byteArray)
    }

    fun encode(byteArray: ByteArray): ByteArray {
        return AESUtil.encode(key, byteArray)
    }

}