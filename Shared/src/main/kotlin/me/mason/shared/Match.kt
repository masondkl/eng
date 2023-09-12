package me.mason.shared

import me.mason.sockets.Connection
import org.joml.Vector2f
import java.util.*

interface PlayerState {
    var id: Int
    var timePos: Long
    val lastPos: Vector2f
    val nextPos: Vector2f
    val pos: Vector2f
    var health: Float
    var alive: Boolean
    var planting: Boolean
    var defusing: Boolean
    var terrorist: Boolean
    suspend fun send(block: suspend Connection.() -> (Unit))
}

fun PlayerState.clear() {
    id = -1
    timePos = 0L
    lastPos.set(0f)
    nextPos.set(0f)
    health = 1f
    alive = true
    planting = false
    defusing = false
    terrorist = false
}

interface MatchState {
    var mode: Int
    val players: BitSet
    val playerStates: Array<PlayerState>
}

