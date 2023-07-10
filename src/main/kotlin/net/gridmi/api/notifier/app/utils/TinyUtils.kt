package net.gridmi.api.notifier.app.utils

import java.util.UUID

object TinyUtils {
    fun isInterrupted(): Boolean {
        return Thread.currentThread().isInterrupted
    }
    fun timestamp() = (System.currentTimeMillis() / 1000L).toInt()
    fun uuid(): String = UUID.randomUUID().toString()

    fun String.toList(separator: String) = when(isNotBlank()) {
        true -> split(separator)
        else -> emptyList()
    }

}