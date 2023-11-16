package me.mason.shared

import kotlinx.coroutines.runBlocking
import org.joml.Vector2f
import org.joml.Vector2i
import java.util.*
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.ArrayList
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds

val BOMB_DEFUSE_RADIUS = 9.5f / 2f

const val LERP_POS_RATE = 50L

val TILE_DIM = Vector2f(1.0f, 1.0f)
val TILE_RAD = Vector2f(0.5f, 0.5f)
val TILE_UV_DIM = Vector2i(4, 4)
val TILES = Array(11) {
    val x = it * 4
    val y = 123
    Vector2i(x, y)
}
val SOLIDS = arrayOf(0.toByte(), 1.toByte())

const val RENDER_DIM = 96
const val RENDER_RAD = RENDER_DIM / 2

val timeMillis: Long get() = System.currentTimeMillis()
val timeNanos: Long get() = System.nanoTime()

data class Bounds(val min: Vector2f = Vector2f(), val max: Vector2f = Vector2f())
data class TileMap(val name: String, val worldSize: Int, val walls: List<Bounds>, val plantBounds: List<Bounds>, val world: ByteArray, val colliders: Array<Collider>, val corners: Array<Vector2f>)

fun emptyTileMap(): TileMap {
    val worldSize = 64
    return TileMap(
        "Empty", worldSize, emptyList(), emptyList(),
        ByteArray(worldSize * worldSize) { 6 },
        Array(worldSize * worldSize) { Collider(Vector2f(), Vector2f()) },
        emptyArray()
    )
}

fun Vector2f.normal(): Vector2f {
    val mag = sqrt(x * x + y * y)
    if (mag == 0f) return this
    x /= mag
    y /= mag
    return this
}

//inline fun BitSet.forEach(block: (Int) -> (Unit)) {
//    var idx = nextSetBit(0)
//    while(idx != -1) {
//        block(idx)
//        idx = nextSetBit(idx + 1)
//    }
//}
//inline fun BitSet.forEachIndexed(block: (Int, Int) -> (Unit)) {
//    var loopIndex = 0
//    var idx = nextSetBit(0)
//    while(idx != -1) {
//        block(loopIndex++, idx)
//        idx = nextSetBit(idx + 1)
//    }
//}