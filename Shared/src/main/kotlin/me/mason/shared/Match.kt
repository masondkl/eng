package me.mason.shared

import me.mason.sockets.Connection
import org.joml.Vector2f
import java.util.*

interface PlayerState {
    var id: Int
    var timePos: Long
    val lastPos: Vector2f
    val nextPos: Vector2f
    fun lerpPos(rate: Long): Vector2f
    var health: Float
    var alive: Boolean
    var planting: Boolean
    var defusing: Boolean
    var terrorist: Boolean
    suspend fun send(block: suspend Connection.(Int) -> (Unit))
}

fun PlayerState.clear() {
    id = -1
    timePos = 0L
    lastPos.set(0f)
    nextPos.set(0f)
    health = 0f
    alive = false
    planting = false
    defusing = false
    terrorist = false
}

interface MatchState {
    val mode: Int
    val players: BitSet
    val states: Array<PlayerState>
    suspend fun broadcast(block: suspend Connection.(Int) -> (Unit))
}

