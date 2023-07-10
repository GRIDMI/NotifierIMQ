package net.gridmi.api.notifier.app

import java.io.Writer
import java.net.Socket

class Subscriber(val io: IO, val who: Who) {

    override fun toString(): String = "Subscriber(io=$io, who=$who)"

    class Who(val id: String, val topics: List<String>): java.io.Serializable {

        override fun toString(): String = "Who(id='$id', topics=$topics)"

        fun isCompletable(notification: Notification): Boolean {

            val chunks: List<String> = notification.to.split(Notification.SEPARATOR_STR)

            if (chunks.contains(Notification.exclude(Notification.ID, id))) return false

            if (chunks.contains(Notification.include(Notification.ID, id))) return true

            var included = 0

            for (t in topics) {

                if (chunks.contains(Notification.exclude(Notification.TOPIC, t))) return false

                if (chunks.contains(Notification.include(Notification.TOPIC, t))) included += 1

            }

            return included > 0// || who.topics.contains(Notification.ALL)

        }

    }

    class IO(private val socket: Socket) {

        val writer = socket.getOutputStream().bufferedWriter()
        val reader = socket.getInputStream().bufferedReader()

        @Synchronized
        fun isConnected(): Boolean = socket.isConnected

        @Synchronized
        fun disconnect() = socket.close()

        override fun toString(): String {
            return "IO(socket=$socket)"
        }

        companion object {

            fun Writer.writeLine(line: String) {
                write("$line\n")
                flush()
            }

        }

    }

}