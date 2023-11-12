package me.mason.client

import me.mason.shared.*
import org.joml.Vector2f
import org.joml.Vector2i
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL30.*
import java.lang.Math.toRadians
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import kotlin.io.path.readBytes
import kotlin.math.*

val COUNTER_PLAYER = Vector2i(0, 115)
val TERRORIST_PLAYER = Vector2i(4, 115)
val DEAD_PLAYER = Vector2i(8, 115)
val PLAYER_UV_DIM = Vector2i(4, 4)

val RED_SPAWN_MARKER = Vector2i(15, 503)
val BLUE_SPAWN_MARKER = Vector2i(19, 503)

val BULLET = Vector2i(9, 121)
val BULLET_UV_DIM = Vector2i(1, 1)
val BULLET_DIM = Vector2f(0.5f, 0.5f)

val GUN = Vector2i(0, 52)
val GUN_FLIPPED = Vector2i(15, 52)
val GUN_UV_DIM = Vector2i(14, 5)
val GUN_DIM = Vector2f(3.5f, 1.25f)

val SETTINGS_ICON = Vector2i(54, 488)
val SETTINGS_ICON_UV_DIM = Vector2i(18, 18)
val SETTINGS_ICON_DIM = Vector2f(4.5f, 4.5f)


val WINDOW = Vector2i(26, 495)
val WINDOW_DIM = Vector2i(9, 9)
val WINDOW_UV_INSETS = Vector2i(4, 4)
val WINDOW_INSETS = Vector2f(1f)

const val COLLISION_DIM = 4
const val COLLISION_RAD = COLLISION_DIM / 2

val GAME_TEXTURE by lazy { Texture(Paths.get("game_atlas_128.png")) }
val FONT_TEXTURE by lazy { Texture(Paths.get("vt323.png")) }
val TEXTURE_INDICES by lazy { mapOf(GAME_TEXTURE to 0, FONT_TEXTURE to 1) }
val TEXTURE_ACTIVE_INDICES by lazy { mapOf(GAME_TEXTURE to GL_TEXTURE0, FONT_TEXTURE to GL_TEXTURE1) }
val TEXTURE_SHADER by lazy {
    Shader(Paths.get("texture.vert"), Paths.get("texture.frag"),
        1, 1, 1
    )
}
val FOV_SHADER by lazy {
    Shader(Paths.get("fov.vert"), Paths.get("fov.frag"),
        3
    )
}

val FONT by lazy { Font(Paths.get("vt323.fnt")) }
val TEXTURE_BUFFER by lazy { GraphicsBuffer(TEXTURE_SHADER, 86000 + 68000) }
//val TEXTURED_SHADER by lazy { Shader(Paths.get("rotate_texture.vert"), Paths.get("rotate_texture.frag"), 1, 3, 2, 1, 1) }
//val TEXTURED_BUFFER by lazy { ShaderBuffer(TEXTURED_SHADER, 64000) }
//val COLORED_SHADER by lazy { Shader(Paths.get("color.vert"), Paths.get("color.frag"), 3, 4) }
//val COLORED_BUFFER by lazy { ShaderBuffer(COLORED_SHADER, 64000) }
//
//val TEXTURED_UI by lazy { TEXTURED_BUFFER.aggregator(32000).apply { len = 32000 } }
//val COLORED_UI by lazy { COLORED_BUFFER.aggregator(32000).apply { len = 32000 } }
//val FOG by lazy { COLORED_BUFFER.aggregator(COLORED_SHADER.quad).apply { len = COLORED_SHADER.quad } }
//val UNCLIPPED by lazy { TEXTURED_BUFFER.aggregator(TEXTURED_SHADER.quad + TEXTURED_SHADER.quad * 9).apply { len = TEXTURED_SHADER.quad + TEXTURED_SHADER.quad * 9 } }
//val WORLD by lazy { FAST_QUAD_BUFFER.aggregator(RENDER_SIZE * RENDER_SIZE * FAST_QUAD_SHADER.fastQuad) }
//val CLIPPED by lazy { TEXTURED_BUFFER.aggregator(513 * TEXTURED_SHADER.quad) }

val PLAYER by lazy { Cursor(TEXTURE_SHADER.quad, TEXTURE_BUFFER) }
val WORLD by lazy { Cursor(RENDER_DIM * RENDER_DIM * TEXTURE_SHADER.quad, TEXTURE_BUFFER) }
val FPS by lazy { Cursor(16 * TEXTURE_SHADER.quad, TEXTURE_BUFFER) }
val FOG by lazy { Cursor(TEXTURE_SHADER.quad, TEXTURE_BUFFER) }
val FOV by lazy { Cursor(TEXTURE_SHADER.quad * 68000, TEXTURE_BUFFER) }

val AUDIO by lazy { AudioSource(64) }
val SHOOT = WaveData.create(Paths.get("shoot.wav").readBytes())

val MAP_DIRECTORY: Path = Paths.get("maps")

object Maps {
    val mapNames = MAP_DIRECTORY.toFile().listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
    val counterSpawns = Array(mapNames.size) { ArrayList<Vector2f>() }
    val terroristSpawns = Array(mapNames.size) { ArrayList<Vector2f>() }
    val maps = Array(mapNames.size) { map ->
        ByteBuffer.wrap(MAP_DIRECTORY.resolve("${mapNames[map]}.map").readBytes()).run {
            val worldSize = int
            val world = ByteArray(worldSize * worldSize); get(world)
            val walls = ArrayList<Bounds>()
            val plantBounds = ArrayList<Bounds>()
            for (i in 0 until int) walls.add(Bounds(
                Vector2f(float, float),
                Vector2f(float, float)
            ))
            for (i in 0 until int) counterSpawns[map].add(Vector2f(float, float))
            for (i in 0 until int) terroristSpawns[map].add(Vector2f(float, float))
            for (i in 0 until int) plantBounds.add(Bounds(
                Vector2f(float, float),
                Vector2f(float, float)
            ))
            val colliders = Array(worldSize * worldSize) {
                if (world[it] in SOLIDS) {
                    val x = it % worldSize
                    val y = it / worldSize
                    Collider(Vector2f(x.toFloat(), y.toFloat()), TILE_DIM)
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
            TileMap(mapNames[map], worldSize, walls, plantBounds, world, colliders, corners)
        }
    }
    fun random() = maps.indices.random()
    operator fun get(map: Int) = maps[map]
}


fun Window.drawTextured(): GraphicsBuffer.(Int) -> (Unit) = { elements ->
    shader.program.let { program ->
        glUseProgram(program)
        glUniform1iv(glGetUniformLocation(program, "SAMPLERS"), (0 until 2).toList().toIntArray())
        glUniformMatrix4fv(glGetUniformLocation(program, "uiProjection"), false, uiProjection.get(matrix))
        glUniformMatrix4fv(glGetUniformLocation(program, "uiView"), false, uiView.get(matrix))
        glUniformMatrix4fv(glGetUniformLocation(program, "projection"), false, projection.get(matrix))
        glUniformMatrix4fv(glGetUniformLocation(program, "view"), false, view.get(matrix))
    }
    TEXTURE_ACTIVE_INDICES.forEach { (tex, activeId) ->
        glActiveTexture(activeId)
        glBindTexture(GL_TEXTURE_2D, tex)
    }
    glBindVertexArray(vao)
    shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
    glDrawElements(GL_TRIANGLES, elements, GL_UNSIGNED_INT, 0)
    shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
    glBindVertexArray(0)
    glBindTexture(GL_TEXTURE_2D, 0)
    glActiveTexture(0)
    glUseProgram(0)
}

fun Window.drawUntextured(): GraphicsBuffer.(Int) -> (Unit) = { elements ->
    shader.program.let { program ->
        glUseProgram(program)
        glUniformMatrix4fv(glGetUniformLocation(program, "uiProjection"), false, uiProjection.get(matrix))
        glUniformMatrix4fv(glGetUniformLocation(program, "uiView"), false, uiView.get(matrix))
        glUniformMatrix4fv(glGetUniformLocation(program, "projection"), false, projection.get(matrix))
        glUniformMatrix4fv(glGetUniformLocation(program, "view"), false, view.get(matrix))
    }
    glBindVertexArray(vao)
    shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
    glDrawElements(GL_TRIANGLES, elements, GL_UNSIGNED_INT, 0)
    shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
    glBindVertexArray(0)
    glUseProgram(0)
}


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

fun Window.mouseUi(): Vector2f {
    val mouseX = BufferUtils.createDoubleBuffer(1)
    val mouseY = BufferUtils.createDoubleBuffer(1)
    val width = BufferUtils.createIntBuffer(1)
    val height = BufferUtils.createIntBuffer(1)
    glfwGetCursorPos(window, mouseX, mouseY)
    glfwGetWindowSize(window, width, height)
    return Vector2f(
        (mouseX.get().toFloat() - 640f) / (1280 / 80f),
        (height.get().toFloat() - mouseY.get().toFloat() - 360f) / (720f / 45f)
    )
}

suspend fun main() = window {
    glEnable(GL_DEPTH_TEST)
    glDepthFunc(GL_LESS)
    val drawTextured = drawTextured()
    camera.set(1f, 1f)
    move()
    val map = Maps[0]
    val collider = Collider(camera, TILE_DIM)
    val delta60 = fixed(60)
    delta60 { _ ->
        val cameraTile = camera.round(Vector2f()).int()
        val nearbyTileColliders = (0 until COLLISION_DIM * COLLISION_DIM).mapNotNull {
            val tile = Vector2i(it % COLLISION_DIM, it / COLLISION_DIM).add(cameraTile).sub(COLLISION_RAD, COLLISION_RAD)
            if (tile.x < 0 || tile.x >= map.worldSize || tile.y < 0 || tile.y >= map.worldSize) null
            else {
                val tileIndex = tile.x + tile.y * map.worldSize
                map.colliders[tileIndex]
            }
        }
        val motion = Vector2f(0f, 0f).apply {
            if (keyState[GLFW_KEY_W]) y += 1
            if (keyState[GLFW_KEY_S]) y -= 1
            if (keyState[GLFW_KEY_A]) x -= 1
            if (keyState[GLFW_KEY_D]) x += 1
        }.normal().mul(0.135f)
        camera.add(collider.move(motion, nearbyTileColliders))
        move()
    }
    var fps = 0
    val delta10 = fixed(10)
    delta10 { _ ->
        FPS.apply {
            clear()
            fntQuad {
                centerX = -20f; centerY = 20f
                text = "fps: ${"%4s".format(fps.toString())}"
//                println("length: " + "fps: ${"%4s".format((1f / delta).toInt().toString())}".trim { it == ' ' }.length * TEXTURE_SHADER.quad)
                ui = true
                point = 0.3f
                z = 10
            }
        }
    }
//    delta10 { _ ->
//        AUDIO.submit(SHOOT)
//    }
    delta { delta ->
        fps = (1f / delta).toInt()
        PLAYER.apply {
            clear()
            texQuad {
                pos(camera)
                dim(TILE_DIM)
                uvPos(COUNTER_PLAYER)
                uvDim(TILE_UV_DIM)
                z = 3
            }
        }
        WORLD.apply {
            clear()
            for (index in 0 until RENDER_DIM * RENDER_DIM) {
                val cameraTile = camera.round(Vector2f()).int()
                val tile = Vector2i(index % RENDER_DIM, index / RENDER_DIM).add(cameraTile).sub(RENDER_RAD, RENDER_RAD)
                if (tile.x < 0 || tile.x >= map.worldSize || tile.y < 0 || tile.y >= map.worldSize) continue
                val tileIndex = tile.let { it.x + it.y * map.worldSize }
                texQuad {
                    pos(tile.float())
                    dim(TILE_DIM)
                    uvPos(TILES[map.world[tileIndex].toInt()])
                    uvDim(TILE_UV_DIM)
                    z = if (map.world[tileIndex] in SOLIDS) 2 else 1
                }
            }
        }
    }

    var fovIndex = 0
    val fovResults = Array(4096) { TiledRay() }
    delta60 { _ ->
        val mouseWorld = mouseWorld()
        fovIndex = 0
        map.corners.sortBy {
            if (mouseWorld.x > camera.x) atan2(it.y - camera.y, it.x - camera.x)
            else atan2(camera.y - it.y, camera.x - it.x)
        }
        val mouse = mouseWorld()
        val first = (atan2(mouse.y - camera.y, mouse.x - camera.x) - toRadians(41.0).toFloat()).let { Vector2f(cos(it), sin(it)) }
        val last = (atan2(mouse.y - camera.y, mouse.x - camera.x) + toRadians(41.0).toFloat()).let { Vector2f(cos(it), sin(it)) }
        map.world.tiledRaycast(map.worldSize, camera, first, fovResults[fovIndex++])
        map.corners.forEach {
            val cornerDist = camera.distance(it)
            val mouseDist = camera.distance(mouseWorld)
            val normalizedLook =
                Vector2f((mouseWorld.x - camera.x) / mouseDist, (mouseWorld.y - camera.y) / mouseDist)
            val normalizedCorner = Vector2f((it.x - camera.x) / cornerDist, (it.y - camera.y) / cornerDist)
            val dot = normalizedLook.dot(normalizedCorner)
            if (dot >= 0.75) {
                val theta = atan2(camera.y - it.y, camera.x - it.x) + PI.toFloat()
                val middle = Vector2f(cos(theta), sin(theta))
                map.apply {
                    world.tiledRaycast(
                        worldSize,
                        camera,
                        Vector2f(cos(theta - 0.00001f), sin(theta - 0.00001f)),
                        fovResults[fovIndex++]
                    )
                    world.tiledRaycast(worldSize, camera, middle, fovResults[fovIndex++])
                    world.tiledRaycast(
                        worldSize,
                        camera,
                        Vector2f(cos(theta + 0.00001f), sin(theta + 0.00001f)),
                        fovResults[fovIndex++]
                    )
                }
            }
        }
        map.world.tiledRaycast(map.worldSize, camera, last, fovResults[fovIndex++])
//        map.world.tiledRaycast(map.worldSize, camera, last, fovResults[fovIndex++])
        FOV.apply {
            clear()
            (1 until fovIndex).forEach {
                val previous = fovResults[it - 1]
                val current = fovResults[it]
                texTri {
                    c(camera, z = 3)
                    b(previous.hit, z = 3)
                    a(current.hit, z = 3)
                    r = 0; g = 0; b = 0; a = 255
                    fill = true
                }
            }
        }
        FOG.apply {
            clear()
            texQuad {
                x = map.worldSize / 2f - 0.5f
                y = map.worldSize / 2f - 0.5f
                dimX = map.worldSize.toFloat()
                dimY = map.worldSize.toFloat()
                r = 0; g = 0; b = 0; a = 32
                z = 2
                fill = true
            }
        }
    }
    delta {
        println(1f / it)
        glClearColor(236f / 350f, 247f / 350f, 252f / 350f, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)

        TEXTURE_BUFFER.draw(drawTextured, WORLD, FPS, PLAYER)

        glStencilMask(0xFF)
        glStencilFunc(GL_ALWAYS, 1, 0xFF)
        glColorMask(false, false, false, false)
        TEXTURE_BUFFER.draw(drawTextured, FOV)
        glStencilMask(0x00)
        glStencilFunc(GL_EQUAL, 1, 0xFF)
        glColorMask(true, true, true, true)
        //clipped

        glStencilFunc(GL_NOTEQUAL, 1, 0xFF)
        TEXTURE_BUFFER.draw(drawTextured, FOG)
        glStencilMask(0xFF)
        glStencilFunc(GL_ALWAYS, 0, 0xFF)

        glfwSwapBuffers(window)
        glfwPollEvents()
    }
}