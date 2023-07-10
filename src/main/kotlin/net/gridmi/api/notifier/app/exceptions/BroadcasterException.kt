package net.gridmi.api.notifier.app.exceptions

open class BroadcasterException(m: String): Exception(m) {
    companion object {
        fun BroadcasterException.call(): Nothing = throw this
    }
}

class TimeoutException(m: String): BroadcasterException(m)

class IOException(m: String): BroadcasterException(m)