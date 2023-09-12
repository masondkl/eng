package me.mason.client

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mason.shared.*
import me.mason.sockets.Connection
import me.mason.sockets.connect
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import java.lang.Math.toRadians
import java.nio.file.Paths
import java.util.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


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

val SETTINGS_ICON = Vector2i(54, 489)
val SETTINGS_ICON_UV_SIZE = Vector2i(18, 18)
val SETTINGS_ICON_SIZE = Vector2f(4.5f, 4.5f)


val WINDOW = Vector2i(26, 496)
val WINDOW_DIM = Vector2i(9, 9)
val WINDOW_UV_INSETS = Vector2i(4, 4)
val WINDOW_INSETS = Vector2f(1f)


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

suspend fun client() = window {
    title = "title"
    dim = Vector2i(1280, 720)
    var settings = false
    val font = font(Paths.get("vt323.fnt"))
    val fontAtlas = atlas(Paths.get("vt323.png"))
    val atlas = atlas(Paths.get("shooter_flipped.png"))
    val textureShader = Shader(Paths.get("texture.vert"), Paths.get("texture.frag"), 3, 1)
    val fontShader = Shader(Paths.get("font.vert"), Paths.get("font.frag"), 3, 1, 3)
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
        glDrawArrays(GL_TRIANGLES, start / shader.stride, count / shader.stride)
        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glUseProgram(0)
    }
    val uiBuffer = ShaderBuffer(textureShader, size = 64000) { start, count ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        shader.program.let {
            glUseProgram(it)
            glUniform1i(glGetUniformLocation(it, "TEX_SAMPLER"), 0)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, uiProjection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, uiView.get(matrix))
        }
        glBindTexture(GL_TEXTURE_2D, atlas)
        glActiveTexture(0)
        glBindVertexArray(vao)
        shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, start / shader.stride, count / shader.stride)
        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glUseProgram(0)
    }
    val uiColorBuffer = ShaderBuffer(colorShader, size = 64000) { start, count ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        shader.program.let {
            glUseProgram(it)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, uiProjection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, uiView.get(matrix))
        }
        glBindVertexArray(vao)
        shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, start / shader.stride, count / shader.stride)
        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glUseProgram(0)
    }
    val fontBuffer = ShaderBuffer(fontShader, size = 64000) { start, count ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        shader.program.let {
            glUseProgram(it)
            glUniform1i(glGetUniformLocation(it, "TEX_SAMPLER"), 0)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, uiProjection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, uiView.get(matrix))
        }
        glBindTexture(GL_TEXTURE_2D, fontAtlas)
        glActiveTexture(0)
        glBindVertexArray(vao)
        shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, start / shader.stride, count / shader.stride)
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
        glDrawArrays(GL_TRIANGLES, start / shader.stride, count / shader.stride)
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
        glDrawArrays(GL_TRIANGLES, start / shader.stride, count / shader.stride)
        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glUseProgram(0)
    }
    glEnable(GL_DEPTH_TEST)
    glDepthFunc(GL_LESS)
    glEnable(GL_STENCIL_TEST)
    glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
    camera.set(1f, 1f)
    move()
    val matchState: ClientMatchState = object : ClientMatchState {
        val lock = Mutex()
        override var id = -1
        override var connection: Connection? = null
        override var map: TileMap? = null
        override val bullets = BitSet(256)
        override val bulletStates = Array(256) { Bullet(Vector2f(), Vector2f(), Vector2f(), 0f) }
        override val query = Query()
        override var mode = 0
        override val players = BitSet()
        override val playerStates: Array<PlayerState> = Array(256) {
            object : PlayerState {
                val lerpPos = Vector2f(1.0f)
                override var id = -1
                override var timePos = 0L
                override val lastPos = Vector2f(1f)
                override val nextPos = Vector2f(1f)
                override val pos: Vector2f get() {
                    val t = ((timeMillis - timePos) / LERP_POS_RATE)
                    return lerpPos.set(
                        lastPos.x + (nextPos.x - lastPos.x) * t,
                        lastPos.y + (nextPos.y - lastPos.y) * t,
                    )
                }
                override var health = 1f
                override var alive = true
                override var planting = false
                override var defusing = false
                override var terrorist = false
                override suspend fun send(block: suspend Connection.() -> Unit) { }
            }
        }
        override suspend fun send(block: suspend Connection.() -> Unit) {
            connection?.apply { lock.withLock { block() } }
        }
    }
    val connection = matchState.run { connect("parkourmate.com", 9999) {
        connection = this
        connect()
    } }
    val collider = Collider(Vector2f(), TILE_SIZE)
    fun collider(id: Int): Collider =
        collider.apply { pos.set(if (matchState.id != id) matchState.playerStates[id].pos else camera) }
    mouseEvent += press@{ code, action ->
        if (matchState.id == -1 || settings) return@press
        val playerState = matchState.playerStates[matchState.id]
        if (code != GLFW_MOUSE_BUTTON_1 || action != GLFW_PRESS || !playerState.alive || matchState.map == null) return@press
        val mouseWorld = mouseWorld()
        val theta = atan2(camera.y - mouseWorld.y, camera.x - mouseWorld.x) + PI.toFloat()
        val dir = Vector2f(cos(theta), sin(theta))
        matchState.apply {
            val nextBullet = bullets.nextClearBit(0)
            bulletStates[nextBullet].also {
                it.start.set(camera)
                it.pos.set(camera.copy())
                it.dir.set(dir)
                it.tileHit = map!!.world.tiledRaycast(map!!.worldSize, camera, dir, TiledRay()).distance
            }; bullets.set(nextBullet)
            send {
                int(OUT_SHOOT)
                vec2f(dir)
            }
        }
    }
    keyEvent += press@ { code, action ->
        if (code != GLFW_KEY_ESCAPE || action != GLFW_PRESS) return@press
        settings = !settings
    }
    keyEvent += press@ { code, action ->
        if (code != GLFW_KEY_GRAVE_ACCENT || action != GLFW_PRESS) return@press
        matchState.send {
            int(OUT_CALL_VOTE)
            int(VOTE_MAP)
        }
    }
    val keyIds = GLFW_KEY_1..GLFW_KEY_9
    keyEvent += press@ { code, action ->
        if (code !in keyIds || action != GLFW_PRESS) return@press
        if (matchState.query.type != -1) {
            matchState.respondQuery(matchState.query.type, keyIds.indexOf(code))
            matchState.query.type = -1
        }
    }
    var fovTriangles: Int
    val fovResults = Array(4096) { TiledRay() }
    onClosed { connection?.close() }
    onFixed(20) { _, _ ->
        matchState.send {
            int(OUT_POS)
            vec2f(camera)
        }
    }
    onFixed(60) { _, elapsed ->
        if (matchState.id == -1 || settings) return@onFixed
        val playerState = matchState.playerStates[matchState.id]
        if (matchState.map == null || !playerState.alive) return@onFixed
        val cameraTile = camera.round(Vector2f()).int()
        val nearbyTileColliders = (0 until COLLISION_SIZE * COLLISION_SIZE).mapNotNull {
            val tile = Vector2i(it % COLLISION_SIZE, it / COLLISION_SIZE).add(cameraTile).sub(COLLISION_RADIUS, COLLISION_RADIUS)
            if (tile.x < 0 || tile.x >= matchState.map!!.worldSize || tile.y < 0 || tile.y >= matchState.map!!.worldSize) null
            else {
                val tileIndex = tile.x + tile.y * matchState.map!!.worldSize
                matchState.map!!.colliders[tileIndex]
            }
        }
        val motion = Vector2f(0f, 0f).apply {
            if (keyState[GLFW_KEY_W]) y += 1
            if (keyState[GLFW_KEY_S]) y -= 1
            if (keyState[GLFW_KEY_A]) x -= 1
            if (keyState[GLFW_KEY_D]) x += 1
        }.normal().mul(0.135f)
        collider(matchState.id).move(motion, nearbyTileColliders)
        camera.add(collider(matchState.id).move(motion, nearbyTileColliders))
        move()
    }
    val fontEntity = fontBuffer.aggregator(64000)
    val uiEntity = uiBuffer.aggregator(64000)
    val uiColorEntity = uiColorBuffer.aggregator(64000)
//    val pos = Vector2f(0f, 0f)
//    val dim = Vector2f(10f, 1.5f)
//    val rad = Vector2f(dim.x / 2f, dim.y / 2f)
//    val start = Vector2f(pos).sub(rad)
//    val end = Vector2f(pos).add(rad)
    val shadowFpsSlider = Slider(
        Vector2f(0f, 0f),
        Vector2f(10f, 1.5f),
        5f, 360f, 144f
    )
    val entityFpsSlider = Slider(
        Vector2f(0f, 5f),
        Vector2f(10f, 1.5f),
        5f, 360f, 144f
    )
    val subtextSize = 0.015f

    onTick { delta, elapsed ->
        fontEntity.clear()
        uiEntity.clear()
        uiColorEntity.clear()
        val mouse = mouseUi()
        val shadowLabelPos = Vector2f(shadowFpsSlider.pos).add(0f, shadowFpsSlider.rad.y).add(0f, font.lineHeight(subtextSize) / 2f)
        val entityLabelPos = Vector2f(entityFpsSlider.pos).add(0f, entityFpsSlider.rad.y).add(0f, font.lineHeight(subtextSize) / 2f)
//        println("m ${mouse.x}, ${mouse.y}")
//        println("p ${pos.x}, ${pos.y}")

//        buttonQuads(uiEntity, fontEntity, pos, dim, uiZ = 10)
        if (matchState.query.type != -1) {
            var maxLength = font.textWidth(matchState.query.prompt, size = subtextSize)
            fontEntity.fontQuads(font, Vector2f(-30f, 0f), matchState.query.prompt, Vector3f(0f, 0f, 0f), size = subtextSize, z = 11)
            for ((index, answer) in matchState.query.answers.withIndex()) {
                val answerLabel = "[${index + 1}]"
                val dim = font.textWidth(answerLabel, size = subtextSize) + font.textWidth(answer, size = subtextSize)
                val rad = dim / 2f
                val answerIndexPos = Vector2f(-30f - rad + font.textRadius(answerLabel, size = subtextSize), font.lineHeight(subtextSize) * -1.2f * (index + 1))
                val answerPos = Vector2f(-30f - rad + font.textWidth(answerLabel, size = subtextSize) + font.textRadius(answer, size = subtextSize), font.lineHeight(subtextSize) * -1.2f * (index + 1))
                fontEntity.fontQuads(font, answerIndexPos, answerLabel, Vector3f(0f, 0f, 0f), size = subtextSize, z = 11)
                fontEntity.fontQuads(font, answerPos, answer, Vector3f(0f, 0f, 0f), size = subtextSize, z = 11)
            }
            uiEntity.slicedQuads(Vector2f(-30f, -2f), Vector2f(maxLength + 4f, font.lineHeight(subtextSize) * 1.4f * (matchState.query.answers.size + 1)), WINDOW_INSETS, WINDOW, WINDOW_DIM, WINDOW_UV_INSETS, z = 10)
        }
        if (settings) {
            uiEntity.slicedQuads(Vector2f(0f, 0f), Vector2f(20f, 40f), WINDOW_INSETS, WINDOW, WINDOW_DIM, WINDOW_UV_INSETS, z = 10)
            uiEntity.textureQuad(Vector2f(-8f + 2.25f, 15f), SETTINGS_ICON_SIZE, SETTINGS_ICON, SETTINGS_ICON_UV_SIZE, z = 11)
            fontEntity.fontQuads(font, Vector2f(2.25f, 15f), "Settings", Vector3f(0f, 0f, 0f), z = 11)
            shadowFpsSlider.update(mouse)
            entityFpsSlider.update(mouse)
            fontEntity.fontQuads(font, shadowLabelPos, "Shadow FPS", Vector3f(0f, 0f, 0f), size = subtextSize, z = 11)
            fontEntity.fontQuads(font, entityLabelPos, "Entity FPS", Vector3f(0f, 0f, 0f), size = subtextSize, z = 11)
            sliderQuads(font, uiColorEntity, fontEntity, shadowFpsSlider.pos, shadowFpsSlider.dim, shadowFpsSlider.cursor, 5f, 360f, z = 11)
            sliderQuads(font, uiColorEntity, fontEntity, entityFpsSlider.pos, entityFpsSlider.dim, entityFpsSlider.cursor, 5f, 360f, z = 11)
//            uiEntity.textureQuad(pos, dim, SETTINGS_ICON, SETTINGS_ICON_UV_SIZE, z = 11)
//            fontEntity.fontQuads(font, pos, "Your mother", Vector3f(1f, 1f, 0f), z = 11)

        }
    }
//    button(uiEntity, fontEntity)
    val unclippedEntity = textureBuffer.aggregator(RENDER_SIZE * RENDER_SIZE * textureShader.quad + textureShader.quad + textureShader.quad * 9)
    onTick { delta, elapsed ->
        if (matchState.map == null || matchState.id == -1) return@onTick
        val playerState = matchState.playerStates[matchState.id]
        unclippedEntity.clear()
        //Local Player
        unclippedEntity.textureQuad(
            camera, TILE_SIZE,
            if (!playerState.alive) DEAD_PLAYER else COUNTER_PLAYER, PLAYER_UV_SIZE,
            z = 3
        )
        //World
        for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
            val cameraTile = camera.round(Vector2f()).int()
            val tile = Vector2i(index % RENDER_SIZE, index / RENDER_SIZE).add(cameraTile).sub(RENDER_RADIUS, RENDER_RADIUS)
            if (tile.x < 0 || tile.x >= matchState.map!!.worldSize || tile.y < 0 || tile.y >= matchState.map!!.worldSize) continue
            val tileIndex = tile.let { it.x + it.y * matchState.map!!.worldSize }
            unclippedEntity.textureQuad(
                tile.float(),
                TILE_SIZE,
                TILES[matchState.map!!.world[tileIndex].toInt()],
                TILE_UV_SIZE,
                z = if (matchState.map!!.world[tileIndex] in SOLIDS) 3 else 1
            )
        }
    }
    val clippedEntity = textureBuffer.aggregator(513 * textureShader.quad)
    onFixed({ entityFpsSlider.value.toInt() }) { delta, elapsed ->
        if (matchState.map == null) return@onFixed
        matchState.bullets.clearIf {
            val bullet = matchState.bulletStates[it]
            val shootRay = raycast<Unit>(bullet.pos, bullet.dir.mul(0.33f, Vector2f()), max = 1f) {
                val intersected = matchState.players.first { id ->
                    val player = matchState.playerStates[id]
                    val min = Vector2f(player.pos.x - TILE_RADIUS.x, player.pos.y - TILE_RADIUS.y)
                    val max = Vector2f(player.pos.x + TILE_RADIUS.x, player.pos.y + TILE_RADIUS.y)
                    it.x > min.x && it.x < max.x && it.y > min.y && it.y < max.y
                }
                intersected != -1 && intersected != matchState.id
            }
            if (!shootRay.result || shootRay.distance > bullet.tileHit) bullet.start.distance(bullet.pos) > bullet.tileHit
            else bullet.start.distance(bullet.pos) > shootRay.distance
        }
        clippedEntity.clear()
        //Bullets
        matchState.bullets.forEach {
            val bullet = matchState.bulletStates[it]
            clippedEntity.textureQuad(
                bullet.pos,
                BULLET_SIZE,
                BULLET,
                BULLET_UV_SIZE,
                z = 3
            )
            val scale = (1f/entityFpsSlider.value.toInt()) * 200f
            bullet.pos.add(bullet.dir.x * scale, bullet.dir.y * scale)
        }
        //Players
        matchState.players.forEach {
            if (it == matchState.id || it >= 256) return@forEach
            val player = matchState.playerStates[it]
            clippedEntity.textureQuad(
                player.pos, TILE_SIZE,
                if (!player.alive) DEAD_PLAYER else COUNTER_PLAYER, PLAYER_UV_SIZE,
                z = 3
            )
        }
        //Bomb
//        if (planted) {
//            val frame = floor(elapsed % 4f).toInt()
//            clippedEntity.textureQuad(bomb, TILE_SIZE, BOMB_FRAMES[frame], TILE_UV_SIZE, z = 3)
//            clippedEntity.textureQuad(bomb, DEFUSE_MARK_SIZE, DEFUSE_MARK, DEFUSE_MARK_UV_SIZE, z = 3)
//        }
    }
    val fogEntity = colorBuffer.aggregator(colorShader.quad)
    onTick { delta, elapsed ->
        if (matchState.map == null) return@onTick
        fogEntity.clear()
        fogEntity.colorQuad(
            Vector2f(matchState.map!!.worldSize / 2f - 0.5f, matchState.map!!.worldSize / 2f - 0.5f),
            Vector2f(matchState.map!!.worldSize.toFloat(), matchState.map!!.worldSize.toFloat()),
            Vector4f(0f, 0f, 0f, 0.25f), z = 2
        )
    }
    val fovEntity = fovBuffer.aggregator(68000)
    onFixed({ shadowFpsSlider.value.toInt() }) { delta, elapsed ->
//        println("fps: ${1f/delta}")
        if (matchState.id == -1) return@onFixed
        val playerState = matchState.playerStates[matchState.id]
        if (matchState.map == null || !playerState.alive) return@onFixed
        val mouseWorld = mouseWorld()
        val map = matchState.map!!
        fovTriangles = 0
        map.corners.sortBy {
            if (mouseWorld.x > camera.x) atan2(it.y - camera.y, it.x - camera.x)
            else atan2(camera.y - it.y, camera.x - it.x)
        }
        val mouse = mouseWorld()
        val first = (atan2(mouse.y - camera.y, mouse.x - camera.x) - toRadians(41.0).toFloat()).let { Vector2f(cos(it), sin(it)) }
        val last = (atan2(mouse.y - camera.y, mouse.x - camera.x) + toRadians(41.0).toFloat()).let { Vector2f(cos(it), sin(it)) }
        map.world.tiledRaycast(map.worldSize, camera, first, fovResults[fovTriangles++])
        map.corners.forEach {
            val cornerDist = camera.distance(it)
            val mouseDist = camera.distance(mouseWorld)
            val normalizedLook =
                Vector2f((mouseWorld.x - camera.x) / mouseDist, (mouseWorld.y - camera.y) / mouseDist)
            val normalizedCorner = Vector2f((it.x - camera.x) / cornerDist, (it.y - camera.y) / cornerDist)
            val dot = normalizedLook.dot(normalizedCorner)
            if (cornerDist < RENDER_RADIUS + 10f && dot >= 0.75) {
                val theta = atan2(camera.y - it.y, camera.x - it.x) + PI.toFloat()
                val middle = Vector2f(cos(theta), sin(theta))
                map.apply {
                    world.tiledRaycast(
                        worldSize,
                        camera,
                        Vector2f(cos(theta - 0.00001f), sin(theta - 0.00001f)),
                        fovResults[fovTriangles++]
                    )
                    world.tiledRaycast(worldSize, camera, middle, fovResults[fovTriangles++])
                    world.tiledRaycast(
                        worldSize,
                        camera,
                        Vector2f(cos(theta + 0.00001f), sin(theta + 0.00001f)),
                        fovResults[fovTriangles++]
                    )
                }
            }
        }
        map.world.tiledRaycast(map.worldSize, camera, last, fovResults[fovTriangles++])
        fovEntity.clear()
        (1 until fovTriangles).forEach {
            val previous = fovResults[it - 1]
            val current = fovResults[it]
            fovEntity.fovTriangle(camera, previous.hit, current.hit, z = 2)
        }
    }
    onTick { delta, elapsed ->
        glClearColor(236f / 255f, 247f / 255f, 252f / 255f, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        if (matchState.map != null) {
            fontEntity.draw()
            uiEntity.draw()
            uiColorEntity.draw()
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