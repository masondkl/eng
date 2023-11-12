package me.mason.client.legacy//package me.mason.client
//
//import me.mason.shared.*
//import org.joml.Vector2f
//import org.joml.Vector2i
//import org.joml.Vector4f
//import org.lwjgl.BufferUtils
//import org.lwjgl.glfw.GLFW
//import org.lwjgl.opengl.GL30.*
//import java.nio.ByteBuffer
//import java.nio.file.Paths
//import java.util.ArrayList
//import java.util.HashSet
//import kotlin.io.path.exists
//import kotlin.io.path.readBytes
//import kotlin.io.path.writeBytes
//import kotlin.math.max
//import kotlin.math.min
//
//val WALL_COLORS = arrayOf(
//    Vector4f(1f, 0f, 0f, 0.33f),
//    Vector4f(1f, 1f, 0f, 0.33f),
//    Vector4f(0f, 1f, 0f, 0.33f),
//    Vector4f(0f, 1f, 1f, 0.33f),
//    Vector4f(0f, 0f, 1f, 0.33f)
//)
//val BOMB_PLANT_COLOR = Vector4f(1f, 1f, 1f, 0.33f)
//
//suspend fun editor(fileName: String, inWorldSize: Int) = window {
//    val atlas = atlas(Paths.get("shooter_flipped.png"))
//    val colorShader = Shader(Paths.get("color.vert"), Paths.get("color.frag"), 3, 4)
//    val textureShader = Shader(Paths.get("texture.vert"), Paths.get("texture.frag"), 3, 1)
//    val matrix = BufferUtils.createFloatBuffer(16)
//    val colorBuffer = ShaderBuffer(colorShader, 200000) { start, count ->
//        glBindBuffer(GL_ARRAY_BUFFER, vbo)
//        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
//        shader.program.let {
//            glUseProgram(it)
//            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, projection.get(matrix))
//            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, view.get(matrix))
//        }
//        glBindVertexArray(vao)
//        shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
//        glDrawArrays(GL_TRIANGLES, start / shader.stride, count / shader.stride)
//        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
//        glBindVertexArray(0)
//        glUseProgram(0)
//    }
//    val textureBuffer = ShaderBuffer(textureShader, 200000) { start, count ->
//        glBindBuffer(GL_ARRAY_BUFFER, vbo)
//        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
//        shader.program.let {
//            glUseProgram(it)
//            glUniform1i(glGetUniformLocation(it, "TEX_SAMPLER"), 0)
//            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, projection.get(matrix))
//            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, view.get(matrix))
//        }
//        glBindTexture(GL_TEXTURE_2D, atlas)
//        glActiveTexture(0)
//        glBindVertexArray(vao)
//        shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
//        glDrawArrays(GL_TRIANGLES, start / shader.stride, count / shader.stride)
//        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
//        glBindVertexArray(0)
//        glBindTexture(GL_TEXTURE_2D, 0)
//        glUseProgram(0)
//    }
//    glEnable(GL_DEPTH_TEST)
//    glDepthFunc(GL_LESS)
//    glEnable(GL_STENCIL_TEST)
//    glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
//
//    var worldSize = inWorldSize
//    val counterSpawns = HashSet<Vector2f>()
//    val terroristSpawns = HashSet<Vector2f>()
//    val plantBounds = HashSet<Bounds>()
//    var world = ByteArray(worldSize * worldSize) { 6.toByte() }
//    var wallColor = 0
//    var walls = ArrayList<Bounds>()
//    fun nextWallColor(): Vector4f {
//        val color = WALL_COLORS[wallColor]
//        wallColor = (wallColor + 1) % WALL_COLORS.size
//        return color
//    }
//    var selected = 0
//    val path = Paths.get(fileName)
//    if (path.exists()) {
//        ByteBuffer.wrap(Paths.get(fileName).readBytes()).run {
//            worldSize = int
//            world = ByteArray(worldSize * worldSize); get(world)
//            walls = ArrayList()
//            for (i in 0 until int) walls.add(Bounds(Vector2f(float, float), Vector2f(float, float)))
//            for (i in 0 until int) counterSpawns.add(Vector2f(float, float))
//            for (i in 0 until int) terroristSpawns.add(Vector2f(float, float))
//            for (i in 0 until int) plantBounds.add(Bounds(Vector2f(float, float), Vector2f(float, float)))
//        }
//    }
//    GLFW.glfwSetScrollCallback(window) { _, x, y ->
//        selected = if (y > 0) min(selected + 1, TILES.size - 1)
//        else max(selected - 1, 0)
//    }
//    keyEvent += press@{ code, action ->
//        if (code != GLFW.GLFW_KEY_S || action != GLFW.GLFW_PRESS || !keyState[GLFW.GLFW_KEY_LEFT_SHIFT]) return@press
//        val sizeBytes = 20
//        val worldBytes = worldSize * worldSize
//        val spawnBytes = (counterSpawns.size + terroristSpawns.size) * 8
//        val wallBytes = walls.size * 16
//        val plantBytes = plantBounds.size * 16
//        val buffer = ByteBuffer.allocate(sizeBytes + worldBytes + spawnBytes + wallBytes + plantBytes).apply {
//            putInt(worldSize)
//            put(world)
//            putInt(walls.size)
//            walls.forEach { wall ->
//                putFloat(wall.min.x); putFloat(wall.min.y)
//                putFloat(wall.max.x); putFloat(wall.max.y)
//            }
//            putInt(counterSpawns.size)
//            counterSpawns.forEach {
//                putFloat(it.x)
//                putFloat(it.y)
//            }
//            putInt(terroristSpawns.size)
//            terroristSpawns.forEach {
//                putFloat(it.x)
//                putFloat(it.y)
//            }
//            putInt(plantBounds.size)
//            plantBounds.forEach { bounds ->
//                putFloat(bounds.min.x); putFloat(bounds.min.y)
//                putFloat(bounds.max.x); putFloat(bounds.max.y)
//            }
//        }
//        println("Saved")
//        Paths.get(fileName).writeBytes(buffer.array())
//    }
//    mouseEvent += right@{ code, action ->
//        if (code != GLFW.GLFW_MOUSE_BUTTON_2 || action != GLFW.GLFW_PRESS || keyState[GLFW.GLFW_KEY_LEFT_CONTROL]) return@right
//        val tile = mouseWorld().round(Vector2f()).int()
//        if (tile.x < 0 || tile.x >= worldSize || tile.y < 0 || tile.y >= worldSize) return@right
//        val index = tile.x + tile.y * worldSize
//        val spawn = Vector2f((index % worldSize).toFloat(), (index / worldSize).toFloat())
//        if (counterSpawns.contains(spawn)) {
//            terroristSpawns.add(spawn)
//            counterSpawns.remove(spawn)
//        } else if (terroristSpawns.contains(spawn)) terroristSpawns.remove(spawn)
//        else counterSpawns.add(spawn)
//    }
//    val tempWall = Bounds()
//    mouseEvent += right@{ code, action ->
//        if (code != GLFW.GLFW_MOUSE_BUTTON_1 || !keyState[GLFW.GLFW_KEY_LEFT_CONTROL]) return@right
//        val mouse = mouseWorld()
//        var toRemove = -1
//        for (i in walls.indices) {
//            val wall = walls[i]
//            if (
//                wall.min.x > mouse.x || wall.max.x < mouse.x ||
//                wall.min.y > mouse.y || wall.max.y < mouse.y
//            ) continue
//            toRemove = i
//            break
//        }
//        if (toRemove != -1) {
//            walls.removeAt(toRemove)
//        }
//        return@right
//    }
//    mouseEvent += right@{ code, action ->
//        if (code != GLFW.GLFW_MOUSE_BUTTON_2 || !keyState[GLFW.GLFW_KEY_LEFT_CONTROL]) return@right
//        val mouse = mouseWorld()
//        val tile = mouse.round(Vector2f())
//        if (action == GLFW.GLFW_PRESS) {
//            tempWall.min.set(tile)
//        } else if (action == GLFW.GLFW_RELEASE) {
//            tempWall.max.set(tile)
//            val min = tempWall.min.min(tempWall.max, Vector2f()).sub(TILE_RADIUS)
//            val max = tempWall.min.max(tempWall.max, Vector2f()).add(TILE_RADIUS)
//            tempWall.min.set(min)
//            tempWall.max.set(max)
//            walls.add(Bounds(tempWall.min.copy(), tempWall.max.copy()))
//        }
//    }
//    val tempPlantBounds = Bounds()
//    mouseEvent += right@{ code, action ->
//        if (code != GLFW.GLFW_MOUSE_BUTTON_2 || !keyState[GLFW.GLFW_KEY_LEFT_SHIFT]) return@right
//        val mouse = mouseWorld()
//        val tile = mouse.round(Vector2f())
//        if (action == GLFW.GLFW_PRESS) {
//            tempPlantBounds.min.set(tile)
//        } else if (action == GLFW.GLFW_RELEASE) {
//            tempPlantBounds.max.set(tile)
//            val min = tempPlantBounds.min.min(tempPlantBounds.max, Vector2f()).sub(TILE_RADIUS)
//            val max = tempPlantBounds.min.max(tempPlantBounds.max, Vector2f()).add(TILE_RADIUS)
//            tempPlantBounds.min.set(min)
//            tempPlantBounds.max.set(max)
//            plantBounds.add(Bounds(tempPlantBounds.min.copy(), tempPlantBounds.max.copy()))
//        }
//    }
//    val topLeft = Vector2f(-38f, 20.5f)
//    onFixed(144) { _, _ ->
//        val motion = Vector2f(0f, 0f).apply {
//            if (keyState[GLFW.GLFW_KEY_W]) y += 1
//            if (keyState[GLFW.GLFW_KEY_S]) y -= 1
//            if (keyState[GLFW.GLFW_KEY_A]) x -= 1
//            if (keyState[GLFW.GLFW_KEY_D]) x += 1
//        }.normal().mul(0.125f)
//        camera.add(motion)
//        move()
//        if (mouseState[GLFW.GLFW_MOUSE_BUTTON_1] && !keyState[GLFW.GLFW_KEY_LEFT_CONTROL]) {
//            val mouse = mouseWorld()
//            val tile = mouse.round(Vector2f()).int()
//            val index = tile.x + tile.y * worldSize
//            if (tile.x < 0 || tile.x >= worldSize || tile.y < 0 || tile.y >= worldSize) return@onFixed
//            world[index] = selected.toByte()
//        }
//    }
//    val wallEntity = colorBuffer.aggregator(200000)
//    onTick { _, _ ->
//        wallEntity.clear()
//        wallColor = 0
//        walls.forEach { wall ->
//            val min = wall.min
//            val max = wall.max
//            val dim = max.sub(min, Vector2f())
//            val rad = Vector2f(dim.x / 2f, dim.y / 2f)
//            wallEntity.colorQuad(
//                rad.add(min),
//                dim,
//                nextWallColor(),
//                z = 10
//            )
//        }
//        plantBounds.forEach { bounds ->
//            val min = bounds.min
//            val max = bounds.max
//            val dim = max.sub(min, Vector2f())
//            val rad = Vector2f(dim.x / 2f, dim.y / 2f)
//            wallEntity.colorQuad(
//                rad.add(min),
//                dim,
//                BOMB_PLANT_COLOR,
//                z = 10
//            )
//        }
//    }
//    val editorEntity = textureBuffer.aggregator(200000)
//    onTick { _, _ ->
//        editorEntity.clear()
//        TILES.forEachIndexed { idx, it ->
//            editorEntity.textureQuad(
//                Vector2f(TILE_SIZE.x * idx.toFloat(), 0f).add(topLeft).add(camera), TILE_SIZE,
//                it, TILE_UV_SIZE, z = 1
//            )
//        }
//        editorEntity.textureQuad(
//            Vector2f(TILE_SIZE.x * selected.toFloat(), 0f).add(topLeft).add(camera), TILE_SIZE,
//            SELECTED, TILE_UV_SIZE, z = 2
//        )
//        editorEntity.textureQuad(camera, TILE_SIZE, COUNTER_PLAYER, PLAYER_UV_DIM, z = 2)
//        for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
//            val cameraTile = camera.round(Vector2f()).int()
//            val tile = Vector2i(index % RENDER_SIZE, index / RENDER_SIZE).add(cameraTile).sub(RENDER_RADIUS, RENDER_RADIUS)
//            if (tile.x < 0 || tile.x >= worldSize || tile.y < 0 || tile.y >= worldSize) continue
//            val tileIndex = tile.let { it.x + it.y * worldSize }
//            val tileFloat = tile.float()
//            editorEntity.textureQuad(tileFloat, TILE_SIZE, TILES[world[tileIndex].toInt()], TILE_UV_SIZE)
//            if (counterSpawns.contains(tileFloat)) {
//                editorEntity.textureQuad(tileFloat, TILE_SIZE, RED_SPAWN_MARKER, TILE_UV_SIZE, z = 2)
//            } else if (terroristSpawns.contains(tileFloat)) {
//                editorEntity.textureQuad(tileFloat, TILE_SIZE, BLUE_SPAWN_MARKER, TILE_UV_SIZE, z = 2)
//            }
//        }
//    }
//    onTick { _, _ ->
//        glClearColor(0f, 0f, 0f, 1f)
//        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
//
//        editorEntity.draw()
//        wallEntity.draw()
//
//        GLFW.glfwSwapBuffers(window)
//        GLFW.glfwPollEvents()
//    }
//}