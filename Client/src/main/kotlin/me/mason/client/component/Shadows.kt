package me.mason.client.component

import me.mason.client.TEXTURE_BUFFER
import me.mason.client.TEXTURE_SHADER
import me.mason.client.engine.Cursor
import me.mason.client.engine.Window
import me.mason.client.mouseWorld
import me.mason.shared.TileMap
import me.mason.shared.TiledRay
import me.mason.shared.tiledRaycast
import org.joml.Vector2f
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

val FOG by lazy { Cursor(TEXTURE_SHADER.quad, TEXTURE_BUFFER) }
val VIEW by lazy { Cursor(TEXTURE_SHADER.tri * 68000, TEXTURE_BUFFER) }

suspend fun Window.Shadows(map: TileMap) {
    var fovIndex: Int
    val fovResults = Array(4096) { TiledRay() }
    onFixed(60) { _, _ ->
        val mouseWorld = mouseWorld()
        fovIndex = 0
        map.corners.sortBy {
            if (mouseWorld.x > camera.x) atan2(it.y - camera.y, it.x - camera.x)
            else atan2(camera.y - it.y, camera.x - it.x)
        }
        val mouse = mouseWorld()
        val first = (atan2(mouse.y - camera.y, mouse.x - camera.x) - Math.toRadians(41.0)
            .toFloat()).let { Vector2f(cos(it), sin(it)) }
        val last = (atan2(mouse.y - camera.y, mouse.x - camera.x) + Math.toRadians(41.0)
            .toFloat()).let { Vector2f(cos(it), sin(it)) }
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
        VIEW.apply {
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
}