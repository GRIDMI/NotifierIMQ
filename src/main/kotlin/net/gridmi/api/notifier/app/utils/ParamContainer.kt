package net.gridmi.api.notifier.app.utils

class ParamContainer(private val params: List<Param>) {

    fun existParam(key: String): Boolean = findParam(key) != null

    fun findParam(key: String) = params.firstOrNull {
        it.key == key
    }?.value

    override fun toString(): String {
        return "net.gridmi.ParamContainer(params=$params)"
    }

    class Param(val key: String, val value: String) {

        override fun toString(): String {
            return "Param(key='$key', value='$value')"
        }

        companion object {
            fun from(args: Array<String>): List<Param> {
                return args.mapNotNull {

                    val chunks = it.split("=", limit = 2)
                    if (chunks.isEmpty()) return@mapNotNull null

                    Param(chunks.first(), chunks.lastOrNull() ?: "")

                }
            }
        }

    }

    companion object {

        fun from(args: Array<String>): ParamContainer {
            return ParamContainer(Param.from(args))
        }

        fun List<KV>.toParams(): List<Param> = map {
            Param(it.key, it.value)
        }

        fun List<KV>.toContainer(): ParamContainer {
            return ParamContainer(toParams())
        }

    }

}