package net.gridmi.api.notifier

import net.gridmi.api.notifier.app.*
import net.gridmi.api.notifier.app.utils.ParamContainer

fun main(args: Array<String>) {

    val params = ParamContainer.from(args)
    println("Initialization by params: $params")

    val port = params.findParam("port")?.toIntOrNull()
        ?: error("Port must be like integer")

    val coder = params.findParam("coder")?.let {
        val chunks = it.split(":", limit = 2)
        if (chunks.size != 2) error("Coder must be PASS:SALT")
        Coder(pass = chunks.first(), salt = chunks.last())
    }

    val logger = when(params.existParam("logger")) {
        true -> object : Logger {

            override fun onInfo(info: String) {
                println(info)
            }

            override fun onError(error: Throwable) {
                error.printStackTrace()
            }

        }
        else -> null
    }

    when(params.findParam("type")) {
        "client" -> {

            val host = params.findParam("host")
                ?: error("Host for client is required!")

            val who = params.findParam("who")
                ?: error("Who for client is required!")

            val w: List<String> = who.split("#", limit = 2)
            if (w.size != 2) error("Who format must be: ID#0,1,2...")

            Client(
                host = host,
                port = port,
                who = Subscriber.Who(
                    w.first(),
                    w.last().split(",")
                ),
                coder = coder,
                logger = logger
            ).start()

        }
        "server" -> {

            Server(
                port = port,
                coder = coder,
                logger = logger
            ).start()

        }
        else -> error("Type must be client or server")
    }

    Thread.currentThread().join()

}
