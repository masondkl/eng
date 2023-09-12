package me.mason.server

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mason.shared.*
import me.mason.sockets.Connection
import me.mason.sockets.accept
import org.joml.Vector2f
import java.util.*

const val FFA = 0
const val TDM = 1
const val SD = 2

interface ServerMatchState : MatchState {
    var querying: Boolean
    val voters: BitSet
    val respondants: BitSet
    val responses: Array<Int>
    val connections: Array<Connection?>
    var map: Int
    suspend fun broadcast(block: suspend Connection.(Int) -> (Unit))
}

suspend fun main() {
    object : ServerMatchState {
        override var mode = FFA
        override var querying = false
        override val players = BitSet(256)
        override val voters = BitSet()
        override val respondants = BitSet()
        override val responses = Array(256) { -1 }
        override val connections = Array<Connection?>(256) { null }
        override var map = Maps.random()
        override val playerStates = Array<PlayerState>(256) {
            object : PlayerState {
                val lock = Mutex()
                val lerpPos = Vector2f()
                override var id = -1
                override var timePos = 0L
                override val lastPos = Vector2f()
                override val nextPos = Vector2f()
                override val pos: Vector2f get() {
                    val t = ((timeMillis - timePos) / LERP_POS_RATE)
                    return lerpPos.set(
                        lastPos.x + (nextPos.x - lastPos.x) * t,
                        lastPos.y + (nextPos.y - lastPos.y) * t,
                    )
                }
                override var health = 1f
                override var alive = true
                override var planting = false
                override var defusing = false
                override var terrorist = false
                override suspend fun send(block: suspend Connection.() -> Unit) {
                    connections[id]?.apply { lock.withLock { block() } }
                }
            }
        }
        override suspend fun broadcast(block: suspend Connection.(Int) -> Unit) = players.forEach {
            playerStates[it].send { block(it) }
        }
    }.apply { accept(9999) { connect() } }
}