package net.gridmi.api.notifier.app

import net.gridmi.api.notifier.app.utils.TinyUtils

data class Exchange(
    var id: String,
    var meta: String,
    var from: String,
    var to: String,
    var data: String
) {

    constructor(meta: String, from: String, to: String, data: String):
            this(TinyUtils.uuid(), meta, from, to, data)

    fun buildRequest(): Notification = build(true)

    private fun build(isRequest: Boolean): Notification {
        val m: String = with(Notification.Meta.Builder()) {
            addExchange(isRequest, id, meta)
            build()
        }
        return Notification(m, from, to, data)
    }

    fun buildResponse(): Notification = build(false)

    fun clone() = Exchange(id, meta, from, to, data)

    override fun toString(): String {
        return "Exchange(id='$id', meta='$meta', from='$from', to='$to', data='$data')"
    }

    data class Result(
        val meta: String,
        val data: String
    ) {

        companion object {

            fun like(code: Int, message: String, data: String = ""): Result {
                return Result("$code $message", data)
            }

        }

    }

    companion object {

        fun Notification.toExchange(): Exchange {
            val id: String = with(if (isRequest()) getMeta().req else getMeta().res) {
                if (this == null) error("Not found meta `req/res` in ${this@toExchange}")
                this
            }
            val meta: String = getMeta().mes ?: error("Not found meta=`mes` in $this")
            return Exchange(id = id, meta = meta, from = from, to = to, data = data)
        }

    }

}