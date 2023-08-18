package me.mason.server

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mason.shared.*
import me.mason.sockets.Connection
import me.mason.sockets.accept
import org.joml.Vector2f
import org.joml.Vector2i
import java.nio.ByteBuffer.wrap
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.readBytes

const val FFA = 0
const val TDM = 1
const val SD = 2

const val IN_JOIN = 0
const val IN_SHOOT = 1
const val IN_POS = 2

const val OUT_JOIN = 0
const val OUT_EXIT = 1
const val OUT_SHOOT = 2
const val OUT_DIE = 3
const val OUT_RESPAWN = 4

const val LERP_POS_RATE = 50L

interface ServerMatchState : MatchState {
    val connections: Array<Connection?>
    var map: Int
}

suspend fun main() {
    object : ServerMatchState {
        override val mode = FFA
        override val players = BitSet(256)
        override val connections = Array<Connection?>(256) { null }
        override var map = Maps.random()
        override val states = Array<PlayerState>(256) {
            object : PlayerState {
                val lock = Mutex()
                val lerpPos = Vector2f()
                override var id = -1
                override var timePos = 0L
                override val lastPos = Vector2f()
                override val nextPos = Vector2f()
                override fun lerpPos(rate: Long): Vector2f {
                    val t = ((timeMillis - timePos) / rate)
                    return lerpPos.set(
                        lastPos.x + (nextPos.x - lastPos.x) * t,
                        lastPos.y + (nextPos.y - lastPos.y) * t,
                    )
                }
                override var health = 0f
                override var alive = false
                override var planting = false
                override var defusing = false
                override var terrorist = false
                override suspend fun send(block: suspend Connection.(Int) -> Unit) {
                    if (id == -1) return
                    connections[id]?.apply { lock.withLock { block(id) } }
                }
            }
        }
        override suspend fun broadcast(block: suspend Connection.(Int) -> Unit) = players.forEach {
            states[it].send(block)
        }
    }.apply { accept(9999) {
        if (int() != IN_JOIN) { close(); return@accept }
        val nextId = players.nextClearBit(0)
        states[nextId].apply {
            id = nextId
            connections[id] = this@accept
            onClose {
                players.clear(id)
                connections[id] = null
                exit()
                clear()
            }; join()
            players.set(id)
            while(true) when(int()) {
                IN_POS -> {
                    timePos = timeMillis
                    lastPos.set(nextPos)
                    nextPos.set(vec2f())
                }
                IN_SHOOT -> {
                    println("Player ${id} shot")
                    shoot(vec2f())
                }
            }
        }
    } }
}