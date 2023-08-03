package me.mason.client

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import me.mason.shared.*
import me.mason.sockets.connect
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import java.nio.ByteBuffer.allocate
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.writeBytes
import kotlin.math.*

//const val WORLD_SIZE = 64
//const val WORLD_RADIUS = WORLD_SIZE / 2
const val COLLISION_SIZE = 4
const val COLLISION_RADIUS = COLLISION_SIZE / 2

val PLAYER = Vector2i(0, 4)
val DEAD_PLAYER = Vector2i(4, 4)
val PLAYER_UV_SIZE_VEC = Vector2i(4, 4)
val SELECTED = Vector2i(11, 4)

val RED_SPAWN_MARKER = Vector2i(15, 4)
val BLUE_SPAWN_MARKER = Vector2i(19, 4)

val BULLET = Vector2i(9, 5)
val BULLET_SIZE_VEC = Vector2f(0.5f, 0.5f)
val BULLET_UV_SIZE_VEC = Vector2i(1, 1)

//suspend fun main(args: Array<String>) {
//    if (args.isNotEmpty()) myntServer()
//    else client()
//}

data class Bullet(val start: Vector2f, val pos: Vector2f, val dir: Vector2f, val tileHit: Float)

fun Window.mouseWorld(): Vector2f {
    val mouseX = BufferUtils.createDoubleBuffer(1)
    val mouseY = BufferUtils.createDoubleBuffer(1)
    val width = BufferUtils.createIntBuffer(1)
    val height = BufferUtils.createIntBuffer(1)
    glfwGetCursorPos(window, mouseX, mouseY)
    glfwGetWindowSize(window, width, height)
    return Vector2f(
        (mouseX.get().toFloat() - 640f) / (1280 / 80f) + camera.x,
        (height.get().toFloat() - mouseY.get().toFloat() - 360f) / (720f / 45f) + camera.y
    )
}


fun editor(worldSize: Int) = window {
    val textureShader = Shader(Paths.get("texture.vert"), Paths.get("texture.frag"), 3, 1)
    val spawns = HashMap<Int, Byte>()
    val world = ByteArray(worldSize * worldSize) { 6.toByte() }
    var selected = 0

    glfwSetScrollCallback(window) { _, x, y ->
        selected = if (y > 0) min(selected + 1, TILES.size - 1)
            else max(selected - 1, 0)
    }

    keyEvent += press@{ code, action ->
        if (code != GLFW_KEY_S || action != GLFW_PRESS || !keys[GLFW_KEY_LEFT_CONTROL]) return@press
        spawns.map { (k, v) -> k to v }
        val buffer = allocate(8 + worldSize * worldSize + spawns.size * 5)
        buffer.putInt(worldSize)
        buffer.put(world)
        buffer.putInt(spawns.size)
        spawns.forEach { (tile, type) ->
            buffer.putInt(tile)
            buffer.put(type)
        }
        println("Saved")
        Paths.get("result.map").writeBytes(buffer.array())
    }

    mouseEvent += right@{ code, action ->
        if (code != GLFW_MOUSE_BUTTON_2 || action != GLFW_PRESS) return@right
        val tile = mouseWorld().round(Vector2f()).int()
        if (tile.x < 0 || tile.x >= worldSize || tile.y < 0 || tile.y >= worldSize) return@right
        val index = tile.x + tile.y * worldSize
        if (!spawns.contains(index)) spawns[index] = RED_SPAWN
        else if (spawns[index] == RED_SPAWN) spawns[index] = BLUE_SPAWN
        else spawns.remove(index)
    }
//    buffer(512, Paths.get("shooter.png"), camera, {  }) {
//        val topLeft = vec(-38f, 20.5f)
//        var quadIndex = 0
//        tick { _, _ ->
//            using(textureShader, 0) {
//                clearQuad(0 until quadIndex)
//                quadIndex = 0
//                val cameraTile = camera.position.rounded().int()
//                for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
//                    val tile = vec(index % RENDER_SIZE, index / RENDER_SIZE) - RENDER_RADIUS + cameraTile
//                    if (tile.x < 0 || tile.x >= worldSize || tile.y < 0 || tile.y >= worldSize) continue
//                    val tileIndex = tile.x + tile.y * worldSize
//                    textureQuad(quadIndex++, tile.float(), TILE_SIZE_VEC, TILES[world[tileIndex].toInt()], TILE_UV_SIZE_VEC)
//                    if (spawns[tileIndex] == RED_SPAWN) {
//                        textureQuad(quadIndex++, tile.float(), TILE_SIZE_VEC, RED_SPAWN_MARKER, TILE_UV_SIZE_VEC)
//                    } else if (spawns[tileIndex] == BLUE_SPAWN) {
//                        textureQuad(quadIndex++, tile.float(), TILE_SIZE_VEC, BLUE_SPAWN_MARKER, TILE_UV_SIZE_VEC)
//                    }
//                }
//            }
//            if (buttons[GLFW_MOUSE_BUTTON_1]) {
//                val tile = mouseWorld().round().int()
//                val index = tile.x + tile.y * worldSize
//                if (tile.x < 0 || tile.x >= worldSize || tile.y < 0 || tile.y >= worldSize) return@tick
//                world[index] = selected.toByte()
//            }
//        }
//        fixed(144) { _ ->
//            val motion = vec(0f, 0f).apply {
//                if (keys[GLFW_KEY_W]) y += 1
//                if (keys[GLFW_KEY_S]) y -= 1
//                if (keys[GLFW_KEY_A]) x -= 1
//                if (keys[GLFW_KEY_D]) x += 1
//            }.normalize() * 0.125f
//            camera.position += motion
//            camera.move()
//        }
//        tick { _, _ ->
//            using(textureShader, 0) {
//                TILES.forEachIndexed { idx, it ->
//                    textureQuad(
//                        quadIndex++,
//                        camera.position + vec(TILE_SIZE_VEC.x * idx.toFloat(), 0f) + topLeft, TILE_SIZE_VEC,
//                        it, TILE_UV_SIZE_VEC
//                    )
//                }
//                textureQuad(
//                    quadIndex++,
//                    camera.position + vec(TILE_SIZE_VEC.x * selected.toFloat(), 0f) + topLeft, TILE_SIZE_VEC,
//                    SELECTED, TILE_UV_SIZE_VEC
//                )
//                textureQuad(quadIndex++, camera.position, TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC)
//            }
//        }
//    }
}


const val IN_JOIN = 0.toByte()
const val IN_EXIT = 1.toByte()
const val IN_MAP = 2.toByte()
const val IN_POS = 3.toByte()

const val OUT_JOIN = 0.toByte()
const val OUT_PING = 1.toByte()
const val OUT_POS = 2.toByte()

fun client() = window { runBlocking {
    title = "title"
    dim = Vector2i(1280, 720)
    val textureShader = Shader(Paths.get("texture.vert"), Paths.get("texture.frag"), 3, 1)
    val colorShader = Shader(Paths.get("color.vert"), Paths.get("color.frag"), 3, 4)
    val fovShader = Shader(Paths.get("fov.vert"), Paths.get("fov.frag"), 3)
    val atlas = atlas(Paths.get("shooter.png"))
    val matrix = BufferUtils.createFloatBuffer(16)
    val textureBuffer = Buffer(textureShader) { range ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        textureShader.program.let {
            glUseProgram(it)
            glUniform1i(glGetUniformLocation(it, "TEX_SAMPLER"), 0)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, projection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, view.get(matrix))
        }
        glBindTexture(GL_TEXTURE_2D, atlas)
        glActiveTexture(0)
        glBindVertexArray(vao)
        textureShader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, range.first / shader.stride, (range.last / shader.stride) + 1)
        textureShader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glUseProgram(0)
    }
    val colorBuffer = Buffer(colorShader, size = colorShader.stride * 6) { range ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        colorShader.program.let {
            glUseProgram(it)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, projection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, view.get(matrix))
        }
        glBindVertexArray(vao)
        colorShader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, range.first / shader.stride, (range.last / shader.stride) + 1)
        colorShader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glUseProgram(0)
    }
    val fovBuffer = Buffer(fovShader, size = 64000) { range ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        fovShader.program.let {
            glUseProgram(it)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, projection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, view.get(matrix))
        }
        glBindVertexArray(vao)
        fovShader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, range.first / shader.stride, (range.last / shader.stride) + 1)
        fovShader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glUseProgram(0)
    }
    val bullets = CopyOnWriteArrayList<Bullet>()
    val receivedMap = AtomicBoolean(false)
    lateinit var map: TileMap
    lateinit var tileColliders: Array<Collider>
    lateinit var corners: Array<Array<Vector2f>>

    var id = 0
    val collider = Collider(Vector2f(0f, 0f), TILE_SIZE_VEC)
    val dead = ConcurrentSkipListSet<Int>()

    val players = BitSet(256)
    val fromPositions = Array(256) { Vector2f() }
    val toPositions = Array(256) { Vector2f() }
    val colliders = Array(256) { Collider(Vector2f(), TILE_SIZE_VEC - 0.1f) }
    val updates = Array(256) { 0L }
    fun interpolated(id: Int): Vector2f {
        val from = fromPositions[id]
        val t = (timeMillis - updates[id]) / 50f
        val diff = (toPositions[id] - from)
        return from + (diff * t)
    }
    fun collider(of: Int): Collider =
        colliders[of].apply { pos.set(if (id != of) interpolated(of) else camera) }
    camera.set(1f, 1f)
    move()

    glEnable(GL_DEPTH_TEST)
    glDepthFunc(GL_LESS)
    glEnable(GL_STENCIL_TEST)
    glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
    val connection = connect("localhost", 9999) {
        var lastPing = timeMillis
        var lastPos = timeMillis
        byte(OUT_JOIN)
        id = int()
        println("Connected as player $id")
        coroutineScope {
            launch { try { while (open) {
                val now = timeMillis
                //TODO: take lock if write responses
                if (now - lastPing > 1000) {
                    byte(OUT_PING)
                    lastPing = now
                }
                if (now - lastPos > 50) {
                    byte(OUT_POS)
                    vec2f(camera)
                    lastPos = now
                }
            } } catch (err: Throwable) { close() } }
            launch { try { while (open) when (byte()) {
                IN_JOIN -> {
                    val connected = int()
                    println("Player ${connected} connected")
                    players.set(connected)
                }
                IN_EXIT -> {
                    val exited = int()
                    println("Player ${exited} exited")
                    players.clear(exited)
                }
                IN_POS -> {
                    val player = int()
                    val pos = vec2f()
                    updates[player] = timeMillis
                    fromPositions[player] = toPositions[player].copy()
                    toPositions[player] = pos
                }
                IN_MAP -> {
                    map = map()
                    tileColliders = Array(map.worldSize * map.worldSize) {
                        if (map.world[it] in SOLIDS) {
                            val x = it % map.worldSize
                            val y = it / map.worldSize
                            Collider(Vector2f(x.toFloat(), y.toFloat()), TILE_SIZE_VEC)
                        } else Collider(Vector2f(0f, 0f), Vector2f(0f, 0f))
                    }
                    corners = Array(map.worldSize * map.worldSize) {
                        val tile = Vector2i(it % map.worldSize, it / map.worldSize).float()
                        arrayOf(
                            tile.copy().add(-0.5f, 0.5f), tile.copy().add(0.5f, 0.5f),
                            tile.copy().add(-0.5f, -0.5f), tile.copy().add(0.5f, -0.5f)
                        )
                    }
                    receivedMap.set(true)
                }
            } } catch(err: Throwable) { close() }}
        }
//        mouseEvent += press@{ code, action ->
//            if (code != GLFW_MOUSE_BUTTON_1 || action != GLFW_PRESS || dead.contains(id)) return@press
//            val mouseWorld = mouseWorld()
//            val theta = atan2(camera.y - mouseWorld.y, camera.x - mouseWorld.x) + PI.toFloat()
//            val dir = Vector2f(cos(theta), sin(theta))
//            writes.add {
//                println("send shoot")
//                byte(OUT_SHOOT)
//                vec2f(camera)
//                vec2f(dir)
//            }
//        }
    }
    val fixed = fixed(60)
    val fps20 = fixed(20)
    val results = ArrayList<RayResult<Unit>>()
    val sortedCorners = ArrayList<Vector2f>()
    onClosed { runBlocking { connection?.close() } }
    onTick { delta, elapsed ->
//        if (fixed.get()) println(1f / delta)
        if (!receivedMap.get()) return@onTick
        if (fixed.get() && !dead.contains(id)) {
            val cameraTile = camera.round(Vector2f()).int()
            val nearbyTileColliders = (0 until COLLISION_SIZE * COLLISION_SIZE).mapNotNull {
                val tile = Vector2i(it % COLLISION_SIZE, it / COLLISION_SIZE) - COLLISION_RADIUS + cameraTile
                if (tile.x < 0 || tile.x >= map.worldSize || tile.y < 0 || tile.y >= map.worldSize) null
                else {
                    val tileIndex = tile.x + tile.y * map.worldSize
                    tileColliders[tileIndex]
                }
            }
            val motion = Vector2f(0f, 0f).apply {
                if (keys[GLFW_KEY_W]) y += 1
                if (keys[GLFW_KEY_S]) y -= 1
                if (keys[GLFW_KEY_A]) x -= 1
                if (keys[GLFW_KEY_D]) x += 1
            }.normal() * 0.135f
//            println("mot")
//            println(motion.x)
//            println(motion.y)
            collider(id).move(motion, nearbyTileColliders)
            camera.add(collider(id).move(motion, nearbyTileColliders))
            move()
        }
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        if (!receivedMap.get()) {
            glfwSwapBuffers(window)
            glfwPollEvents()
            return@onTick
        }
        if (!dead.contains(id)
//                && fixed.get()
        ) {
//            quad(0, camera.position, vec(80f, 45f))
            val mouseWorld = mouseWorld()
            results.clear()
            sortedCorners.clear()
            val cameraTile = camera.round(Vector2f()).int()
            for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
                val tile = Vector2i(index % RENDER_SIZE, index / RENDER_SIZE) - RENDER_RADIUS + cameraTile
                if (tile.x < 0 || tile.x >= map.worldSize || tile.y < 0 || tile.y >= map.worldSize) continue
                val tileIndex = tile.let { it.x + it.y * map.worldSize }
                if (map.world[tileIndex] !in SOLIDS) continue
                corners[tileIndex].forEach {
                    sortedCorners.add(it)
                }
            }
            sortedCorners.removeIf {
                val cornerDist = camera.distance(it)
                val mouseDist = camera.distance(mouseWorld)
                val normalizedLook =
                    Vector2f((mouseWorld.x - camera.x) / mouseDist, (mouseWorld.y - camera.y) / mouseDist)
                val normalizedCorner = Vector2f((it.x - camera.x) / cornerDist, (it.y - camera.y) / cornerDist)
                val dot = normalizedLook.dot(normalizedCorner)
                dot < 0.75
            }
            sortedCorners.sortBy {
                if (mouseWorld.x > camera.x) atan2(it.y - camera.y, it.x - camera.x) + PI.toFloat()
                else atan2(camera.y - it.y, camera.x - it.x) + PI.toFloat()
            }
            sortedCorners.forEach {
                val theta = atan2(camera.y - it.y, camera.x - it.x) + PI.toFloat()
                results.add(
                    map.world.tiledRaycast(
                        map.worldSize,
                        camera,
                        Vector2f(cos(theta - 0.00001f), sin(theta - 0.00001f))
                    )
                )
                results.add(map.world.tiledRaycast(map.worldSize, camera, Vector2f(cos(theta), sin(theta))))
                results.add(
                    map.world.tiledRaycast(
                        map.worldSize,
                        camera,
                        Vector2f(cos(theta + 0.00001f), sin(theta + 0.00001f))
                    )
                )
            }
            var triangleIndex = 0
            fovBuffer.apply {
                (1 until results.size).forEach {
                    val previous = results[it - 1]
                    val current = results[it]
                    fovTriangle(triangleIndex++ * triangle, camera, previous.hit, current.hit, z = 2)
                }
                clear(triangleIndex * triangle until fovBuffer.data.size)
            }
            colorBuffer.apply {
                colorQuad(
                    0,
                    Vector2f(map.worldSize / 2f - 0.5f, map.worldSize / 2f - 0.5f),
                    Vector2f(map.worldSize.toFloat(), map.worldSize.toFloat()),
                    Vector4f(0f, 0f, 0f, 0.25f), z = 2
                )
            }
        }
        textureBuffer.apply {
            clear(quad until (RENDER_SIZE * RENDER_SIZE + 513) * quad)
            val cameraTile = camera.round(Vector2f()).int()
            for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
                val tile = Vector2i(index % RENDER_SIZE, index / RENDER_SIZE) - RENDER_RADIUS + cameraTile
                if (tile.x < 0 || tile.x >= map.worldSize || tile.y < 0 || tile.y >= map.worldSize) continue
                val tileIndex = tile.let { it.x + it.y * map.worldSize }
                textureQuad(
                    (index + 1) * quad,
                    tile.float(),
                    TILE_SIZE_VEC,
                    TILES[map.world[tileIndex].toInt()],
                    TILE_UV_SIZE_VEC,
                    z = if (map.world[tileIndex] in SOLIDS) 3 else 1
                )
            }
            textureQuad(0, camera, TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC, z = 2)
            bullets.removeIf { bullet ->
                val shootRay = raycast<Unit>(bullet.pos, bullet.dir * 0.33f, max = 1f) {
                    val intersected = players.first { player ->
                        val interpolated = interpolated(player)
                        val min = interpolated - TILE_SIZE_VEC / 2f
                        val max = interpolated + TILE_SIZE_VEC / 2f
                        it.x > min.x && it.x < max.x && it.y > min.y && it.y < max.y
                    }
                    intersected != -1
                }
                if (shootRay.distance == 0f || shootRay.distance > bullet.tileHit) bullet.start.distance(bullet.pos) > bullet.tileHit
                else bullet.start.distance(bullet.pos) > shootRay.distance
            }
            for (index in 0 until min(256, bullets.size)) {
                val bullet = bullets[index]
                textureBuffer.textureQuad(
                    ((RENDER_SIZE * RENDER_SIZE + 1) + index) * quad,
                    bullet.pos,
                    BULLET_SIZE_VEC,
                    BULLET,
                    BULLET_UV_SIZE_VEC,
                    z = 3
                )
                bullet.pos += bullet.dir * 1.25f
            }
            bullets.removeIf { it.pos.distance(camera) > 100f }
            players.forEach {
                if (it == id || it >= 256) return@forEach
                textureQuad(
                    (RENDER_SIZE * RENDER_SIZE + 257 + it) * quad,
                    interpolated(it), TILE_SIZE_VEC,
                    if (dead.contains(it)) DEAD_PLAYER else PLAYER, PLAYER_UV_SIZE_VEC,
                    z = 3
                )
            }
        }

//        println("camera: ${camera.x}, ${camera.y}")

//        textureBuffer.textureQuad(0, Vector2f(0f, 0f), TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC, z = 1)

        textureBuffer.draw(0 until ((RENDER_SIZE * RENDER_SIZE + 1) * textureBuffer.quad))
        glStencilFunc(GL_ALWAYS, 1, 0xFF)
        glStencilMask(0xFF)
//        println(colorBuffer.data.size)
        glColorMask(false, false, false, false)
        fovBuffer.draw(0 until (results.size * fovBuffer.triangle))
        glStencilMask(0x00)
//        glDisable(GL_DEPTH_TEST)
        glStencilFunc(GL_EQUAL, 1, 0xFF)
        glColorMask(true, true, true, true)
        textureBuffer.draw((RENDER_SIZE * RENDER_SIZE + 1) * textureBuffer.quad until (RENDER_SIZE * RENDER_SIZE + 513) * textureBuffer.quad)
        glStencilFunc(GL_NOTEQUAL, 1, 0xFF)
        colorBuffer.draw(0 until colorBuffer.quad)
        glStencilMask(0xFF)
        glStencilFunc(GL_ALWAYS, 0, 0xFF)
        glEnable(GL_DEPTH_TEST)

        glfwSwapBuffers(window)
        glfwPollEvents()
    }
} }

fun main(args: Array<String>) =
    if (args.isEmpty()) client()
    else if (args.first() == "editor") editor(Integer.parseInt(args[1]))
    else error("Invalid startup arguments")

//            println("theta: $theta")



//            val fovResolutionMultiplier = 5.0f
//            val startRad = theta + fovRad / 2f

//            (0 until (fov * fovResolutionMultiplier).toInt()).forEach {
//                val dirTheta = (startRad - toRadians(it.toDouble() / fovResolutionMultiplier)).toFloat()
//                val dir = Vector2f(cos(dirTheta), sin(dirTheta))
//                if (isNaN(dir.x) || isNaN(dir.y)) {
////                    println("gg")
//                    return@forEach
//                }
//                val result = map.world.tiledRaycast(map.worldSize, camera, dir)
////                    val index = result.hitTile.x + result.hitTile.y * map.worldSize
//                results.add(result)
//            }

//            var fix = true
//            while (fix) {
//                fix = false
//                fixPoints.clear()
//                (1 until results.size).forEach {
//                    val previous = results[it - 1]
//                    val current = results[it]
//                    if ((current.hitTile.x != previous.hitTile.x || current.hitTile.y != previous.hitTile.y) && current.hit.distance(previous.hit) > 1.5) {
//                        lateinit var point: Vector2f
//                        if (current.hit.distance(camera) > previous.hit.distance(camera)) {
//                            point = when (previous.side) {
//                                0 -> previous.hitTile.float() + Vector2f(-0.5f, -0.5f)
//                                1 -> previous.hitTile.float() + Vector2f(-0.5f, 0.5f)
//                                2 -> previous.hitTile.float() + Vector2f(0.5f, 0.5f)
//                                else -> previous.hitTile.float() + Vector2f(0.5f, -0.5f)
//                            }
//                            val pointTheta = atan2(point.y - camera.y, point.x - camera.x)
//                            val pointDir = Vector2f(cos(pointTheta), sin(pointTheta))
//                            val result = map.world.tiledRaycast(map.worldSize, point, pointDir)
//                            fixPoints.add(current.copy().apply { hit.set(point) })
//                            fixPoints.add(result)
//                            fixPoints.add(previous)
////                            fix = true
//                        } else {
//                            point = if (current.side == 0) current.hitTile.float() + Vector2f(-0.5f, 0.5f)
//                                else if (current.side == 1) current.hitTile.float() + Vector2f(0.5f, 0.5f)
//                                else if (current.side == 2) current.hitTile.float() + Vector2f(0.5f, -0.5f)
//                                else current.hitTile.float() + Vector2f(-0.5f, -0.5f)
//                            val pointTheta = atan2(point.y - camera.y, point.x - camera.x)
//                            val pointDir = Vector2f(cos(pointTheta), sin(pointTheta))
//                            val result = map.world.tiledRaycast(map.worldSize, point, pointDir)
//                            fixPoints.add(current.copy().apply { hit.set(point) })
//                            fixPoints.add(previous)
//                            fixPoints.add(result)
////                            fix = true
//                        }
//                    }
//                    fixPoints.add(current)
//                }
//                results = ArrayList(fixPoints)
//            }










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