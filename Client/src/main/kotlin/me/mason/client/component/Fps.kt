package me.mason.client.component

import me.mason.client.TEXTURE_BUFFER
import me.mason.client.TEXTURE_SHADER
import me.mason.client.engine.Cursor
import me.mason.client.engine.Window
import me.mason.shared.Collider
import me.mason.shared.TileMap

val FPS by lazy { Cursor(48 * TEXTURE_SHADER.quad, TEXTURE_BUFFER) }

suspend fun Window.Fps() {
    var fps = 0
    var fpsMin = 9999
    var fpsMax = 0
    var lastClear = 0f
    onFixed(10) { _, _ ->
        FPS.apply {
            clear()
            fntQuad {
                centerX = -30f; centerY = 19f
                r = 255
                g = 0
                b = 0
                a = 1
                text = "(min) fps: ${"%4s".format(fpsMin.toString())}"
                z = 10
                ui = true
            }
            fntQuad {
                centerX = -30f; centerY = 20f
                text = "      fps: ${"%4s".format(fps.toString())}"
                z = 10
                ui = true
            }
            fntQuad {
                centerX = -30f; centerY = 21f
                text = "(max) fps: ${"%4s".format(fpsMax.toString())}"
                z = 10
                ui = true
            }
        }
    }
    onTick { delta, elapsed ->
        fps = (1f / delta).toInt()
        if (fps < fpsMin) fpsMin = fps
        if (fps > fpsMax) fpsMax = fps
        if (elapsed - lastClear > 5f) {
            lastClear = elapsed
            fpsMin = 9999
            fpsMax = 0
        }
    }
}