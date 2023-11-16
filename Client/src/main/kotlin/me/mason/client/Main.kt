package me.mason.client

import com.github.exerosis.mynt.SocketProvider
import com.github.exerosis.mynt.base.Address
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import me.mason.client.component.*
import me.mason.client.engine.*
import me.mason.shared.*
import org.joml.Vector2f
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL30.*
import java.lang.Math.toRadians
import java.lang.Runtime.getRuntime
import java.nio.channels.AsynchronousChannelGroup
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Executors.newFixedThreadPool
import kotlin.io.path.readBytes
import kotlin.math.*

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
val FONT_TEXTURE by lazy { Texture(Paths.get("font.png")) }

val TEXTURE_INDICES by lazy { mapOf(GAME_TEXTURE to 0, FONT_TEXTURE to 1) }
val TEXTURE_ACTIVE_INDICES by lazy { mapOf(GAME_TEXTURE to GL_TEXTURE0, FONT_TEXTURE to GL_TEXTURE1) }
val TEXTURE_SHADER by lazy { Shader(Paths.get("texture.vert"), Paths.get("texture.frag"), 1, 1, 1) }
val TEXTURE_BUFFER by lazy { GraphicsBuffer(TEXTURE_SHADER, 172000) }

val AUDIO by lazy { AudioSource(64) }
val SHOOT by lazy { WaveData.create(Paths.get("shoot.wav").readBytes()) }

val CHANNEL_GROUP: AsynchronousChannelGroup =
    AsynchronousChannelGroup.withThreadPool(newFixedThreadPool(getRuntime().availableProcessors() * 8))

suspend fun main() = window {
    val map = Maps[0]
    val collider = Collider(camera, TILE_DIM)

    Networking(Address("127.0.0.1", 9999), SocketProvider(9999, CHANNEL_GROUP))
    World(map)
    Shadows(map)
    Player(map, collider)
    Fps()

    onTick { _, _ ->
        glClearColor(236f / 350f, 247f / 350f, 252f / 350f, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)

        TEXTURE_BUFFER.draw(WORLD, PLAYER, FPS)

        glStencilMask(0xFF)
        glStencilFunc(GL_ALWAYS, 1, 0xFF)
        glColorMask(false, false, false, false)
        TEXTURE_BUFFER.draw(VIEW)
        glStencilMask(0x00)
        glStencilFunc(GL_EQUAL, 1, 0xFF)
        glColorMask(true, true, true, true)
        //clipped

        glStencilFunc(GL_NOTEQUAL, 1, 0xFF)
        TEXTURE_BUFFER.draw(FOG)
        glStencilMask(0xFF)
        glStencilFunc(GL_ALWAYS, 0, 0xFF)

        glfwSwapBuffers(window)
        glfwPollEvents()
    }
}