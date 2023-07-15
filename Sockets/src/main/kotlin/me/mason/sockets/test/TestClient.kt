package me.mason.socks.test

import kotlinx.coroutines.*
import me.mason.sockets.connect

private const val PING = 0.toByte()

suspend fun main() {
    connect("localhost", 9999) { context ->
        write.byte(PING)
        while (context.isActive) {
            val op = read.byte()
            if (op == PING) {
                println("read ping")
                write.byte(PING)
            }
            delay(50)
        }
    }
}