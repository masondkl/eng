package me.mason.sockets

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.lang.Runtime.getRuntime
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.text.Charsets.UTF_8

interface Read {
    suspend fun bytes(size: Int): ByteArray
    suspend fun bool(): Boolean
    suspend fun byte(): Byte
    suspend fun char(): Char
    suspend fun short(): Short
    suspend fun int(): Int
    suspend fun float(): Float
    suspend fun double(): Double
    suspend fun long(): Long
    suspend fun string(): String
}
interface Write {
    suspend fun bytes(bytes: ByteArray)
    suspend fun bool(bool: Boolean)
    suspend fun byte(byte: Byte)
    suspend fun char(char: Char)
    suspend fun short(short: Short)
    suspend fun int(int: Int)
    suspend fun float(float: Float)
    suspend fun double(double: Double)
    suspend fun long(long: Long)
    suspend fun string(string: String)
}
interface Connection : Read, Write {
    val dispatcher: CoroutineDispatcher
    val open: Boolean
    val read: Read
    val write: Write
    suspend fun close()
    suspend fun onClose(block: suspend () -> (Unit))
}
suspend fun Connection(dispatcher: CoroutineDispatcher, channel: SocketChannel): Connection {
    val isOpen = AtomicBoolean(true)
    val writeBuffer = allocate(Short.MAX_VALUE.toInt())
    val readBuffer = allocate(Short.MAX_VALUE.toInt())
    val tempBytes = ByteArray(Short.MAX_VALUE.toInt())
    val closeEvents = ArrayList<suspend () -> (Unit)>()
    suspend fun closeEvents() = closeEvents.forEach { it() }
    val read = object : Read {
        var cursor = 0
        suspend fun <T> read(size: Int, block: ByteBuffer.() -> (T)): T? = readBuffer.run {
            if (!isOpen.get()) return null
            if (remaining() < size) {
                val length = position() - cursor
                get(cursor, tempBytes, 0, length)
                position(0)
                put(tempBytes, 0, length)
                cursor = 0
            }
            while (isOpen.get() && position() - cursor < size) {
                try {
                    if (channel.read(readBuffer) == -1) error("End of stream")
                } catch (err: Throwable) {
                    if (isOpen.get()) closeEvents()
                    isOpen.set(false)
                    try { channel.close() }
                    catch (_: Throwable) { }
                    return null
                }
                delay(1)
            }
            val result = block()
            cursor += size
            result
        }
        override suspend fun bytes(size: Int): ByteArray = read(size) {
            val bytes = ByteArray(size)
            get(cursor, bytes)
            bytes
        }!!

        override suspend fun bool(): Boolean = read(1) { get(cursor) }!! == 1.toByte()
        override suspend fun byte(): Byte = read(1) { get(cursor) }!!
        override suspend fun char(): Char = read(1) { getChar(cursor) }!!
        override suspend fun short(): Short = read(2) { getShort(cursor) }!!
        override suspend fun int(): Int = read(4) { getInt(cursor) }!!
        override suspend fun float(): Float = read(4) { getFloat(cursor) }!!
        override suspend fun double(): Double = read(8) { getDouble(cursor) }!!
        override suspend fun long(): Long = read(8) { getLong(cursor) }!!
        override suspend fun string(): String {
            val size = int()
            val bytes = bytes(size)
            return String(bytes, UTF_8)
        }
    }
    val write = object : Write {
        override suspend fun bytes(bytes: ByteArray) { write { put(bytes, 0, bytes.size) } }
        override suspend fun bool(bool: Boolean) { write { put(if (bool) 1.toByte() else 0.toByte()) } }
        override suspend fun byte(byte: Byte) { write { put(byte) } }
        override suspend fun char(char: Char) { write { putChar(char) } }
        override suspend fun short(short: Short) { write { putShort(short) } }
        override suspend fun int(int: Int) { write { putInt(int) } }
        override suspend fun float(float: Float) { write { putFloat(float) } }
        override suspend fun double(double: Double) { write { putDouble(double) } }
        override suspend fun long(long: Long) { write { putLong(long) } }
        override suspend fun string(string: String) {
            val bytes = string.toByteArray(UTF_8)
            int(bytes.size)
            bytes(bytes)
        }
        suspend fun write(block: ByteBuffer.() -> (ByteBuffer)) {
            if (!isOpen.get()) return
            writeBuffer.apply {
                try {
                    channel.write(block().flip())
                    position(0)
                    limit(capacity())
                } catch (err: Throwable) {
                    if (isOpen.get()) closeEvents()
                    isOpen.set(false)
                    try { channel.close() }
                    catch (_: Throwable) { }
                }
            }
        }
    }
    return object : Connection, Write by write, Read by read {
        override val dispatcher = dispatcher
        override val open: Boolean get() = isOpen.get()
        override val read = read
        override val write = write
        override suspend fun close() {

            if (isOpen.get()) closeEvents()
            isOpen.set(false)
            try { channel.close() }
            catch (_: Throwable) { }
        }
        override suspend fun onClose(block: suspend () -> Unit) {
            closeEvents += block
        }
    }
}

suspend fun accept(port: Int, block: suspend Connection.() -> (Unit)) {
    val dispatcher = newFixedThreadPoolContext(getRuntime().availableProcessors() * 16, "Server")
    val serverChannel = ServerSocketChannel.open().bind(InetSocketAddress(port))
    serverChannel.configureBlocking(false)
    while (true) {
        val channel = serverChannel.accept() ?: continue
        channel.configureBlocking(false)
        GlobalScope.launch(dispatcher) {
            val connection = Connection(dispatcher, channel)
            try { connection.block() }
            catch (_: Throwable) { println("Caught high level error") }
        }
    }
}

suspend fun connect(addr: String, port: Int, block: suspend Connection.() -> (Unit)): Connection? {
    val address = InetSocketAddress(addr, port)
    val channel = SocketChannel.open()
    if (!channel.connect(address)) return null
    channel.configureBlocking(false)
    val dispatcher = newFixedThreadPoolContext(getRuntime().availableProcessors() * 8, "Client")
    val connection = Connection(dispatcher, channel)
    GlobalScope.launch(dispatcher) {
        try { connection.block() }
        catch(e: Throwable) { e.printStackTrace(); println("Caught high level error") }
    }
    return connection
}