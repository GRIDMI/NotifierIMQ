package net.gridmi.api.notifier.app

import net.gridmi.api.notifier.app.utils.Base64Utils.decodeFromBase64
import net.gridmi.api.notifier.app.utils.Base64Utils.encodeToBase64
import net.gridmi.api.notifier.app.Exchange.Companion.toExchange
import net.gridmi.api.notifier.app.utils.Json.fromJson
import net.gridmi.api.notifier.app.utils.Json.toJson
import net.gridmi.api.notifier.app.Subscriber.IO.Companion.writeLine
import net.gridmi.api.notifier.app.utils.TinyUtils.isInterrupted
import net.gridmi.api.notifier.app.utils.TinyUtils.timestamp
import net.gridmi.api.notifier.app.exceptions.BroadcasterException.Companion.call
import net.gridmi.api.notifier.app.exceptions.IOException
import net.gridmi.api.notifier.app.exceptions.TimeoutException
import java.net.Socket
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Client(
    private val host: String,
    private val port: Int,
    val who: Subscriber.Who,
    private val coder: Coder? = null,
    private val logger: Logger? = null) {

    private var io: Subscriber.IO? = null
    private var isRunning: Boolean = false

    var onExchange: Listener.OnExchange? = null
    var onNotification: Listener.OnNotification? = null

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

    fun isConnected(): Boolean = io?.isConnected() == true

    fun sendNotification(to: String, data: String): Boolean {
        return sendNotification(Notification(from = who.id, to = to, data = data))
    }

    @Synchronized
    fun sendNotification(n: Notification) = runCatching {
        io!!.writer.writeLine(encode(n.toJson()))
    }.isSuccess

    private fun onResponse(exchange: Exchange) {
        synchronized(responses) {
            responses.put(exchange.id, exchange)
        }
    }

    private fun onRequest(exchange: Exchange) {

        logger?.onInfo("Request received with parameters -> $exchange")

        val response = onExchange?.onExchange(exchange.clone()) ?: return

        exchange.to = Notification.include(Notification.ID, exchange.from)
        exchange.meta = response.meta
        exchange.data = response.data
        exchange.from = who.id

        sendNotification(exchange.buildResponse())

    }

    private fun onExchange(n: Notification) {
        n.runCatching {
            when {
                n.isExchange() -> {

                    val isRequest: Boolean = n.isRequest()

                    val exchange: Exchange = n.toExchange()

                    if (isRequest) onRequest(exchange)
                    else onResponse(exchange)

                }
                else -> error("This notification isn't exchange...")
            }
        }.onFailure {

        }
    }

    private fun onNotification(n: Notification) {
        if (n.isExchange()) onExchange(n)
        else onNotification?.onNotification(n)
    }

    override fun toString(): String {
        return "net.gridmi.Client(host='$host', port=$port, who=$who)"
    }

    @Synchronized
    private fun disconnect() {
        notifyReader?.interrupt()
        io?.disconnect()
        io = null
    }

    @Synchronized
    private fun connect() {
        disconnect()
        runCatching {

            val io = Subscriber.IO(Socket(host, port))

            runCatching {

                if (decode(io.reader.readLine()) != "INIT") {
                    error("Failed to initialize connection")
                }

                io.writer.writeLine(encode(this.who.toJson()))

                if (decode(io.reader.readLine()) != who.id) {
                    error("Unexpected server response")
                }

                this@Client.io = io

                notifyReader = Thread {

                    while (!isInterrupted() && isConnected()) try {
                        onNotification(decode(io.reader.readLine()).fromJson())
                    } catch (ignored: Throwable) {
                        break
                    }

                    lock.withLock {
                        stateCondition.signal()
                    }

                }
                notifyReader?.start()

            }.onFailure {
                io.disconnect()
                throw it
            }

        }.onFailure {
            throw it
        }
    }

    private var running: Thread? = null
    private var notifyReader: Thread? = null

    private val lock = ReentrantLock()

    private val stateCondition = lock.newCondition()

    fun start() = lock.withLock {

        this.isRunning = true
        stateCondition.signal()

        if (running == null) {
            running = Thread {
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
            running?.start()
        }

    }

    fun stop() = lock.withLock {
        this.isRunning = false
        stateCondition.signal()
    }

    private val responses = mutableMapOf<String, Exchange>()

    fun forExchange(meta: String, to: String, data: String): Exchange {
        return Exchange(meta = meta, from = who.id, to = to, data = data)
    }

    fun exchange(e: Exchange, timeout: Int = 30): Exchange {

        val start = timestamp(); val end = start + timeout

        val s: Boolean = sendNotification(e.buildRequest())

        if (!s) IOException("Failed for exchange -> $e").call()

        while (timestamp() < end) synchronized(responses) {
            responses.remove(e.id)?.let {
                return it
            }
        }

        TimeoutException("$start ... += $timeout seconds").call()

    }

    interface Listener {

        interface OnNotification: Listener {
            fun onNotification(notification: Notification)
        }

        interface OnExchange: Listener {
            fun onExchange(exchange: Exchange): Exchange.Result?
        }

    }

}