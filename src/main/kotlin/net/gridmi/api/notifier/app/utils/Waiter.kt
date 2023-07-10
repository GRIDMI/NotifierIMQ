package net.gridmi.api.notifier.app.utils

class Waiter<T>(@Volatile var data: T? = null) {
    fun await(timeout: Long = 5000): T {
        val end: Long = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() <= end) data?.let {
            return it
        }
        error("Timeout [$timeout] reached while await data")
    }
}