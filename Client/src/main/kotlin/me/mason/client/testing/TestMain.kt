//package me.mason.client.testing
//
//import me.mason.client.*
//import me.mason.shared.*
//import org.joml.Vector2f
//import org.joml.Vector2i
//import org.lwjgl.glfw.GLFW.*
//import org.lwjgl.opengl.GL11.*
//import java.nio.file.Paths
//
//val GAME_ATLAS by lazy { atlas(Paths.get("shooter_flipped.png")) }
//val FONT_ATLAS by lazy { atlas(Paths.get("vt323.png")) }
//val ATLASES by lazy { intArrayOf(GAME_ATLAS, FONT_ATLAS) }
//
//val FAST_QUAD_SHADER by lazy { Shader(Paths.get("fast_quad.vert"), Paths.get("fast_quad.frag"), 3, 1) }
//val FAST_QUAD_BUFFER by lazy { ShaderBuffer(FAST_QUAD_SHADER, 128000, fastQuads = true) }
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
//
//suspend fun main() = window {
//    title = "title"
//    dim = Vector2i(1280, 720)
////    glEnable(GL_DEPTH_TEST)
////    glDepthFunc(GL_LESS)
////    glEnable(GL_STENCIL_TEST)
////    glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
//    println("FAST QUAD SHADER PROGRAM: ${FAST_QUAD_SHADER.program}")
//    val drawFastGameQuadTextured = drawFastQuadTextured(false, GAME_ATLAS)
//    val drawTextured = drawTextured(false, *ATLASES)
//    val drawTexturedUi = drawTextured(true, *ATLASES)
//    val draw = draw(false)
//    val drawUi = draw(false)
//    val map = emptyTileMap()
//    println(RENDER_SIZE * RENDER_SIZE * FAST_QUAD_SHADER.fastQuad)
//    val collider = Collider(camera, TILE_DIM)
//    onFixed(60) { _, _ ->
//        val cameraTile = camera.round(Vector2f()).int()
//        val nearbyTileColliders = (0 until COLLISION_DIM * COLLISION_DIM).mapNotNull {
//            val tile = Vector2i(it % COLLISION_DIM, it / COLLISION_DIM).add(cameraTile).sub(COLLISION_RAD, COLLISION_RAD)
//            if (tile.x < 0 || tile.x >= map.worldSize || tile.y < 0 || tile.y >= map.worldSize) null
//            else {
//                val tileIndex = tile.x + tile.y * map.worldSize
//                map.colliders[tileIndex]
//            }
//        }
//        val motion = Vector2f(0f, 0f).apply {
//            if (keyState[GLFW_KEY_W]) y += 1
//            if (keyState[GLFW_KEY_S]) y -= 1
//            if (keyState[GLFW_KEY_A]) x -= 1
//            if (keyState[GLFW_KEY_D]) x += 1
//        }.normal().mul(0.135f)
//        camera.add(collider.move(motion, nearbyTileColliders))
//        move()
//        println("${camera.x}, ${camera.y}")
//        PLAYER.apply {
//            clear()
//            texturedQuad(GAME_ATLAS, camera, TILE_DIM, COUNTER_PLAYER, TILE_UV_DIM, z = 1)
//        }
//    }
//    WORLD.apply {
//        onFixed(360) { _, _ ->
//            clear()
//            for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
//                val cameraTile = camera.round(Vector2f()).int()
//                val tile =
//                    Vector2i(index % RENDER_SIZE, index / RENDER_SIZE).add(cameraTile).sub(RENDER_RADIUS, RENDER_RADIUS)
//                if (tile.x < 0 || tile.x >= map.worldSize || tile.y < 0 || tile.y >= map.worldSize) continue
//                val tileIndex = tile.let { it.x + it.y * map.worldSize }
//                fastTexturedQuad(
//                    tile.float(), TILE_DIM,
//                    TILES[map.world[tileIndex].toInt()], TILE_UV_DIM,
//                    z = if (map.world[tileIndex] in SOLIDS) 3 else 1
//                )
//            }
//        }
//    }
//    onTick { _, _ ->
////        println("${1f / delta}")
//        glClearColor(236f / 255f, 247f / 255f, 252f / 255f, 1f)
//        glClear(GL_COLOR_BUFFER_BIT
////                or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT
//        )
//
//        println("=============")
//        println(WORLD.pos)
//        println(WORLD.len)
////        TEXTURED_UI.draw(drawTexturedUi)
////        COLORED_UI.draw(drawUi)
//        UNCLIPPED.draw(drawTextured)
//        WORLD.draw(drawFastGameQuadTextured)
////        glStencilFunc(GL_ALWAYS, 1, 0xFF)
////        glStencilMask(0xFF)
////        glColorMask(false, false, false, false)
////        //fov
////        glStencilMask(0x00)
////        glStencilFunc(GL_EQUAL, 1, 0xFF)
////        glColorMask(true, true, true, true)
////        CLIPPED.draw(drawTextured)
////        glStencilFunc(GL_NOTEQUAL, 1, 0xFF)
////        FOG.draw(draw)
////        glStencilMask(0xFF)
////        glStencilFunc(GL_ALWAYS, 0, 0xFF)
//
//        glfwSwapBuffers(window)
//        glfwPollEvents()
//    }
//}