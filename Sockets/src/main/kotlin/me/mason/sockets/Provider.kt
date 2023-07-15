package me.mason.sockets

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate
import java.nio.ByteBuffer.allocateDirect
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val EMPTY_BUFFER: ByteBuffer = allocate(0)

typealias Address = InetSocketAddress

interface Connection {
    val open: Boolean
    fun <T> read(block: Read.() -> T): T
    fun write(block: Write.() -> (Unit))
    fun close()
    val read: Read
    val write: Write
}

interface Read {
    fun byte(): Byte
    fun char(): Char
    fun short(): Short
    fun int(): Int
    fun float(): Float
    fun double(): Double
    fun long(): Long
}

interface Write {
    fun byte(byte: Byte)
    fun char(char: Char)
    fun short(short: Short)
    fun int(int: Int)
    fun float(float: Float)
    fun double(double: Double)
    fun long(long: Long)
}


suspend fun Connection(
    channel: SocketChannel,
    context: CoroutineContext,
    block: suspend Connection.(CoroutineContext) -> (Unit)
) {
    coroutineScope<Unit> { launch(context) {
        val writeBuffer = allocate(Short.MAX_VALUE.toInt())
        val readBuffer = allocate(Short.MAX_VALUE.toInt())
        val connection = object : Connection {
            override var open = true
            override fun <T> read(block: Read.() -> T): T = read.block()
            override fun write(block: Write.() -> (Unit)) { write.block() }
            override fun close() {
                if (!open) return
                open = false
                channel.close()
            }
            override val read = object : Read {
                var cursor = 0
                override fun byte(): Byte = read(1) { get(cursor) }
                override fun char(): Char = read(1) { getChar(cursor) }
                override fun short(): Short = read(2) { getShort(cursor) }
                override fun int(): Int = read(4) { getInt(cursor) }
                override fun float(): Float = read(4) { getFloat(cursor) }
                override fun double(): Double = read(8) { getDouble(cursor) }
                override fun long(): Long = read(8) { getLong(cursor) }
                fun <T> read(size: Int, block: ByteBuffer.() -> (T)): T {
                    if (readBuffer.position() - cursor >= size) {
                        val result = readBuffer.block()
                        cursor += size
                        return result
                    }
                    if (readBuffer.limit() - readBuffer.position() < size) {
                        val dest = ByteArray(readBuffer.position() - cursor)
                        readBuffer.get(cursor, dest)
                        readBuffer.clear()
                        readBuffer.put(dest)
                        cursor = 0
                    }
                    try {
                        channel.read(readBuffer)
                        val result = readBuffer.block()
                        cursor += size
                        return result
                    } catch (throwable: Throwable) {
                        open = false
                        close()
//                        throwable.printStackTrace()
                        error("Closed")
                    }
                }
            }
            override val write = object : Write {
                override fun byte(byte: Byte) { write { put(byte) } }
                override fun char(char: Char) { write { putChar(char) } }
                override fun int(int: Int) { write { putInt(int) } }
                override fun float(float: Float) { write { putFloat(float) } }
                override fun double(double: Double) { write { putDouble(double) } }
                override fun long(long: Long) { write { putLong(long) } }
                override fun short(short: Short) { write { putShort(short) } }
                fun write(block: ByteBuffer.() -> (ByteBuffer)) {
                    writeBuffer.apply {
                        try {
                            channel.write(block().flip())
                        } catch (throwable: Throwable) {
                            open = false
                            close()
//                            throwable.printStackTrace()
                            error("Closed")
                        }
                        position(0)
                        limit(capacity())
                    }
                }
            }
        }
        connection.block(context)
    } }
}

suspend fun accept(port: Int, block: suspend Connection.(CoroutineContext) -> (Unit)) {
    val dispatcher = ThreadPoolExecutor(
        0, Int.MAX_VALUE,
        1L, TimeUnit.SECONDS,
        SynchronousQueue()
    ).asCoroutineDispatcher()
    val serverChannel = ServerSocketChannel.open().bind(Address(port))
    GlobalScope.launch(dispatcher) {
        while(isActive) {
            val channel = serverChannel.accept()
            try { Connection(channel, coroutineContext, block) }
            catch (_: Throwable) {
//                println(it.message)
            }
        }
    }
}

suspend fun connect(addr: String, port: Int, block: suspend Connection.(CoroutineContext) -> (Unit)) {
    val address = Address(addr, port)
    val channel = SocketChannel.open()
    if (!channel.connect(address)) return
    val dispatcher = ThreadPoolExecutor(
        0, Int.MAX_VALUE,
        1L, TimeUnit.SECONDS,
        SynchronousQueue()
    ).asCoroutineDispatcher()
    GlobalScope.launch(dispatcher) {
        while(!channel.isConnected) { delay(50) }
        try { Connection(channel, coroutineContext, block) }
        catch(_: Throwable) {
//            println(it.message)
        }
//        cancel("gg")
    }
}