package me.mason.shared

import org.joml.Vector2f
import kotlin.math.abs


data class Collider(val pos: Vector2f, val dim: Vector2f)

fun contains(pos: Vector2f, otherPos: Vector2f, otherDim: Vector2f): Boolean {
    val otherRad = Vector2f(otherDim.x / 2f, otherDim.y / 2f)
    val min = Vector2f(otherPos).sub(otherRad)
    val max = Vector2f(otherPos).add(otherRad)
    return pos.x >= min.x && pos.y >= min.y && pos.x <= max.x && pos.y <= max.y
}

fun Collider.collides(
    other: Collider,
    change: Vector2f = Vector2f(0f, 0f)
): Boolean {
    if (other.dim.x == 0f || other.dim.y == 0f || dim.x == 0f || dim.y == 0f) return false
    return pos.y + change.y - dim.y / 2 < other.pos.y + other.dim.y / 2 &&
            pos.y + change.y + dim.y / 2 > other.pos.y - other.dim.y / 2 &&
            pos.x + change.x - dim.x / 2 < other.pos.x + other.dim.x / 2 &&
            pos.x + change.x + dim.x / 2 > other.pos.x - other.dim.x / 2
}

fun Collider.move(motion: Vector2f, collisions: List<Collider>): Vector2f {
//    println(motion)
    val change = Vector2f(motion.x, motion.y)
    val intersectsHor = collisions.find { collides(it, change = Vector2f(change.x, 0f)) }
    val intersectsDia = collisions.find { collides(it, change = Vector2f(change.x, change.y)) }
    val intersectVer = collisions.find { collides(it, change = Vector2f(0f, change.y)) }
    if (intersectVer != null) {
        val max = intersectVer.pos.y + intersectVer.dim.y / 2
        val min = pos.y + change.y - dim.y / 2
        if (max - min < abs(change.y)) change.y += (max - min) * 1.05f
        else change.y = 0f
    }
    if (intersectsHor != null || intersectVer == null && intersectsDia != null) {
        val with = intersectsHor ?: intersectsDia!!
        val withMax = with.pos.x + with.dim.x / 2
        val withMin = with.pos.x - with.dim.x / 2
        val max = pos.x + change.x + dim.x / 2
        val min = pos.x + change.x - dim.x / 2
        if (withMax - min > change.x && withMax - min < with.dim.x) {
            val offset = withMax - min
            if (offset <= with.dim.x / 2 && offset >= -with.dim.x / 2)
                change.x += offset
        } else if (max - withMin < change.x && max - withMin < with.dim.x) {
            val offset = max - withMin
            if (offset <= with.dim.x / 2 && offset >= -with.dim.x / 2)
                change.x -= offset
        } else change.x = 0f
    }
    return change
}