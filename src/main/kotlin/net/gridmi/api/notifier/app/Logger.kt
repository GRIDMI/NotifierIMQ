package net.gridmi.api.notifier.app

interface Logger {
    fun onInfo(info: String)
    fun onError(error: Throwable)
}