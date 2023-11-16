package me.mason.client.component

import me.mason.client.TEXTURE_BUFFER
import me.mason.client.TEXTURE_SHADER
import me.mason.client.engine.Cursor
import me.mason.client.engine.Window
import me.mason.shared.*
import org.joml.Vector2f
import org.joml.Vector2i

val WORLD by lazy { Cursor(RENDER_DIM * RENDER_DIM * TEXTURE_SHADER.quad, TEXTURE_BUFFER) }

suspend fun Window.World(map: TileMap) {
    onFixed(1) { _, _ ->
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
}