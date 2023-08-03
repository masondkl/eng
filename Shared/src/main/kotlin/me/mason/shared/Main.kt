package me.mason.shared

import me.mason.sockets.Read
import me.mason.sockets.Write
import org.joml.Vector2f
import org.joml.Vector2i
import java.nio.ByteBuffer
import java.util.ArrayList
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

val TILE_SIZE_VEC = Vector2f(1.0f, 1.0f)
val TILE_UV_SIZE = 4
val TILE_UV_SIZE_VEC = Vector2i(TILE_UV_SIZE, TILE_UV_SIZE)
val TILES = Array(11) {
    val x = (it * 4) % 512
    val y = ((it * 4) / 512) * 4
    Vector2i(x, y)
}
val SOLIDS = arrayOf(0.toByte(), 1.toByte())

const val RENDER_SIZE = 82
const val RENDER_RADIUS = RENDER_SIZE / 2

data class Collider(val pos: Vector2f, val dim: Vector2f)

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

interface RayResult<T> {
    val result: Boolean
    var data: T?
    val distance: Float
    val hit: Vector2f
    val hitTile: Vector2i
    val side: Int
}

fun <T> RayResult<T>.copy() = object : RayResult<T> {
    override val result = this@copy.result
    override var data = this@copy.data
    override val distance = this@copy.distance
    override val hit = this@copy.hit.copy()
    override val hitTile = this@copy.hitTile.copy()
    override val side = this@copy.side
}

fun ByteArray.tiledRaycast(
    worldSize: Int,
    start: Vector2f,
    dir: Vector2f,
    max: Float = RENDER_RADIUS.toFloat()
): RayResult<Unit> {
    val currentTileCoord = Vector2i(start.x.roundToInt(), start.y.roundToInt())
    val bottomLeft = Vector2f(currentTileCoord.x - 0.5f, currentTileCoord.y - 0.5f)
    val rayMaxStepSize = Vector2f(abs(1 / dir.x), abs(1 / dir.y))
    val rayStepLength = Vector2f(0f, 0f)
    val mapStep = Vector2i(0, 0)

    if (dir.x < 0) {
        mapStep.x = -1
        rayStepLength.x = (start.x - bottomLeft.x) * rayMaxStepSize.x
    } else {
        mapStep.x = 1
        rayStepLength.x = (bottomLeft.x + 1 - start.x) * rayMaxStepSize.x
    }

    if (dir.y < 0) {
        mapStep.y = -1
        rayStepLength.y = (start.y - bottomLeft.y) * rayMaxStepSize.y
    } else {
        mapStep.y = 1
        rayStepLength.y = (bottomLeft.y + 1 - start.y) * rayMaxStepSize.y
    }

    var fDistance = 0f
    var currentCoord = Vector2f(0.0f, 0.0f)
    var hitSomething = false

    while (fDistance < max) {
        currentCoord = start.copy()
        if (rayStepLength.x < rayStepLength.y) {
            currentCoord = currentCoord + dir * rayStepLength.x
            currentTileCoord.x += mapStep.x
            fDistance = rayStepLength.x
            rayStepLength.x += rayMaxStepSize.x
        } else {
            currentCoord = currentCoord + dir * rayStepLength.y
            currentTileCoord.y += mapStep.y
            fDistance = rayStepLength.y
            rayStepLength.y += rayMaxStepSize.y
        }
        val index = (currentTileCoord.y * worldSize) + currentTileCoord.x
        if(currentTileCoord.x >= worldSize || currentTileCoord.x < 0 ||
            currentTileCoord.y >= worldSize || currentTileCoord.y < 0 ||
            this[index] in SOLIDS
        ) {
            hitSomething = true
            break
        }
    }
    hitSomething = true
    //TODO:idk
//    quad(shader, idx, if (hitSomething) currentCoord else start + dir * max, TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC)
    return object : RayResult<Unit> {
        override val result = hitSomething
        override var data: Unit? = Unit
        override val distance = fDistance
        override val hit = if (hitSomething) currentCoord else start + dir * max
        override val hitTile = currentTileCoord
        override val side =
            if (hit.x == currentTileCoord.x - 0.5f) 0
            else if (hit.x == currentTileCoord.x + 0.5f) 2
            else if (hit.y == currentTileCoord.y - 0.5f) 3
            else if (hit.y == currentTileCoord.y + 0.5f) 1
            else -1
    }
}

fun <T> raycast(
    start: Vector2f,
    dir: Vector2f,
    max: Float = RENDER_RADIUS.toFloat(),
    test: RayResult<T>.(Vector2f) -> (Boolean)
): RayResult<T> {
    val position = start.copy()
    val rayResult = object : RayResult<T> {
        override var result = false
        override var data: T? = null
        override var distance = 0f
        override val hit = position
        override val hitTile = position.round(Vector2f()).int()
        override val side = -1
    }
    rayResult.result = rayResult.test(position)
    if (rayResult.result) return rayResult
    var distance = sqrt((position.x - start.x).pow(2) + (position.y - start.y).pow(2))
    while (distance < max) {
        position += dir
        distance = sqrt((position.x - start.x).pow(2) + (position.y - start.y).pow(2))
        rayResult.result = rayResult.test(position)
        rayResult.distance = distance
        if (rayResult.result) return rayResult
    }
    rayResult.distance = 0f
    rayResult.data = null
    return rayResult
}

//
//fun ByteBuffer.map(name: String): TileMap {
//    val worldSize = int
//    val world = ByteArray(worldSize * worldSize); get(world)
//    val redSpawns = ArrayList<FloatVector>()
//    val blueSpawns = ArrayList<FloatVector>()
//    for (i in 0 until int) {
//        val tile = int
//        val type = get()
//        val vec = vec(tile % worldSize, tile / worldSize).float()
//        if (type == RED_SPAWN) redSpawns.add(vec)
//        else blueSpawns.add(vec)
//    }
//    return object : TileMap {
//        override val name = name
//        override val worldSize = worldSize
//        override val world = world
//    }
//}

suspend fun Write.vec2f(vec: Vector2f) { float(vec.x); float(vec.y) }
suspend fun Write.vec2i(vec: Vector2i) { int(vec.x); int(vec.y) }
suspend fun Read.vec2f() = Vector2f(float(), float())
suspend fun Read.vec2i() = Vector2i(int(), int())

val RED_SPAWN = 0.toByte()
val BLUE_SPAWN = 1.toByte()

data class TileMap(val name: String, val worldSize: Int, val world: ByteArray)

suspend fun Write.map(map: TileMap) {
    string(map.name)
    int(map.worldSize)
    bytes(map.world)
}

suspend fun Read.map(): TileMap {
    val name = string()
    val worldSize = int()
    val bytes = bytes(worldSize * worldSize)
    return TileMap(name, worldSize, bytes)
}

fun main() {
    println("gg")
}