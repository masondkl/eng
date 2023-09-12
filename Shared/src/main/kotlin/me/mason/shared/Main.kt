package me.mason.shared

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mason.sockets.Read
import me.mason.sockets.Write
import org.joml.Vector2f
import org.joml.Vector2i
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

val BOMB_DEFUSE_RADIUS = 9.5f / 2f

const val LERP_POS_RATE = 50L

val TILE_SIZE = Vector2f(1.0f, 1.0f)
val TILE_RADIUS = Vector2f(0.5f, 0.5f)
val TILE_UV_SIZE = Vector2i(4, 4)
val TILES = Array(11) {
    val x = it * 4
    val y = 508
    Vector2i(x, y)
}
val SOLIDS = arrayOf(0.toByte(), 1.toByte())

val VOTE_MAP = 0
val VOTE_KICK = 1

class Pool<T>(private val clear: (T) -> (T), size: Int = 1024, initial: () -> (T)) {
    companion object {
        val vec2f = Pool({ it.set(0f, 0f) }) { Vector2f() }
        val vec2i = Pool({ it.set(0, 0) }) { Vector2i() }
    }
    private val lock = Mutex()
    private val pool: Queue<T> = LinkedList<T>()
        .apply { (0 until size).forEach { _ -> add(initial()) } }
    private val deferred: Queue<T> = LinkedList()
    suspend fun <R> take(block: suspend Pool<T>.(suspend () -> (T)) -> (R)): R {
        val result = block { lock.withLock { pool.poll().also { deferred.add(it) } } }
        while(deferred.isNotEmpty()) lock.withLock { pool.add(clear(deferred.poll())) }
        return result
    }
}


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

data class TiledRay(
    val hit: Vector2f = Vector2f(),
    val tile: Vector2i = Vector2i(),
    var distance: Float = 0f
)

//interface RayResult<T> {
//    val result: Boolean
//    var data: T?
//    val distance: Float
//    val hit: Vector2f
//    val hitTile: Vector2i
//    val side: Int
//}

fun ByteArray.tiledRaycast(
    worldSize: Int,
    start: Vector2f,
    dir: Vector2f,
    result: TiledRay,
    max: Float = RENDER_RADIUS.toFloat()
): TiledRay {
    val currentTileCoord = Vector2i(start.x.roundToInt(), start.y.roundToInt())
    val bottomLeft = Vector2f(currentTileCoord.x - 0.5f, currentTileCoord.y - 0.5f)
    val rayMaxStepSize = Vector2f(abs(1 / dir.x), abs(1 / dir.y))
    val rayStepLength = Vector2f(0f, 0f)
    val mapStep = Vector2i(0, 0)
    val tempDir = Vector2f()

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

    while (fDistance < max) {
        currentCoord = start.copy()
        if (rayStepLength.x < rayStepLength.y) {
            currentCoord.add(tempDir.set(dir).mul(rayStepLength.x))
            currentTileCoord.x += mapStep.x
            fDistance = rayStepLength.x
            rayStepLength.x += rayMaxStepSize.x
        } else {
            currentCoord.add(tempDir.set(dir).mul(rayStepLength.y))
            currentTileCoord.y += mapStep.y
            fDistance = rayStepLength.y
            rayStepLength.y += rayMaxStepSize.y
        }
        val index = (currentTileCoord.y * worldSize) + currentTileCoord.x
        if(currentTileCoord.x >= worldSize || currentTileCoord.x < 0 ||
            currentTileCoord.y >= worldSize || currentTileCoord.y < 0 ||
            this[index] in SOLIDS
        ) break
    }
    //TODO:idk
//    quad(shader, idx, if (hitSomething) currentCoord else start + dir * max, TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC)
    return result.apply {
        distance = fDistance
        hit.set(currentCoord)
        tile.set(currentTileCoord)
    }
}

interface Ray<T> {
    val result: Boolean
    val hit: Vector2f
    val distance: Float
    var collision: T?
}

suspend fun <T> raycast(
    start: Vector2f,
    dir: Vector2f,
    max: Float = RENDER_RADIUS.toFloat(),
    test: suspend Ray<T>.(Vector2f) -> (Boolean)
): Ray<T> {
    val position = start.copy()
    val ray = object : Ray<T> {
        override var result = false
        override var distance = 0f
        override var collision: T? = null
        override val hit = position
    }
    ray.result = ray.test(position)
    if (ray.result) return ray
    var distance = sqrt((position.x - start.x).pow(2) + (position.y - start.y).pow(2))
    while (distance < max) {
        position.add(dir)
        distance = sqrt((position.x - start.x).pow(2) + (position.y - start.y).pow(2))
        ray.result = ray.test(position)
        ray.distance = distance
        if (ray.result) return ray
    }
    ray.collision = null
    return ray
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

data class Bounds(val min: Vector2f = Vector2f(), val max: Vector2f = Vector2f())
data class TileMap(val name: String, val worldSize: Int, val walls: List<Bounds>, val world: ByteArray, val colliders: Array<Collider>, val corners: Array<Vector2f>)

//tileColliders = Array(map.worldSize * map.worldSize) {
//    if (map.world[it] in SOLIDS) {
//        val x = it % map.worldSize
//        val y = it / map.worldSize
//        Collider(Vector2f(x.toFloat(), y.toFloat()), TILE_SIZE)
//    } else Collider(Vector2f(0f, 0f), Vector2f(0f, 0f))
//}
//corners = ArrayList<Vector2f>().apply {
//    map.walls.indices.forEach {
//        val wall = map.walls[it]
//        val min = wall.min
//        val max = wall.max
//        add(Vector2f(min.x, min.y))
//        add(Vector2f(max.x, min.y))
//        add(Vector2f(min.x, max.y))
//        add(Vector2f(max.x, max.y))
//    }
//}

suspend fun Write.map(map: TileMap) {
    string(map.name)
    int(map.worldSize)
    int(map.walls.size)
    for (wall in map.walls) {
        vec2f(wall.min)
        vec2f(wall.max)
    }
    bytes(map.world)
}

suspend fun Read.map(): TileMap {
    val name = string()
    val worldSize = int()
    val wallCount = int()
    val walls = ArrayList<Bounds>()
    (0 until wallCount).forEach { _ -> walls.add(Bounds(vec2f(), vec2f())) }
    val bytes = bytes(worldSize * worldSize)
    val colliders = Array(worldSize * worldSize) {
        if (bytes[it] in SOLIDS) {
            val x = it % worldSize
            val y = it / worldSize
            Collider(Vector2f(x.toFloat(), y.toFloat()), TILE_SIZE)
        } else Collider(Vector2f(0f, 0f), Vector2f(0f, 0f))
    }
    val corners = Array(walls.size * 4) { Vector2f() }
    walls.indices.forEach {
        val wall = walls[it]
        val min = wall.min
        val max = wall.max
        corners[it * 4] = Vector2f(min.x, min.y)
        corners[it * 4 + 1] = Vector2f(max.x, min.y)
        corners[it * 4 + 2] = Vector2f(min.x, max.y)
        corners[it * 4 + 3] = Vector2f(max.x, max.y)
    }
    return TileMap(name, worldSize, walls, bytes, colliders, corners)
}

fun contains(pos: Vector2f, otherPos: Vector2f, otherDim: Vector2f): Boolean {
    val otherRad = Vector2f(otherDim.x / 2f, otherDim.y / 2f)
    val min = Vector2f(otherPos).sub(otherRad)
    val max = Vector2f(otherPos).add(otherRad)
    return pos.x >= min.x && pos.y >= min.y && pos.x <= max.x && pos.y <= max.y
}

fun main() {
    println("gg")
}