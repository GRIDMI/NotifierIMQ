package net.gridmi.api.notifier.app

import net.gridmi.api.notifier.app.utils.KV.Companion.stringPresentation
import net.gridmi.api.notifier.app.utils.KV.Companion.toListKV
import net.gridmi.api.notifier.app.utils.TinyUtils.timestamp
import net.gridmi.api.notifier.app.utils.TinyUtils.uuid
import net.gridmi.api.notifier.app.utils.Json
import net.gridmi.api.notifier.app.utils.KV

class Notification(
    private val meta: String? = null,
    val from: String,
    val to: String,
    val data: String,
): java.io.Serializable, Json.OnAfterInit {

    constructor(from: String, to: String, data: String):
            this(null, from, to, data)

    val id: String = uuid()
    val created = timestamp()

    @Transient
    private lateinit var metaFact: Meta

    fun getMeta(): Meta = this.metaFact

    override fun onAfterInit() {
        metaFact = Meta(meta ?: "")
    }

    fun isExchange(): Boolean {
        return isRequest() || isResponse()
    }

    fun isRequest() = getMeta().let {
        it.req != null && it.mes != null
    }

    fun isResponse() = getMeta().let {
        it.res != null && it.mes != null
    }

    override fun toString(): String {
        return "Notification(id='$id', meta='$meta', from='$from', to='$to', data='$data', created=$created)"
    }

    companion object {

        const val ID = "id"
        const val TOPIC = "topic"
        const val SYSTEM = "system"

        const val SEPARATOR_STR: String = ";"

        private const val EXCLUDE: String = "exclude"

        fun exclude(mark: String, to: String): String {
            return "$EXCLUDE.${include(mark, to)}"
        }

        fun include(mark: String, to: String): String {
            return "$mark=$to"
        }

        fun joinWithSeparator(list: List<String>): String {
            return list.joinToString(SEPARATOR_STR)
        }

        fun joinWithSeparator(vararg chunks: String): String {
            return joinWithSeparator(chunks.toList())
        }

    }

    data class Meta(private val raw: String) {

        private val current = raw.toListKV()

        val req: String? by lazy { find(REQ) }
        val res: String? by lazy { find(RES) }
        val mes: String? by lazy { find(MES) }

        fun find(key: String): String? {
            return current.firstOrNull {
                it.key == key
            }?.value
        }

        data class Builder(private val raw: String = "") {

            private val current = this@Builder.raw.toListKV().toMutableList()

            private fun add(check: Boolean, key: String, value: String): Boolean {
                if (check && key.isSystemMetaKey()) error("Key for system -> $key")
                return current.add(KV(key, value))
            }

            fun add(key: String, value: String): Boolean = add(true, key, value)

            fun addExchange(isRequest: Boolean, id: String, mes: String): Boolean {
                return add(false, if (isRequest) REQ else RES, id) && add(false, MES, mes)
            }

            fun build(): String = current.stringPresentation()

            fun clear() = current.clear()

        }

        companion object {

            const val REQ = "req"
            const val RES = "res"
            const val MES = "mes"

            private fun String.isSystemMetaKey(): Boolean {
                return this == REQ || this == RES || this == MES
            }

        }

    }

}