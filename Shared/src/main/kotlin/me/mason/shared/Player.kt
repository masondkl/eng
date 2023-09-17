package me.mason.shared

import org.joml.Vector2f
import java.lang.Math.toRadians

interface Player {
    var id: Int
    var timePos: Long
    val lastPos: Vector2f
    val nextPos: Vector2f
    val pos: Vector2f
    var lastDir: Float
    var nextDir: Float
    val dir: Float
    var health: Float
    var alive: Boolean
    var planting: Boolean
    var defusing: Boolean
    var t: Boolean
}

fun Player() = object : Player {
    override var id = -1
    override var timePos = 0L
    override val lastPos = Vector2f()
    override val nextPos = Vector2f()
    override val pos: Vector2f get() {
        val difference = Vector2f(nextPos).sub(lastPos)
        val t = 50L.coerceAtMost(timeMillis - timePos) / 50f
        return Vector2f(lastPos).add(difference.mul(t))
    }
    override var lastDir = 0f
    override var nextDir = 0f
    override val dir: Float get() {
        val difference = (nextDir - lastDir).let {
            if (it > toRadians(180.0)) it - toRadians(360.0).toFloat()
            else if (it < -toRadians(180.0)) toRadians(360.0).toFloat() + it
            else it
        }
        val t = 50L.coerceAtMost(timeMillis - timePos) / 50f
        return lastDir + difference * t
    }
    override var health = 1f
    override var alive = true
    override var planting = false
    override var defusing = false
    override var t = false
}

var Player.ct: Boolean
    set(value) { t = !value }
    get() = !t
