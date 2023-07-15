package me.mason.socks.test

import kotlinx.coroutines.*
import me.mason.sockets.accept

private const val PING = 0.toByte()

suspend fun main() {
    accept(9999) { context ->
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