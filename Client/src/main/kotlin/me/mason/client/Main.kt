package me.mason.client

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mason.sockets.Read
import me.mason.sockets.Write
import me.mason.sockets.connect
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL20.*
import java.lang.Math.toRadians
import java.nio.ByteBuffer
import java.nio.ByteBuffer.wrap
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.math.*
import kotlin.time.TimeMark
import kotlin.time.TimeSource.Monotonic.markNow

const val WORLD_SIZE = 64
const val WORLD_RADIUS = WORLD_SIZE / 2
const val RENDER_SIZE = 82
const val RENDER_RADIUS = RENDER_SIZE / 2
const val COLLISION_SIZE = 4
const val COLLISION_RADIUS = COLLISION_SIZE / 2

val TILE_SIZE_VEC = vec(1.0f, 1.0f)
val TILE_UV_SIZE = 4
val TILE_UV_SIZE_VEC = vec(TILE_UV_SIZE, TILE_UV_SIZE)
val TILES = Array(11) {
    val x = (it * 4) % 512
    val y = ((it * 4) / 512) * 4
    vec(x, y)
}
val SOLIDS = arrayOf(0, 1)

val PLAYER = vec(0, 4)
val DEAD_PLAYER = vec(4, 4)
val PLAYER_UV_SIZE_VEC = vec(4, 4)
val SELECTED = vec(11, 4)

val BULLET = vec(9, 5)
val BULLET_SIZE_VEC = vec(0.5f, 0.5f)
val BULLET_UV_SIZE_VEC = vec(1, 1)

//suspend fun main(args: Array<String>) {
//    if (args.isNotEmpty()) myntServer()
//    else client()
//}

val CLI_OUT_MOVE = 0.toByte()
val CLI_OUT_REQUEST_ID = 1.toByte()
val CLI_OUT_SHOOT = 2.toByte()
val CLI_OUT_KILL = 3.toByte()

val CLI_IN_DISCONNECT = 32.toByte()
val CLI_IN_MOVE = 33.toByte()
val CLI_IN_ID = 34.toByte()
val CLI_IN_SHOOT = 35.toByte()
val CLI_IN_DIE = 36.toByte()
val CLI_IN_RESET = 37.toByte()

fun Write.vec2f(vec: FloatVector) { float(vec.x); float(vec.y) }
fun Write.vec2i(vec: IntVector) { int(vec.x); int(vec.y) }
fun Read.vec2f() = vec(float(), float())
fun Read.vec2i() = vec(int(), int())

data class Collider(val pos: FloatVector, val dim: FloatVector)

fun Collider.collides(
    other: Collider,
    change: FloatVector = vec(0f, 0f)
): Boolean {
    if (other.dim.x == 0f || other.dim.y == 0f || dim.x == 0f || dim.y == 0f) return false
    return pos.y + change.y - dim.y / 2 < other.pos.y + other.dim.y / 2 &&
            pos.y + change.y + dim.y / 2 > other.pos.y - other.dim.y / 2 &&
            pos.x + change.x - dim.x / 2 < other.pos.x + other.dim.x / 2 &&
            pos.x + change.x + dim.x / 2 > other.pos.x - other.dim.x / 2
}

fun Collider.move(motion: FloatVector, collisions: List<Collider>): FloatVector {
//    println(motion)
    val change = vec(motion.x, motion.y)
    val intersectsHor = collisions.find { collides(it, change = vec(change.x, 0f)) }
    val intersectsDia = collisions.find { collides(it, change = vec(change.x, change.y)) }
    val intersectVer = collisions.find { collides(it, change = vec(0f, change.y)) }
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
    val hit: FloatVector
    val hitTile: IntVector
    val side: Int
}

data class Bullet(val start: FloatVector, val pos: FloatVector, val dir: FloatVector, val tileHit: Float)

fun IntArray.tiledRaycast(
    start: FloatVector,
    dir: FloatVector,
    max: Float = 100f
): RayResult<Unit> {
    val currentTileCoord = vec(start.x.roundToInt(), start.y.roundToInt())
    val bottomLeft = vec(currentTileCoord.x - 0.5f, currentTileCoord.y - 0.5f)
    val rayMaxStepSize = vec(abs(1 / dir.x), abs(1 / dir.y))
    val rayStepLength = vec(0f, 0f)
    val mapStep = vec(0, 0)

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
    var currentCoord = vec(0.0f, 0.0f)
    var hitSomething = false

    while (fDistance < max) {
        currentCoord = start.clone()
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
        val index = (currentTileCoord.y * WORLD_SIZE) + currentTileCoord.x
        if(currentTileCoord.x >= WORLD_SIZE || currentTileCoord.x < 0 ||
            currentTileCoord.y >= WORLD_SIZE || currentTileCoord.y < 0 ||
            this[index] in SOLIDS
        ) {

            hitSomething = true
            break
        }
    }
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
    start: FloatVector,
    dir: FloatVector,
    max: Float = 100f,
    test: RayResult<T>.(FloatVector) -> (Boolean)
): RayResult<T> {
    val position = start.clone()
    val rayResult = object : RayResult<T> {
        override var result = false
        override var data: T? = null
        override var distance = 0f
        override val hit = position
        override val hitTile = position.rounded().int()
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

fun Window.mouseWorld(): FloatVector {
    val mouseX = BufferUtils.createDoubleBuffer(1)
    val mouseY = BufferUtils.createDoubleBuffer(1)
    val width = BufferUtils.createIntBuffer(1)
    val height = BufferUtils.createIntBuffer(1)
    glfwGetCursorPos(window, mouseX, mouseY)
    glfwGetWindowSize(window, width, height)
    return vec(
        (mouseX.get().toFloat() - 640f) / (1280 / 80f) + camera.position.x,
        (height.get().toFloat() - mouseY.get().toFloat() - 360f) / (720f / 45f) + camera.position.y
    )
}

fun editor() = window { input {
    val textureShader = shader("texture.vert", "texture.frag") {
        attr(2); attr(1)
    }
    val world = IntArray(WORLD_SIZE * WORLD_SIZE) { 6 }
    var selected = 0

    glfwSetScrollCallback(window) { _, x, y ->
        if (y > 0) selected = min(selected + 1, TILES.size - 1)
        else selected = max(selected - 1, 0)
    }

    key += press@{ code, action ->
        if (code != GLFW_KEY_S || action != GLFW_PRESS || keys[GLFW_KEY_LEFT_CONTROL]) return@press
        val buffer = ByteBuffer.allocate(WORLD_SIZE * WORLD_SIZE * 4)
        world.forEach { buffer.putInt(it) }
        Paths.get("world.tiles").writeBytes(buffer.array())
    }

//    button += press@{ code, action ->
//        if (code != GLFW_MOUSE_BUTTON_1 || action != GLFW_PRESS) return@press
//        val tile = mouseWorld().round().int()
//        val index = tile.x + tile.y * WORLD_SIZE
//        println(tile)
//        world[index] = selected
//    }

    buffer(512, Paths.get("shooter.png"), camera, {  }) {
        fixed(144) { elapsed ->
            val motion = vec(0f, 0f).apply {
                if (keys[GLFW_KEY_W]) y += 1
                if (keys[GLFW_KEY_S]) y -= 1
                if (keys[GLFW_KEY_A]) x -= 1
                if (keys[GLFW_KEY_D]) x += 1
            }.normalize() * 0.125f
            camera.position += motion
            camera.move()
            using(textureShader, 0) {
                textureQuad(RENDER_SIZE * RENDER_SIZE, camera.position, TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC)
            }
        }
        tick { delta, elapsed ->
            using(textureShader, 0) {
                clearQuad(0 until RENDER_SIZE * RENDER_SIZE)
                val cameraTile = camera.position.rounded().int()
                val topLeft = vec(-38f, 20.5f)
                for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
                    val tile = vec(index % RENDER_SIZE, index / RENDER_SIZE) - RENDER_RADIUS + cameraTile
                    if (tile.x < 0 || tile.x >= WORLD_SIZE || tile.y < 0 || tile.y >= WORLD_SIZE) continue
                    val tileIndex = tile.x + tile.y * WORLD_SIZE
                    textureQuad(index, tile.float(), TILE_SIZE_VEC, TILES[world[tileIndex]], TILE_UV_SIZE_VEC)
                }
                TILES.forEachIndexed { idx, it ->
                    textureQuad(
                        RENDER_SIZE * RENDER_SIZE + 1 + idx,
                        camera.position + vec(TILE_SIZE_VEC.x * idx.toFloat(), 0f) + topLeft, TILE_SIZE_VEC,
                        it, TILE_UV_SIZE_VEC
                    )
                }
                textureQuad(
                    RENDER_SIZE * RENDER_SIZE + 1 + TILES.size,
                    camera.position + vec(TILE_SIZE_VEC.x * selected.toFloat(), 0f) + topLeft, TILE_SIZE_VEC,
                    SELECTED, TILE_UV_SIZE_VEC
                )
            }
            if (buttons[GLFW_MOUSE_BUTTON_1]) {
                val tile = mouseWorld().round().int()
                val index = tile.x + tile.y * WORLD_SIZE
                if (tile.x < 0 || tile.x >= WORLD_SIZE || tile.y < 0 || tile.y >= WORLD_SIZE) return@tick
                world[index] = selected
            }
        }
    }
} }

fun client() = window { input { runBlocking {
    title = "title"
    width = 1280
    height = 720
    val fovTriangles = FloatArray(1536) { 1f }
    val textureShader = shader("texture.vert", "texture.frag") {
        attr(2); attr(1)
    }
    val clipTextureShader = shader("cliptexture.vert", "cliptexture.frag") {
        attr(2); attr(1)
        uniform("triangles") { loc -> glUniform1fv(loc, fovTriangles) }
    }
    val fovShader = shader("fov.vert", "fov.frag") {
        attr(2)
        uniform("triangles") { loc -> glUniform1fv(loc, fovTriangles) }
    }
    val bullets = CopyOnWriteArrayList<Bullet>()
    val buffer = wrap(Paths.get("world.tiles").readBytes())
    val world = IntArray(WORLD_SIZE * WORLD_SIZE) { buffer.getInt() }
    val tileColliders = Array(WORLD_SIZE * WORLD_SIZE) {
        if (world[it] in SOLIDS) {
            val x = it % WORLD_SIZE
            val y = it / WORLD_SIZE
            Collider(vec(x.toFloat(), y.toFloat()), TILE_SIZE_VEC)
        } else Collider(vec(0f, 0f), vec(0f, 0f))
    }

    var id = 0
    val collider = Collider(vec(0f, 0f), TILE_SIZE_VEC)
    val dead = ConcurrentSkipListSet<Int>()
    val fromPositions = ConcurrentHashMap<Int, FloatVector>()
    val toPositions = ConcurrentHashMap<Int, FloatVector>()
    val colliders = ConcurrentHashMap<Int, Collider>()
    val updates = ConcurrentHashMap<Int, TimeMark>()
    fun interpolated(id: Int): FloatVector {
        if (toPositions[id] == null) return vec(1f, 1f)
        val from = fromPositions[id] ?: toPositions[id]!!
        val t = updates[id]!!.elapsedNow().inWholeMilliseconds / 10f
        return from + (toPositions[id]!! - from) * t
    }
    camera.position.set(vec(1f, 1f))

    fun collider(of: Int): Collider =
        if (id != of)
            colliders.getOrPut(of) { Collider(interpolated(of), TILE_SIZE_VEC - 0.1f) }
                .apply { pos.set(interpolated(of)) }
        else collider.apply { pos.set(camera.position) }
    connect("parkourmate.com", 9999) { context ->
        write.byte(CLI_OUT_REQUEST_ID)
        if (read.byte() != CLI_IN_ID) error("Could not receive id from server")
        id = read.int()
        println("Connected as ${id}")
        launch(context) {
            try {
                while (isActive && open) {
                    val op = read.byte()
                    if (op == CLI_IN_MOVE) {
                        val moving = read.int()
                        val pos = read.vec2f()
                        if (!toPositions.containsKey(moving))
                            println("Player $moving connected")
                        updates[moving] = markNow()
                        toPositions[moving]?.let { fromPositions[moving] = it.clone() }
                        toPositions[moving] = pos
                    } else if (op == CLI_IN_SHOOT) {
                        val shooter = read.int()
                        val pos = read.vec2f()
                        val dir = read.vec2f()
                        val blockRay = world.tiledRaycast(pos, dir)
                        bullets.add(Bullet(pos.clone(), pos, dir, blockRay.distance))
                    } else if (op == CLI_IN_DISCONNECT) {
                        val disconnected = read.int()
                        fromPositions.remove(disconnected)
                        toPositions.remove(disconnected)
                        dead.remove(disconnected)
                        colliders.remove(disconnected)
                        println("Player $disconnected disconnected")
                    } else if (op == CLI_IN_DIE) {
                        dead.add(read.int())
                    } else if (op == CLI_IN_RESET) {
                        camera.position.set(vec(1f, 1f))
                        dead.clear()
                        bullets.clear()
                    }
                }
            } catch (_: Throwable) {
            }
        }
        val writeLock = Mutex()
        launch(context) {
            try {
                while (isActive && open && !glfwWindowShouldClose(window)) {
                    writeLock.withLock {
                        write.byte(CLI_OUT_MOVE)
                        write.int(id)
                        write.vec2f(camera.position)
                    }
                    delay(10)
                }; close()
            } catch (_: Throwable) {
            }
        }
        button += press@{ code, action ->
            if (code != GLFW_MOUSE_BUTTON_1 || action != GLFW_PRESS || dead.contains(id)) return@press
            val mouseWorld = mouseWorld()
            val theta = atan2(camera.position.y - mouseWorld.y, camera.position.x - mouseWorld.x) + PI.toFloat()
            val dir = vec(cos(theta), sin(theta))
            val shootRay = raycast<Int>(camera.position, dir * 0.33f) {
                val result = toPositions.keys.firstOrNull { id ->
                    val interpolated = interpolated(id)
                    val min = interpolated - TILE_SIZE_VEC / 2f
                    val max = interpolated + TILE_SIZE_VEC / 2f
                    it.x > min.x && it.x < max.x && it.y > min.y && it.y < max.y
                } ?: return@raycast false
                data = result
                true
            }
            val blockRay = world.tiledRaycast(camera.position, dir)
            bullets.add(Bullet(camera.position.clone(), camera.position.clone(), dir, blockRay.distance))
            runBlocking {
                writeLock.withLock {
                    write.byte(CLI_OUT_SHOOT)
                    write.int(id)
                    write.vec2f(camera.position)
                    write.vec2f(dir)
                    if (shootRay.result && (!blockRay.result || blockRay.distance >= shootRay.distance)) {
                        dead.add(shootRay.data!!)
                        write.byte(CLI_OUT_KILL)
                        write.int(id)
                        write.int(shootRay.data!!)
                    }
                }
            }
        }
    }
    buffer(512, Paths.get("shooter.png"), camera, { }) {
        fixed(144) { elapsed ->
            val cameraTile = camera.position.rounded().int()
            val nearbyTileColliders = (0 until COLLISION_SIZE * COLLISION_SIZE).mapNotNull {
                val tile = vec(it % COLLISION_SIZE, it / COLLISION_SIZE) - COLLISION_RADIUS + cameraTile
                if (tile.x < 0 || tile.x >= WORLD_SIZE || tile.y < 0 || tile.y >= WORLD_SIZE) null
                else {
                    val tileIndex = tile.x + tile.y * WORLD_SIZE
                    tileColliders[tileIndex]
                }
            }
            if (!dead.contains(id)) {
                val motion = vec(0f, 0f).apply {
                    if (keys[GLFW_KEY_W]) y += 1
                    if (keys[GLFW_KEY_S]) y -= 1
                    if (keys[GLFW_KEY_A]) x -= 1
                    if (keys[GLFW_KEY_D]) x += 1
                }.normalize() * 0.075f
                camera.position += collider(id).move(motion, nearbyTileColliders)
                camera.move()
            }
            using(textureShader, 0) {
                textureQuad(
                    RENDER_SIZE * RENDER_SIZE,
                    camera.position,
                    TILE_SIZE_VEC,
                    if (dead.contains(id)) DEAD_PLAYER else PLAYER,
                    PLAYER_UV_SIZE_VEC
                )
                clearQuad((RENDER_SIZE * RENDER_SIZE + 1) until (RENDER_SIZE * RENDER_SIZE + 129))
                bullets.removeIf { bullet ->
                    val shootRay = raycast<Unit>(bullet.pos, bullet.dir * 0.33f, max = 1f) {
                        toPositions.keys.firstOrNull { id ->
                            val interpolated = interpolated(id)
                            val min = interpolated - TILE_SIZE_VEC / 2f
                            val max = interpolated + TILE_SIZE_VEC / 2f
                            it.x > min.x && it.x < max.x && it.y > min.y && it.y < max.y
                        } ?: return@raycast false
                        true
                    }
                    if (shootRay.distance == 0f || shootRay.distance > bullet.tileHit) bullet.start.distance(bullet.pos) > bullet.tileHit
                    else bullet.start.distance(bullet.pos) > shootRay.distance
                }
                for (i in 0 until min(128, bullets.size)) {
                    val bullet = bullets[i]
                    textureQuad(
                        RENDER_SIZE * RENDER_SIZE + 1 + i,
                        bullet.pos,
                        BULLET_SIZE_VEC,
                        BULLET,
                        BULLET_UV_SIZE_VEC
                    )
                    bullet.pos += bullet.dir * 1.25f
                }
                bullets.removeIf { it.pos.distance(camera.position) > 100f }
            }
        }
        tick { delta, elapsed ->
//            println(1/delta)
            using(fovShader, 2) {
                quad(0, camera.position, vec(80f, 45f))
                if (dead.contains(id)) return@using
                val fov = 120
                val fovRad = toRadians(fov.toDouble())
                val mouseWorld = mouseWorld()
                val theta = atan2(camera.position.y - mouseWorld.y, camera.position.x - mouseWorld.x) + PI.toFloat()
                val fovResolutionMultiplier = 0.5
                val startRad = theta + fovRad / 2f
                val list = Array((fov * fovResolutionMultiplier).toInt()) {
                    val dirTheta = (startRad - toRadians(it.toDouble() / fovResolutionMultiplier)).toFloat()
                    val dir = vec(cos(dirTheta), sin(dirTheta))
                    world.tiledRaycast(camera.position, dir)
                }
                for (index in 1 until (fov * fovResolutionMultiplier).toInt()) {
                    fovTriangles[(index - 1) * 6] = list[index - 1].hit.x
                    fovTriangles[(index - 1) * 6 + 1] = list[index - 1].hit.y
                    fovTriangles[(index - 1) * 6 + 2] = list[index].hit.x
                    fovTriangles[(index - 1) * 6 + 3] = list[index].hit.y
                    fovTriangles[(index - 1) * 6 + 4] = camera.position.x
                    fovTriangles[(index - 1) * 6 + 5] = camera.position.y
                }
            }
//            println(1.0/delta)
            using(textureShader, 0) {
                clearQuad(0 until RENDER_SIZE * RENDER_SIZE)
                val cameraTile = camera.position.rounded().int()
                for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
                    val tile = vec(index % RENDER_SIZE, index / RENDER_SIZE) - RENDER_RADIUS + cameraTile
                    if (tile.x < 0 || tile.x >= WORLD_SIZE || tile.y < 0 || tile.y >= WORLD_SIZE) continue
                    val tileIndex = tile.let { it.x + it.y * WORLD_SIZE }
                    textureQuad(index, tile.float(), TILE_SIZE_VEC, TILES[world[tileIndex]], TILE_UV_SIZE_VEC)
                }
            }
            using(clipTextureShader, 1) {
                clearQuad(0 until 128)
                toPositions.keys.forEachIndexed { index, id ->
                    textureQuad(
                        index,
                        interpolated(id), TILE_SIZE_VEC,
                        if (dead.contains(id)) DEAD_PLAYER else PLAYER, PLAYER_UV_SIZE_VEC
                    )
                }
            }
        }
    }
} } }

fun main(args: Array<String>) =
    if (args.isEmpty()) client()
    else if (args.first() == "editor") editor()
    else error("Invalid startup arguments")


//                var previous: RayResult
//                var current: RayResult
//
//                for (index in 0 until fov * fovResolutionMultiplier) {
//                    current = list[index]
//                    if(index > 0) {
//                        previous = list[index - 1]
//
//                        if(current.hitTile != previous.hitTile)
//                        {
//                            val newDistance = current.hitTile.float().distance(camera.position)
//                            val prevDistance = previous.hitTile.float().distance(camera.position)
//
//                            if(abs(newDistance - prevDistance) > 1.3f)
//                            {
//                                if(prevDistance > newDistance)
//                                {
//                                    // if bottom edge, get left corner, if left edge, get top corner
//                                }
//                                else
//                                {
//
//                                }
//                            }
//                        }
//                    }
//                }
//
//                println(list[0].hitTile)