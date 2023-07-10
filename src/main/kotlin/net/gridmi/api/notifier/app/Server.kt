package net.gridmi.api.notifier.app

import net.gridmi.api.notifier.app.utils.Base64Utils.decodeFromBase64
import net.gridmi.api.notifier.app.utils.Base64Utils.encodeToBase64
import net.gridmi.api.notifier.app.Exchange.Companion.toExchange
import net.gridmi.api.notifier.app.utils.Json.fromJson
import net.gridmi.api.notifier.app.utils.Json.toJson
import net.gridmi.api.notifier.app.utils.KV.Companion.toListKV
import net.gridmi.api.notifier.app.utils.ParamContainer.Companion.toContainer
import net.gridmi.api.notifier.app.utils.TinyUtils.isInterrupted
import net.gridmi.api.notifier.app.utils.TinyUtils.toList
import net.gridmi.api.notifier.app.utils.ParamContainer
import java.io.BufferedWriter
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Server(
    private val port: Int,
    private val coder: Coder? = null,
    private val logger: Logger? = null) {

    private var socketServer: ServerSocket? = null
    private val executor = Executors.newSingleThreadExecutor()

    private fun encode(string: String): String {
        return encode(string.toByteArray()).decodeToString()
    }

    private fun decode(string: String): String {
        return decode(string.toByteArray()).decodeToString()
    }

    private fun encode(byteArray: ByteArray): ByteArray {
        val arr = coder?.encode(byteArray) ?: byteArray
        return arr.encodeToBase64()
    }

    private fun decode(byteArray: ByteArray): ByteArray {
        val arr: ByteArray = byteArray.decodeFromBase64()
        return coder?.decode(arr) ?: arr
    }

    private fun onWrite(writer: BufferedWriter, raw: String) {
        writer.write("$raw\n")
        writer.flush()
    }

    private fun getRaw(notification: Notification): String {
        val json: String = notification.toJson()
        val encoded = encode(json.toByteArray())
        return encoded.decodeToString()
    }

    private fun onSystemResponse(exchange: Exchange): Notification {
        return Notification("", "", "")
    }

    private fun onSystemRequest(exchange: Exchange): Notification {

        val params: ParamContainer = exchange.meta.toListKV().toContainer()

        val result: Exchange.Result = when(params.findParam("action")) {
            "findIdsWhoContainsTopics" -> run {

                val topics = params.findParam("topics")?.toList(";") ?:
                return@run Exchange.Result.like(400, "No param `topics`")

                val ids = synchronized(subscribers) {
                    val ret: MutableSet<String> = mutableSetOf()
                    for (s in subscribers) for (t in s.who.topics) {
                        if (topics.contains(t)) ret.add(s.who.id)
                    }
                    return@synchronized ret
                }.joinToString(";")

                Exchange.Result.like(200, "Ids are fetched...", ids)

            }
            else -> Exchange.Result.like(400, "Not implemented action")
        }

        exchange.to = Notification.include(Notification.ID, exchange.from)
        exchange.meta = result.meta
        exchange.data = result.data
        exchange.from = Notification.SYSTEM

        return exchange.buildResponse()

    }

    private fun onNotify(notification: Notification): Notification? {
        return null
    }

    @Synchronized
    private fun onSystemNotification(notification: Notification) {
        if (notification.to != Notification.SYSTEM) return
        val w = synchronized(subscribers) {
            subscribers.firstOrNull {
                notification.from == it.who.id
            }
        }?.io?.writer ?: return
        val n: Notification? = when(!notification.isExchange()) {
            true -> onNotify(notification)
            else -> {

                val isRequest: Boolean = notification.isRequest()

                val exchange: Exchange = notification.toExchange()

                if (isRequest) onSystemRequest(exchange)
                else onSystemResponse(exchange)

            }
        }
        if (n != null) this.onWrite(writer = w, raw = this.getRaw(n))
    }

    @Synchronized
    private fun onNotification(notification: Notification) {

        if (notification.to == Notification.SYSTEM) {
            return onSystemNotification(notification)
        }

        val raw = getRaw(notification)

        synchronized(subscribers) {
            for (s in subscribers) {
                try {

                    if (s.who.isCompletable(notification)) {

                        onWrite(writer = s.io.writer, raw)

                        if (notification.isExchange()) break

                    }

                } catch (ignored: Throwable) {
                    ignored.printStackTrace()
                    s.io.disconnect()
                }
            }
        }

    }

    private val subscribers = mutableListOf<Subscriber>()

    private fun onNewSubscriber(subscriber: Subscriber) {
        synchronized(subscribers) {

            subscribers.add(subscriber)

            Thread {

                while (subscriber.io.isConnected()) {
                    try {

                        val line: String = subscriber.io.reader.readLine()

                        onNotification(decode(line.toByteArray()).fromJson())

                    } catch (ignored: Throwable) {
                        break
                    }
                }

                synchronized(subscribers) {
                    subscribers.remove(subscriber)
                }

                subscriber.io.disconnect()

            }.start()

        }
    }

    private fun isConnected() = !(socketServer?.isClosed ?: true)

    @Synchronized
    private fun disconnect() {
        threadOfAccept?.interrupt()
        socketServer?.close()
        socketServer = null
    }

    private var threadOfAccept: Thread? = null

    @Synchronized
    private fun connect() {

        logger?.onInfo("Connect server $this")

        disconnect()
        socketServer = ServerSocket(port)

        threadOfAccept = Thread {

            while (!isInterrupted() && isConnected()) {

                val s = socketServer?.accept() ?: break

                executor.execute {

                    var result: Any? = null

                    val io = Subscriber.IO(s)

                    runCatching {

                        onWrite(io.writer, encode(string = "INIT"))

                        val raw = decode(io.reader.readLine())
                        result = Subscriber(io, raw.fromJson())

                        onNewSubscriber(result as Subscriber)

                    }.onFailure {
                        result = it
                    }

                    runCatching {
                        val ret: String = with(result) {

                            (result as? Subscriber)?.who?.id?.let {
                                return@with it
                            }

                            (this as? Throwable)?.message ?: "NO RET"

                        }
                        onWrite(io.writer, raw = encode(ret))
                    }

                    if (result !is Subscriber) {
                        io.disconnect()
                    }

                }

            }

            lock.withLock {
                stateCondition.signal()
            }

        }
        threadOfAccept?.start()

    }

    private val lock = ReentrantLock()

    private val stateCondition = lock.newCondition()

    private var threadOfConnect: Thread? = null

    private var isRunning: Boolean = false

    fun start() = lock.withLock {
        this.isRunning = true
        stateCondition.signal()
        if (threadOfConnect == null) {
            threadOfConnect = Thread {
                while (true) lock.withLock {
                    runCatching {
                        if (!isRunning) {
                            disconnect()
                        } else {
                            connect()
                        }
                    }.onSuccess {
                        stateCondition.await()
                    }
                }
            }
            threadOfConnect?.start()
        }
    }

    fun stop() = lock.withLock {
        this.isRunning = false
        stateCondition.signal()
    }

}