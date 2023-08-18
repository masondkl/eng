package me.mason.client

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import me.mason.client.components.fps
import me.mason.shared.*
import me.mason.sockets.connect
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import java.lang.Math.toRadians
import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.math.*

//const val WORLD_SIZE = 64
//const val WORLD_RADIUS = WORLD_SIZE / 2
const val COLLISION_SIZE = 4
const val COLLISION_RADIUS = COLLISION_SIZE / 2

val BOMB_FRAMES = Array(4) { Vector2i(it * 4, 496) }
val DEFUSE_MARK = Vector2i(0, 458)
val DEFUSE_MARK_UV_SIZE = Vector2i(38, 38)
val DEFUSE_MARK_SIZE = Vector2f(9.5f, 9.5f)

val COUNTER_PLAYER = Vector2i(0, 500)
val TERRORIST_PLAYER = Vector2i(4, 500)
val DEAD_PLAYER = Vector2i(8, 500)
val PLAYER_UV_SIZE = Vector2i(4, 4)
val SELECTED = Vector2i(11, 504)

val RED_SPAWN_MARKER = Vector2i(15, 504)
val BLUE_SPAWN_MARKER = Vector2i(19, 504)

val BULLET = Vector2i(9, 506)
val BULLET_SIZE = Vector2f(0.5f, 0.5f)
val BULLET_UV_SIZE = Vector2i(1, 1)

//suspend fun main(args: Array<String>) {
//    if (args.isNotEmpty()) myntServer()
//    else client()
//}

data class Bullet(val start: Vector2f, val pos: Vector2f, val dir: Vector2f, var tileHit: Float)

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

val WALL_COLORS = arrayOf(
    Vector4f(255f, 0f, 0f, 0.33f),
    Vector4f(255f, 255f, 0f, 0.33f),
    Vector4f(0f, 255f, 0f, 0.33f),
    Vector4f(0f, 255f, 255f, 0.33f),
    Vector4f(0f, 0f, 255f, 0.33f)
)

suspend fun editor(fileName: String, inWorldSize: Int) = window {
    val atlas = atlas(Paths.get("shooter_flipped.png"))
    val colorShader = Shader(Paths.get("color.vert"), Paths.get("color.frag"), 3, 4)
    val textureShader = Shader(Paths.get("texture.vert"), Paths.get("texture.frag"), 3, 1)
    val matrix = BufferUtils.createFloatBuffer(16)
    val colorBuffer = ShaderBuffer(colorShader) { start, count ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        shader.program.let {
            glUseProgram(it)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, projection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, view.get(matrix))
        }
        glBindVertexArray(vao)
        shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, start, count)
        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glUseProgram(0)
    }
    val textureBuffer = ShaderBuffer(textureShader) { start, count ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        shader.program.let {
            glUseProgram(it)
            glUniform1i(glGetUniformLocation(it, "TEX_SAMPLER"), 0)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, projection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, view.get(matrix))
        }
        glBindTexture(GL_TEXTURE_2D, atlas)
        glActiveTexture(0)
        glBindVertexArray(vao)
        shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, start, count)
        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glUseProgram(0)
    }
    glEnable(GL_DEPTH_TEST)
    glDepthFunc(GL_LESS)
    glEnable(GL_STENCIL_TEST)
    glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
    var worldSize = inWorldSize
    val counterSpawns = HashSet<Vector2f>()
    val terroristSpawns = HashSet<Vector2f>()
    var world = ByteArray(worldSize * worldSize) { 6.toByte() }
    var wallColor = 0
    var walls = ArrayList<Bounds>()
    fun nextWallColor(): Vector4f {
        val color = WALL_COLORS[wallColor]
        wallColor = (wallColor + 1) % WALL_COLORS.size
        return color
    }
    var selected = 0
    val path = Paths.get(fileName)
    if (path.exists()) {
        ByteBuffer.wrap(Paths.get(fileName).readBytes()).run {
            worldSize = int
            world = ByteArray(worldSize * worldSize); get(world)
            walls = ArrayList()
            for (i in 0 until int) walls.add(Bounds(Vector2f(float, float), Vector2f(float, float)))
            for (i in 0 until int) counterSpawns.add(Vector2f(float, float))
            for (i in 0 until int) terroristSpawns.add(Vector2f(float, float))
        }
    }
    glfwSetScrollCallback(window) { _, x, y ->
        selected = if (y > 0) min(selected + 1, TILES.size - 1)
            else max(selected - 1, 0)
    }
    keyEvent += press@{ code, action ->
        if (code != GLFW_KEY_S || action != GLFW_PRESS || !keyState[GLFW_KEY_LEFT_SHIFT]) return@press
        val sizeBytes = 16
        val worldBytes = worldSize * worldSize
        val spawnBytes = counterSpawns.size * 8 + terroristSpawns.size * 8
        val wallBytes = walls.size * 16
        val buffer = allocate(sizeBytes + worldBytes + spawnBytes + wallBytes).apply {
            putInt(worldSize)
            put(world)
            putInt(walls.size)
            walls.forEach { wall ->
                putFloat(wall.min.x); putFloat(wall.min.y)
                putFloat(wall.max.x); putFloat(wall.max.y)
            }
            putInt(counterSpawns.size)
            counterSpawns.forEach {
                putFloat(it.x)
                putFloat(it.y)
            }
            putInt(terroristSpawns.size)
            terroristSpawns.forEach {
                putFloat(it.x)
                putFloat(it.y)
            }
        }
        println("Saved")
        Paths.get(fileName).writeBytes(buffer.array())
    }
    mouseEvent += right@{ code, action ->
        if (code != GLFW_MOUSE_BUTTON_2 || action != GLFW_PRESS || keyState[GLFW_KEY_LEFT_CONTROL]) return@right
        val tile = mouseWorld().round(Vector2f()).int()
        if (tile.x < 0 || tile.x >= worldSize || tile.y < 0 || tile.y >= worldSize) return@right
        val index = tile.x + tile.y * worldSize
        val spawn = Vector2f((index % worldSize).toFloat(), (index / worldSize).toFloat())
        if (counterSpawns.contains(spawn)) {
            terroristSpawns.add(spawn)
            counterSpawns.remove(spawn)
        } else if (terroristSpawns.contains(spawn)) terroristSpawns.remove(spawn)
        else counterSpawns.add(spawn)
    }
    val tempWall = Bounds()
    mouseEvent += right@{ code, action ->
        if (code != GLFW_MOUSE_BUTTON_1 || !keyState[GLFW_KEY_LEFT_CONTROL]) return@right
        val mouse = mouseWorld()
        var toRemove = -1
        for (i in walls.indices) {
            val wall = walls[i]
            if (
                wall.min.x > mouse.x || wall.max.x < mouse.x ||
                wall.min.y > mouse.y || wall.max.y < mouse.y
            ) continue
            toRemove = i
            break
        }
        if (toRemove != -1) {
            walls.removeAt(toRemove)
        }
        return@right
    }
    mouseEvent += right@{ code, action ->
        if (code != GLFW_MOUSE_BUTTON_2 || !keyState[GLFW_KEY_LEFT_CONTROL]) return@right
        val mouse = mouseWorld()
        val tile = mouse.round(Vector2f())
        if (action == GLFW_PRESS) {
            tempWall.min.set(tile)
        } else if (action == GLFW_RELEASE) {
            tempWall.max.set(tile)
            val min = tempWall.min.min(tempWall.max, Vector2f()).sub(TILE_RADIUS)
            val max = tempWall.min.max(tempWall.max, Vector2f()).add(TILE_RADIUS)
            tempWall.min.set(min)
            tempWall.max.set(max)
            walls.add(Bounds(tempWall.min.copy(), tempWall.max.copy()))
        }

    }
    val topLeft = Vector2f(-38f, 20.5f)
    onFixed(144) { _, _ ->
        val motion = Vector2f(0f, 0f).apply {
            if (keyState[GLFW_KEY_W]) y += 1
            if (keyState[GLFW_KEY_S]) y -= 1
            if (keyState[GLFW_KEY_A]) x -= 1
            if (keyState[GLFW_KEY_D]) x += 1
        }.normal().mul(0.125f)
        camera.add(motion)
        move()
        if (mouseState[GLFW_MOUSE_BUTTON_1] && !keyState[GLFW_KEY_LEFT_CONTROL]) {
            val mouse = mouseWorld()
            val tile = mouse.round(Vector2f()).int()
            val index = tile.x + tile.y * worldSize
            if (tile.x < 0 || tile.x >= worldSize || tile.y < 0 || tile.y >= worldSize) return@onFixed
            world[index] = selected.toByte()
        }
    }
    val wallEntity = colorBuffer.reserve(200000) {
        onTick { _, _ ->
            clear()
            wallColor = 0
            walls.forEach { wall ->
                val min = wall.min
                val max = wall.max
                val dim = max.sub(min, Vector2f())
                val rad = dim.div(2f, Vector2f())
                colorQuad(
                    rad.add(min),
                    dim,
                    nextWallColor(),
                    z = 10
                )
            }
        }
    }
    val editorEntity = textureBuffer.reserve(200000) {
        onTick { _, _ ->
            clear()
            TILES.forEachIndexed { idx, it ->
                textureQuad(
                    Vector2f(TILE_SIZE.x * idx.toFloat(), 0f).add(topLeft).add(camera), TILE_SIZE,
                    it, TILE_UV_SIZE, z = 1
                )
            }
            textureQuad(
                Vector2f(TILE_SIZE.x * selected.toFloat(), 0f).add(topLeft).add(camera), TILE_SIZE,
                SELECTED, TILE_UV_SIZE, z = 2
            )
            textureQuad(camera, TILE_SIZE, COUNTER_PLAYER, PLAYER_UV_SIZE, z = 2)
            for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
                val cameraTile = camera.round(Vector2f()).int()
                val tile = Vector2i(index % RENDER_SIZE, index / RENDER_SIZE).sub(cameraTile).sub(RENDER_RADIUS, RENDER_RADIUS)
                if (tile.x < 0 || tile.x >= worldSize || tile.y < 0 || tile.y >= worldSize) continue
                val tileIndex = tile.let { it.x + it.y * worldSize }
                val tileFloat = tile.float()
                textureQuad(tileFloat, TILE_SIZE, TILES[world[tileIndex].toInt()], TILE_UV_SIZE)
                if (counterSpawns.contains(tileFloat)) {
                    textureQuad(tileFloat, TILE_SIZE, RED_SPAWN_MARKER, TILE_UV_SIZE, z = 2)
                } else if (terroristSpawns.contains(tileFloat)) {
                    textureQuad(tileFloat, TILE_SIZE, BLUE_SPAWN_MARKER, TILE_UV_SIZE, z = 2)
                }
            }
        }
    }
    onTick { _, _ ->
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        editorEntity.draw()
        wallEntity.draw()

        glfwSwapBuffers(window)
        glfwPollEvents()
    }
}

const val IN_JOIN = 0.toByte()
const val IN_EXIT = 1.toByte()
const val IN_MAP = 2.toByte()
const val IN_POS = 3.toByte()
const val IN_SHOOT = 4.toByte()
const val IN_DIE = 5.toByte()
const val IN_RESPAWN = 6.toByte()
const val IN_TELEPORT = 7.toByte()
const val IN_END_PLANT = 8.toByte()
const val IN_END_DEFUSE = 9.toByte()
const val IN_CONFIRM_START_DEFUSE = 10.toByte()
const val IN_BOMB = 11.toByte()
const val IN_CLEAR_BOMB = 12.toByte()

const val OUT_JOIN = 0.toByte()
const val OUT_PING = 1.toByte()
const val OUT_POS = 2.toByte()
const val OUT_SHOOT = 3.toByte()
const val OUT_START_PLANT = 4.toByte()
const val OUT_START_DEFUSE = 5.toByte()
const val OUT_CANCEL_PLANT = 6.toByte()
const val OUT_CANCEL_DEFUSE = 7.toByte()

suspend fun client() = window {
    title = "title"
    dim = Vector2i(1280, 720)
    val font = font(Paths.get("vt323.fnt"))
    val fontAtlas = atlas(Paths.get("vt323.png"))
    val atlas = atlas(Paths.get("shooter_flipped.png"))
    val textureShader = Shader(Paths.get("texture.vert"), Paths.get("texture.frag"), 3, 1)
    val fontShader = Shader(Paths.get("font.vert"), Paths.get("font.frag"), 3, 1)
    val colorShader = Shader(Paths.get("color.vert"), Paths.get("color.frag"), 3, 4)
    val fovShader = Shader(Paths.get("fov.vert"), Paths.get("fov.frag"), 3)
    val matrix = BufferUtils.createFloatBuffer(16)
    val textureBuffer = ShaderBuffer(textureShader) { start, count ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        shader.program.let {
            glUseProgram(it)
            glUniform1i(glGetUniformLocation(it, "TEX_SAMPLER"), 0)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, projection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, view.get(matrix))
        }
        glBindTexture(GL_TEXTURE_2D, atlas)
        glActiveTexture(0)
        glBindVertexArray(vao)
        shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, start, count)
        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glUseProgram(0)
    }
    val fontBuffer = ShaderBuffer(fontShader, size = 64000) { start, count ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        shader.program.let {
            glUseProgram(it)
            glUniform1i(glGetUniformLocation(it, "TEX_SAMPLER"), 0)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, projection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, view.get(matrix))
        }
        glBindTexture(GL_TEXTURE_2D, fontAtlas)
        glActiveTexture(0)
        glBindVertexArray(vao)
        shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, start, count)
        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glUseProgram(0)
    }
    val colorBuffer = ShaderBuffer(colorShader, size = colorShader.quad) { start, count ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        shader.program.let {
            glUseProgram(it)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, projection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, view.get(matrix))
        }
        glBindVertexArray(vao)
        shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, start, count)
        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glUseProgram(0)
    }
    val fovBuffer = ShaderBuffer(fovShader, size = 68000) { start, count ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        shader.program.let {
            glUseProgram(it)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, projection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, view.get(matrix))
        }
        glBindVertexArray(vao)
        shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, start, count)
        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glUseProgram(0)
    }
    val bullets = BitSet()
    val bulletStates = Array(256) {
        Bullet(Vector2f(), Vector2f(), Vector2f(), 0f)
    }
    val receivedMap = AtomicBoolean(false)
    lateinit var map: TileMap
    lateinit var tileColliders: Array<Collider>
    lateinit var corners: ArrayList<Vector2f>

    var id = 0
    val dead = BitSet(256)
    val players = BitSet(256)
    val fromPositions = Array(256) { Vector2f() }
    val toPositions = Array(256) { Vector2f() }
    val colliders = Array(256) { Collider(Vector2f(), TILE_SIZE.sub(0.1f, 0.1f)) }
    val updates = Array(256) { 0L }
    var planted = false
    var planting = false
    var defusing = false
    val bomb = Vector2f()
    suspend fun interpolated(id: Int, result: Vector2f): Vector2f = Pool.vec2f.defer { take ->
        val from = fromPositions[id]
        val t = (timeMillis - updates[id]) / 50f
        val diff = (toPositions[id].sub(from, take()))
        result.set(from.add(diff.mul(t, take()), take()))
    }
    suspend fun collider(of: Int): Collider =
        colliders[of].apply { pos.set(if (id != of) interpolated(of, Vector2f()) else camera) }

    glEnable(GL_DEPTH_TEST)
    glDepthFunc(GL_LESS)
    glEnable(GL_STENCIL_TEST)
    glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)

    camera.set(1f, 1f)
    move()

    val connection = connect("localhost", 9999) {
        var lastPing = timeMillis
        var lastPos = timeMillis
        byte(OUT_JOIN)
        id = int()
        println("Joined as player $id")
        coroutineScope {
            mouseEvent += press@{ code, action ->
                if (code != GLFW_MOUSE_BUTTON_1 || action != GLFW_PRESS || dead[id]) return@press
                val mouseWorld = mouseWorld()
                val theta = atan2(camera.y - mouseWorld.y, camera.x - mouseWorld.x) + PI.toFloat()
                val dir = Vector2f(cos(theta), sin(theta))
                val nextBullet = bullets.nextClearBit(0)
                bulletStates[nextBullet].also {
                    it.start.set(camera)
                    it.pos.set(camera.copy())
                    it.dir.set(dir)
                    it.tileHit = map.world.tiledRaycast(map.worldSize, camera, dir).distance
                }; bullets.set(nextBullet)
                runBlocking {
                    writeLock.withLock {
                        byte(OUT_SHOOT)
                        vec2f(dir)
                    }
                }
            }
            keyEvent += press@{ code, action ->
                if (code != GLFW_KEY_E || dead[id] || planted) return@press
                if (action == GLFW_PRESS) {
                    planting = true
                    println("send start plant")
                    runBlocking { writeLock.withLock {
                        byte(OUT_START_PLANT)
                    } }
                    println("finish start plant")
                } else if (action == GLFW_RELEASE) {
                    println("send cancel plant")
                    runBlocking { writeLock.withLock {
                        byte(OUT_CANCEL_PLANT)
                    } }
                    println("finish cancel plant")
                }
            }
            keyEvent += press@{ code, action ->
                if (code != GLFW_KEY_E || dead[id] || !planted) return@press
                //TODO: check that they can defuse
                if (action == GLFW_PRESS) {
                    println("send start defuse")
                    runBlocking { writeLock.withLock {
                        byte(OUT_START_DEFUSE)
                    } }
                    println("finish start defuse")
                } else if (action == GLFW_RELEASE) {
                    println("send cancel defuse")
                    runBlocking { writeLock.withLock {
                        byte(OUT_CANCEL_DEFUSE)
                    } }
                    println("finish cancel defuse")
                }
            }
            launch { try { while (open) {
                val now = timeMillis
                //TODO: take lock if write responses
                writeLock.withLock {
                    if (now - lastPing > 1000) {
                        byte(OUT_PING)
                        lastPing = now
                    }
                    if (now - lastPos > 50) {
                        byte(OUT_POS)
                        vec2f(camera)
                        lastPos = now
                    }
                }
            } } catch (err: Throwable) { close() } }
            launch { try { while (open) when (byte()) {
                IN_JOIN -> {
                    val connected = int()
                    println("Player ${connected} joined")
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
                            Collider(Vector2f(x.toFloat(), y.toFloat()), TILE_SIZE)
                        } else Collider(Vector2f(0f, 0f), Vector2f(0f, 0f))
                    }
                    corners = ArrayList<Vector2f>().apply {
                        map.walls.indices.forEach {
                            val wall = map.walls[it]
                            val min = wall.min
                            val max = wall.max
                            add(Vector2f(min.x, min.y))
                            add(Vector2f(max.x, min.y))
                            add(Vector2f(min.x, max.y))
                            add(Vector2f(max.x, max.y))
                        }
                    }
                    receivedMap.set(true)
                }
                IN_SHOOT -> {
                    val pos = vec2f()
                    val dir = vec2f()
                    val nextBullet = bullets.nextClearBit(0)
                    bulletStates[nextBullet].also {
                        it.start.set(pos)
                        it.pos.set(pos)
                        it.dir.set(dir)
                        it.tileHit = map.world.tiledRaycast(map.worldSize, camera, dir).distance
                    }; bullets.set(nextBullet)
                }
                IN_DIE -> dead.set(int())
                IN_RESPAWN -> {
                    val respawned = int()
                    dead.clear(respawned)
                }
                IN_TELEPORT -> {
                    val to = vec2f()
                    camera.set(to)
                    move()
                }
                IN_END_DEFUSE -> {
                    defusing = false
                }
                IN_BOMB -> {
                    println("in_bomb")
                    bomb.set(vec2f())
                    planted = true
                    planting = false
                }
                IN_CLEAR_BOMB -> {
                    println("clear bomb")
                    planted = false
                }
                IN_CONFIRM_START_DEFUSE -> {
                    defusing = true
                }
            } } catch(err: Throwable) { close() }}
        }
    }
    val results = ArrayList<TiledRay>()
//    val sortedCorners = ArrayList<Vector2f>()
    onClosed { runBlocking { connection?.close() } }

    onFixed(60) { _, elapsed ->
        if (!receivedMap.get() || dead[id]) return@onFixed
        val cameraTile = camera.round(Vector2f()).int()
        val nearbyTileColliders = (0 until COLLISION_SIZE * COLLISION_SIZE).mapNotNull {
            val tile = Vector2i(it % COLLISION_SIZE, it / COLLISION_SIZE).add(cameraTile).sub(COLLISION_RADIUS, COLLISION_RADIUS)
            if (tile.x < 0 || tile.x >= map.worldSize || tile.y < 0 || tile.y >= map.worldSize) null
            else {
                val tileIndex = tile.x + tile.y * map.worldSize
                tileColliders[tileIndex]
            }
        }
        val motion = Vector2f(0f, 0f).apply {
            if (keyState[GLFW_KEY_W]) y += 1
            if (keyState[GLFW_KEY_S]) y -= 1
            if (keyState[GLFW_KEY_A]) x -= 1
            if (keyState[GLFW_KEY_D]) x += 1
        }.normal().mul(0.135f)
        collider(id).move(motion, nearbyTileColliders)
        camera.add(collider(id).move(motion, nearbyTileColliders))
        move()
    }
    val fontEntity = fontBuffer.reserve(64000) {
        fps(font)
    }
    val unclippedEntity = textureBuffer.reserve(RENDER_SIZE * RENDER_SIZE * textureShader.quad) {
        onTick { delta, elapsed ->
            if (!receivedMap.get()) return@onTick
            clear()
            //Local Player
            textureQuad(
                camera, TILE_SIZE,
                if (dead[id]) DEAD_PLAYER else COUNTER_PLAYER, PLAYER_UV_SIZE,
                z = 3
            )
            //World
            for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
                val offset = camera.round(Vector2f()).int().add(RENDER_RADIUS, RENDER_RADIUS)
                val tile = Vector2i(index % RENDER_SIZE, index / RENDER_SIZE).sub(offset)
                if (tile.x < 0 || tile.x >= map.worldSize || tile.y < 0 || tile.y >= map.worldSize) continue
                val tileIndex = tile.let { it.x + it.y * map.worldSize }
                textureQuad(
                    tile.float(),
                    TILE_SIZE,
                    TILES[map.world[tileIndex].toInt()],
                    TILE_UV_SIZE,
                    z = if (map.world[tileIndex] in SOLIDS) 3 else 1
                )
            }
        }
    }
    val clippedEntity = textureBuffer.reserve(513 * textureShader.quad) {
        onFixed(144) { delta, elapsed ->
            if (!receivedMap.get()) return@onFixed
            bullets.clearIf {
                val bullet = bulletStates[it]
                val shootRay = raycast<Unit>(bullet.pos, bullet.dir.mul(0.33f, Vector2f()), max = 1f) {
                    val intersected = players.first { player ->
                        val interpolated = interpolated(player, Vector2f())
                        val min = Vector2f(interpolated.x - TILE_RADIUS.x, interpolated.y - TILE_RADIUS.y)
                        val max = Vector2f(interpolated.x + TILE_RADIUS.x, interpolated.y + TILE_RADIUS.y)
                        it.x > min.x && it.x < max.x && it.y > min.y && it.y < max.y
                    }
                    intersected != -1 && intersected != id
                }
                if (!shootRay.result || shootRay.distance > bullet.tileHit) bullet.start.distance(bullet.pos) > bullet.tileHit
                else bullet.start.distance(bullet.pos) > shootRay.distance
            }
            clear()
            //Bullets
            bullets.forEach {
                val bullet = bulletStates[it]
                textureQuad(
                    bullet.pos,
                    BULLET_SIZE,
                    BULLET,
                    BULLET_UV_SIZE,
                    z = 3
                )
                bullet.pos.add(bullet.dir.x * 2.5f, bullet.dir.y * 2.5f)
            }
            //Players
            players.forEach {
                if (it == id || it >= 256) return@forEach
                textureQuad(
                    interpolated(it, Vector2f()), TILE_SIZE,
                    if (dead[it]) DEAD_PLAYER else COUNTER_PLAYER, PLAYER_UV_SIZE,
                    z = 3
                )
            }
            //Bomb
            if (planted) {
//                println("drawin bomb at ${bomb.x}, ${bomb.y}")
                val frame = floor(elapsed % 4f).toInt()
                textureQuad(bomb, TILE_SIZE, BOMB_FRAMES[frame], TILE_UV_SIZE, z = 3)
                textureQuad(bomb, DEFUSE_MARK_SIZE, DEFUSE_MARK, DEFUSE_MARK_UV_SIZE, z = 3)
            }
        }
    }
    val fogEntity = colorBuffer.reserve(colorShader.quad) {
        onTick { delta, elapsed ->
            if (!receivedMap.get()) return@onTick
            clear()
            colorQuad(
                Vector2f(map.worldSize / 2f - 0.5f, map.worldSize / 2f - 0.5f),
                Vector2f(map.worldSize.toFloat(), map.worldSize.toFloat()),
                Vector4f(0f, 0f, 0f, 0.25f), z = 2
            )
        }
    }
    val fovEntity = fovBuffer.reserve(68000) {
        onFixed(60) { delta, elapsed ->
            if (!receivedMap.get() || dead[id]) return@onFixed
            val mouseWorld = mouseWorld()
            results.clear()
            corners.sortBy {
                if (mouseWorld.x > camera.x) atan2(it.y - camera.y, it.x - camera.x)
                else atan2(camera.y - it.y, camera.x - it.x)
            }
            val mouse = mouseWorld()
            val first = (atan2(mouse.y - camera.y, mouse.x - camera.x) - toRadians(41.0).toFloat()).let { Vector2f(cos(it), sin(it)) }
            val last = (atan2(mouse.y - camera.y, mouse.x - camera.x) + toRadians(41.0).toFloat()).let { Vector2f(cos(it), sin(it)) }
            results.add(map.world.tiledRaycast(map.worldSize, camera, first))
            corners.forEach {
                val cornerDist = camera.distance(it)
                val mouseDist = camera.distance(mouseWorld)
                val normalizedLook =
                    Vector2f((mouseWorld.x - camera.x) / mouseDist, (mouseWorld.y - camera.y) / mouseDist)
                val normalizedCorner = Vector2f((it.x - camera.x) / cornerDist, (it.y - camera.y) / cornerDist)
                val dot = normalizedLook.dot(normalizedCorner)
                if (dot >= 0.75) {
                    val theta = atan2(camera.y - it.y, camera.x - it.x) + PI.toFloat()
                    val middle = Vector2f(cos(theta), sin(theta))
                    results.add(
                        map.world.tiledRaycast(
                            map.worldSize,
                            camera,
                            Vector2f(cos(theta - 0.00001f), sin(theta - 0.00001f))
                        )
                    )
                    results.add(map.world.tiledRaycast(map.worldSize, camera, middle))
                    results.add(
                        map.world.tiledRaycast(
                            map.worldSize,
                            camera,
                            Vector2f(cos(theta + 0.00001f), sin(theta + 0.00001f))
                        )
                    )
                }
            }
            results.add(map.world.tiledRaycast(map.worldSize, camera, last))
            clear()
            (1 until results.size).forEach {
                val previous = results[it - 1]
                val current = results[it]
                fovTriangle(camera, previous.hit, current.hit, z = 2)
            }
        }
    }
    onTick { delta, elapsed ->
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        if (receivedMap.get()) {
            fontEntity.draw()
            unclippedEntity.draw()
            glStencilFunc(GL_ALWAYS, 1, 0xFF)
            glStencilMask(0xFF)
            glColorMask(false, false, false, false)
            fovEntity.draw()
            glStencilMask(0x00)
            glStencilFunc(GL_EQUAL, 1, 0xFF)
            glColorMask(true, true, true, true)
            clippedEntity.draw()
            glStencilFunc(GL_NOTEQUAL, 1, 0xFF)
            fogEntity.draw()
            glStencilMask(0xFF)
            glStencilFunc(GL_ALWAYS, 0, 0xFF)
        }
        glfwSwapBuffers(window)
        glfwPollEvents()
    }
}

suspend fun main(args: Array<String>) =
    if (args.isEmpty()) client()
    else if (args.size == 2) editor(args[0], Integer.parseInt(args[1]))
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