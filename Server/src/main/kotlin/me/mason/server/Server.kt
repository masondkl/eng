package me.mason.server

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import me.mason.shared.*
import me.mason.sockets.Connection
import me.mason.sockets.accept
import org.joml.Vector2i
import java.nio.ByteBuffer.wrap
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.readBytes

const val IN_JOIN = 0.toByte()
const val IN_PING = 1.toByte()
const val IN_POS = 2.toByte()

const val OUT_JOIN = 0.toByte()
const val OUT_EXIT = 1.toByte()
const val OUT_MAP = 2.toByte()
const val OUT_POS = 3.toByte()

val MAP_DIRECTORY: Path = Paths.get("maps")

suspend fun main() {
    val players = BitSet()
    val pings = Array(256) { 0L }
    val mapNames = MAP_DIRECTORY.toFile().listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
    val redSpawns = Array(mapNames.size) { ArrayList<Vector2i>() }
    val blueSpawns = Array(mapNames.size) { ArrayList<Vector2i>() }
    val spawns = Array(mapNames.size) { ArrayList<Vector2i>() }
    val maps = Array(mapNames.size) { map ->
        val buffer = wrap(MAP_DIRECTORY.resolve("${mapNames[map]}.map").readBytes())
        val worldSize = buffer.int
        val world = ByteArray(worldSize * worldSize)
        buffer.get(world)
        for (i in 0 until buffer.int) {
            val tile = buffer.int
            val type = buffer.get()
            val spawn = Vector2i(tile % worldSize, tile / worldSize)
            spawns[map].add(spawn)
            if (type == RED_SPAWN) redSpawns[map].add(spawn)
            else blueSpawns[map].add(spawn)
        }
        TileMap(mapNames[map], worldSize, world)
    }
    val map = mapNames.indices.random()
    val connections = Array<Connection?>(256) { null }
    val whenClosed: suspend (Int) -> (Unit) = {
        players.clear(it)
        connections[it] = null
        players.forEach { otherId ->
            connections[otherId]?.also { other ->
                other.writeLock.withLock {
                    other.byte(OUT_EXIT)
                    other.int(it)
                }
            }
        }
    }
    accept(9999) { coroutineScope { launch {
        var id = -1
        onClose {
            println("Player $id disconnected")
            pings[id] = 0L
            whenClosed(id)
        }
        try { while (open) when(byte()) {
            IN_JOIN -> {
                id = players.nextClearBit(0)
                writeLock.withLock {
                    int(id)
                    println("Player $id connected")
                    players.forEach { otherId ->
                        connections[otherId]?.also { other ->
                            other.writeLock.withLock {
                                other.byte(OUT_JOIN)
                                other.int(id)
                            }
                        }
                        byte(OUT_JOIN)
                        int(otherId)
                    }
                    byte(OUT_MAP)
                    map(maps[map])
                }
                connections[id] = this@accept
                players.set(id)
            }
            IN_PING -> {
                val now = timeMillis
                if (pings[id] != 0L && pings[id].let { now - it } > 2000L) {
                    println("ping timeout")
                    close(); return@launch
                }
                pings[id] = now
            }
            IN_POS -> {
                val pos = vec2f()
                players.forEach { otherId ->
                    connections[otherId]?.also { other ->
                        other.writeLock.withLock {
                            other.byte(OUT_POS)
                            other.int(id)
                            other.vec2f(pos)
                        }
                    }
                }
            }
        } } catch (err: Throwable) { close() }
    } } }
}