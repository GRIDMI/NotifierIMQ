package net.gridmi.api.notifier.app.utils

import net.gridmi.api.notifier.app.utils.Base64Utils.fromBase64
import net.gridmi.api.notifier.app.utils.Base64Utils.toBase64
import net.gridmi.api.notifier.app.utils.TinyUtils.toList

data class KV(val key: String, val value: String) {

    private fun valueBase64() = value.toBase64()

    private fun keyBase64() = key.toBase64()

    override fun toString(): String {
        return "KV(key='$key', value='$value')"
    }

    fun build(): String {
        val k = keyBase64()
        val v = valueBase64()
        return "$k$SEPARATOR_A$v"
    }

    companion object {

        private const val SEPARATOR_A: String = "&"
        private const val SEPARATOR_B: String = ";"

        fun List<KV>.stringPresentation(): String {
            return joinToString(SEPARATOR_B) {
                it.build()
            }
        }

        fun String.toListKV() = toList(SEPARATOR_B).toListKV()

        fun List<String>.toListKV(): List<KV> = mapNotNull {
            from(it)
        }

        fun from(raw: String?) = raw?.runCatching {
            val chunked: List<String> = split(SEPARATOR_A, limit = 2)
            KV(chunked[0].fromBase64(), chunked[1].fromBase64())
        }?.getOrNull()

    }

}