//package me.mason.client.testing
//
//import me.mason.client.Window
//import me.mason.client.clear
//import me.mason.client.texturedQuad
//import me.mason.shared.*
//import org.joml.Vector2f
//import org.joml.Vector2i
//import org.lwjgl.glfw.GLFW.*
//
//val PLAYER by lazy { UNCLIPPED.aggregator(TEXTURED_SHADER.quad) }
//
//val COUNTER_PLAYER = Vector2i(0, 500)
//val TERRORIST_PLAYER = Vector2i(4, 500)
//val DEAD_PLAYER = Vector2i(8, 500)
//
//const val COLLISION_DIM = 4
//const val COLLISION_RAD = COLLISION_DIM / 2
//
//suspend fun Window.PlayerEntity(map: TileMap) {
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
//            texturedQuad(GAME_ATLAS, camera, TILE_DIM, COUNTER_PLAYER, TILE_UV_DIM)
//        }
//    }
//}