package me.mason.client.component

import me.mason.client.*
import me.mason.client.engine.Cursor
import me.mason.client.engine.Window
import me.mason.shared.*
import org.joml.Vector2f
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW

val COUNTER_PLAYER = Vector2i(0, 115)

val PLAYER by lazy { Cursor(TEXTURE_SHADER.quad, TEXTURE_BUFFER) }

suspend fun Window.Player(map: TileMap, collider: Collider) {
    onFixed(60) { _, _ ->
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
            if (keyState[GLFW.GLFW_KEY_W]) y += 1
            if (keyState[GLFW.GLFW_KEY_S]) y -= 1
            if (keyState[GLFW.GLFW_KEY_A]) x -= 1
            if (keyState[GLFW.GLFW_KEY_D]) x += 1
        }.normal().mul(0.135f)
        camera.add(collider.move(motion, nearbyTileColliders))
        move()
    }
    onTick { delta, elapsed ->
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
    }
}