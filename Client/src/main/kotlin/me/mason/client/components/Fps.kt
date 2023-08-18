package me.mason.client.components

import me.mason.client.*
import me.mason.shared.timeMillis
import org.joml.Vector2f

context(Window)
suspend fun Reserve.fps(font: Font) = reserve(shader.quad) {
    var lastFps = timeMillis
    var fps = 0
    onTick { delta, elapsed ->
        if (timeMillis - lastFps > 1000 / 10f) {
            lastFps = timeMillis
            fps = (1f/delta).toInt()
        }
        clear()
        fontQuads(font, Vector2f(-37.5f, 15f).add(camera), "FPS: $fps", z = 10)
    }
}