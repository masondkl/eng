package me.mason.client

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mason.sockets.connect
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL20.*
import java.lang.Math.toRadians
import java.lang.System.exit
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer.allocate
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.ClosedChannelException
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*
import kotlin.system.exitProcess

const val WORLD_SIZE = 32
const val WORLD_RADIUS = WORLD_SIZE / 2
const val RENDER_SIZE = 82
const val RENDER_RADIUS = RENDER_SIZE / 2

val TILE_SIZE_VEC = vec(1.0f)
val TILE_UV_SIZE = 4
val TILE_UV_SIZE_VEC = vec(TILE_UV_SIZE)
val TILES = Array(11) {
    val x = (it * 4) % 512
    val y = ((it * 4) / 512) * 4
    vec(x, y)
}
val SOLIDS = arrayOf(0)

val PLAYER = vec(0, 4)
val PLAYER_UV_SIZE = 4
val PLAYER_UV_SIZE_VEC = vec(4, 4)

//suspend fun main(args: Array<String>) {
//    if (args.isNotEmpty()) myntServer()
//    else client()
//}

val CLI_OUT_MOVE = 0.toByte()
val CLI_OUT_REQUEST_ID = 1.toByte()
val CLI_OUT_END = 2.toByte()

val CLI_IN_PLAYER_MOVE = 3.toByte()
val CLI_IN_ID = 4.toByte()
val CLI_IN_END = 5.toByte()
val CLI_IN_PLAYER_DISCONNECT = 6.toByte()

interface RayResult {
    val hit: FloatVector
    val hitTile: IntVector
    val side: Int
}

context(Buffer)
fun IntArray.tiledRaycast(
    idx: Int,
    start: FloatVector,
    theta: Float,
    max: Float = 50f
): RayResult {
    val dir = vec(cos(theta), sin(theta))
    val currentTileCoord = vec(start.x.roundToInt(), start.y.roundToInt())
    val bottomLeft = vec(currentTileCoord.x - 0.5f, currentTileCoord.y - 0.5f)
    val rayMaxStepSize = vec(abs(1 / dir.x), abs(1 / dir.y))
    val rayStepLength = vec(0f, 0f)
    val mapStep = vec(0, 0)

    if (dir.x < 0) {
        mapStep.x = -1
        rayStepLength.x = (start.x - bottomLeft.x) * rayMaxStepSize.x
    } else {
        mapStep.x = 1
        rayStepLength.x = (bottomLeft.x + 1 - start.x) * rayMaxStepSize.x
    }

    if (dir.y < 0) {
        mapStep.y = -1
        rayStepLength.y = (start.y - bottomLeft.y) * rayMaxStepSize.y
    } else {
        mapStep.y = 1
        rayStepLength.y = (bottomLeft.y + 1 - start.y) * rayMaxStepSize.y
    }

    var fDistance = 0f
    var currentCoord = vec(0.0f, 0.0f)
    var hitSomething = false

    while (fDistance < max) {
        currentCoord = start.clone()
        if (rayStepLength.x < rayStepLength.y) {
            currentCoord = currentCoord + dir * rayStepLength.x
            currentTileCoord.x += mapStep.x
            fDistance = rayStepLength.x
            rayStepLength.x += rayMaxStepSize.x
        } else {
            currentCoord = currentCoord + dir * rayStepLength.y
            currentTileCoord.y += mapStep.y
            fDistance = rayStepLength.y
            rayStepLength.y += rayMaxStepSize.y
        }
        val index = (currentTileCoord.y * WORLD_SIZE) + currentTileCoord.x
        if(currentTileCoord.x >= WORLD_SIZE || currentTileCoord.x < 0 ||
            currentTileCoord.y >= WORLD_SIZE || currentTileCoord.y < 0 ||
            this[index] in SOLIDS
        ) {
            hitSomething = true
            break
        }
    }
    //TODO:idk
//    quad(shader, idx, if (hitSomething) currentCoord else start + dir * max, TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC)
    return object : RayResult {
        override val hit = if (hitSomething) currentCoord else start + dir * max
        override val hitTile = currentTileCoord
        override val side =
            if (hit.x == currentTileCoord.x - 0.5f) 0
            else if (hit.x == currentTileCoord.x + 0.5f) 2
            else if (hit.y == currentTileCoord.y - 0.5f) 3
            else if (hit.y == currentTileCoord.y + 0.5f) 1
            else -1
    }
}

fun Window.mouseWorld(): FloatVector {
    val mouseX = BufferUtils.createDoubleBuffer(1)
    val mouseY = BufferUtils.createDoubleBuffer(1)
    val width = BufferUtils.createIntBuffer(1)
    val height = BufferUtils.createIntBuffer(1)
    glfwGetCursorPos(window, mouseX, mouseY)
    glfwGetWindowSize(window, width, height)
    return vec(
        mouseX.get().toFloat() + camera.position.x - 640,
        height.get() - mouseY.get().toFloat() + camera.position.y - 360
    )
}

fun main() = window { input { runBlocking {
    title = "title"
    width = 1280
    height = 720
    val fovTriangles = FloatArray(1536) { 1f }
    val textureShader = shader("texture.vert", "texture.frag") {
        attr(2); attr(1)
    }
    val clipTextureShader = shader("cliptexture.vert", "cliptexture.frag") {
        attr(2); attr(1)
        uniform("triangles") { loc -> glUniform1fv(loc, fovTriangles) }
    }
    val fovShader = shader("fov.vert", "fov.frag") {
        attr(2)
        uniform("triangles") { loc -> glUniform1fv(loc, fovTriangles) }
    }
    val world = IntArray(WORLD_SIZE * WORLD_SIZE) { 6 }
    camera.position = vec(WORLD_RADIUS).float()
    camera.move()
    world[WORLD_RADIUS + WORLD_RADIUS * WORLD_SIZE] = 0
    world[WORLD_RADIUS + WORLD_RADIUS * WORLD_SIZE + 1] = 0
    world[WORLD_RADIUS + WORLD_RADIUS * WORLD_SIZE + 2] = 0
    world[WORLD_RADIUS + WORLD_RADIUS * WORLD_SIZE + 3] = 0
    world[WORLD_RADIUS + WORLD_RADIUS * WORLD_SIZE + 4] = 0

    val positions = ConcurrentHashMap<Int, FloatVector>()

    connect("parkourmate.com", 9999) { context ->
        write.byte(CLI_OUT_REQUEST_ID)
        if (read.byte() != CLI_IN_ID) error("Could not receive id from server")
        val id = read.int()
        println("Connected as ${id}")
        launch(context) {
            try {
                while (isActive && open) {
                    val op = read.byte()
                    if (op == CLI_IN_PLAYER_MOVE) {
                        val moving = read.int()
                        val x = read.float()
                        val y = read.float()
                        if (!positions.containsKey(moving))
                            println("Player $moving connected")
                        positions[moving] = vec(x, y)
                    } else if (op == CLI_IN_PLAYER_DISCONNECT) {
                        val disconnected = read.int()
                        positions.remove(disconnected)
                        println("Player $disconnected disconnected")
                    }
                }
            } catch (_: Throwable) { }
        }
        launch(context) {
            try {
                while (isActive && open && !glfwWindowShouldClose(window)) {
                    write.byte(CLI_OUT_MOVE)
                    write.int(id)
                    write.float(camera.position.x)
                    write.float(camera.position.y)
                    delay(50)
                }; close()
            } catch(_: Throwable) {}
        }
    }
//    val address = InetSocketAddress("127.0.0.1", 9999)
//    val executor = Executors.newFixedThreadPool(4)
//    val group = AsynchronousChannelGroup.withThreadPool(executor)
//    val provider = SocketProvider(50000, group) {
//        it.setOption(StandardSocketOptions.TCP_NODELAY, true)
//        it.setOption(StandardSocketOptions.SO_KEEPALIVE, false)
//    }
//    val connection = provider.connect(address)
//    val clientJob = GlobalScope.launch(executor.asCoroutineDispatcher()) {
//        connection.apply {
//            var id = 0
//            try {
//                write.byte(CLI_OUT_REQUEST_ID)
//                if (read.byte() != CLI_IN_ID) error("Could not receive id from server")
//                id = read.int()
//                println("You connected as player $id")
//            } catch (_: ClosedChannelException) {
//            } catch (throwable: Throwable) {
//                throwable.printStackTrace()
//                //                exit(1)
//            }
//            launch {
//                try {
//                    while (isActive && isOpen && !glfwWindowShouldClose(window)) {
//                        when (read.byte()) {
//                            CLI_IN_PLAYER_MOVE -> {
//                                val moving = read.int()
//                                val x = read.float()
//                                val y = read.float()
//                                if (!positions.containsKey(moving))
//                                    println("Player $moving connected")
//                                positions[moving] = vec(x, y)
//                            }
//
//                            CLI_IN_PLAYER_DISCONNECT -> {
//                                val disconnected = read.int()
//                                positions.remove(disconnected)
//                                println("Player $disconnected disconnected")
//                            }
//                        }
//                    }
//                } catch (_: ClosedChannelException) {
//                    println("closed channel a")
//                } catch (throwable: Throwable) {
//                    throwable.printStackTrace()
//                }
//            }
//            launch {
//                try {
//                    while (isActive && isOpen && !glfwWindowShouldClose(window)) {
//                        println("like wat 2")
//                        write.byte(CLI_OUT_MOVE)
//                        write.int(id)
//                        write.float(camera.position.x)
//                        write.float(camera.position.y)
//                    }
//                } catch (_: ClosedChannelException) {
//                    println("closed channel b")
//                } catch (throwable: Throwable) {
//                    println("??")
//                    throwable.printStackTrace()
//                }
//            }
//        }
//    }
    buffer(512, Paths.get("shooter.png"), camera, {  }) {
        tick { delta, elapsed ->
            using(fovShader, 2) {
                quad(0, camera.position, vec(80f, 45f))
//                tri(0, vec(0f, 0f), vec(1f, -1f), vec(-1f, -1f))
                val fov = 120
                val fovRad = toRadians(fov.toDouble())
                val mouseWorld = mouseWorld()
                val theta = atan2(camera.position.y - mouseWorld.y, camera.position.x - mouseWorld.x) + PI.toFloat()
////                println(theta)
//
                val fovResolutionMultiplier = 0.5
//
                val startRad = theta + fovRad / 2f
////
                val list = Array((fov * fovResolutionMultiplier).toInt()) {
                    world.tiledRaycast(it + RENDER_SIZE * RENDER_SIZE + 3, camera.position, (startRad - toRadians(it.toDouble() / fovResolutionMultiplier)).toFloat())
                }
//                list.forEachIndexed { index, it ->
//                    fovTriangles[index * 2] = it.hit.x - camera.position.x
//                    fovTriangles[index * 2 + 1] = it.hit.y - camera.position.x
//                }

                for (index in 1 until (fov * fovResolutionMultiplier).toInt()) {
//                    println(index)
                    fovTriangles[(index - 1) * 6] = list[index - 1].hit.x
                    fovTriangles[(index - 1) * 6 + 1] = list[index - 1].hit.y
                    fovTriangles[(index - 1) * 6 + 2] = list[index].hit.x
                    fovTriangles[(index - 1) * 6 + 3] = list[index].hit.y
                    fovTriangles[(index - 1) * 6 + 4] = camera.position.x
                    fovTriangles[(index - 1) * 6 + 5] = camera.position.y
                }

//                var previous: RayResult
//                var current: RayResult
//
//                for (index in 0 until fov * fovResolutionMultiplier) {
//                    current = list[index]
//                    if(index > 0) {
//                        previous = list[index - 1]
//
//                        if(current.hitTile != previous.hitTile)
//                        {
//                            val newDistance = current.hitTile.float().distance(camera.position)
//                            val prevDistance = previous.hitTile.float().distance(camera.position)
//
//                            if(abs(newDistance - prevDistance) > 1.3f)
//                            {
//                                if(prevDistance > newDistance)
//                                {
//                                    // if bottom edge, get left corner, if left edge, get top corner
//                                }
//                                else
//                                {
//
//                                }
//                            }
//                        }
//                    }
//                }
//
//                println(list[0].hitTile)
            }
//            println(1.0/delta)
            using(textureShader, 0) {
                clearQuad(0 until RENDER_SIZE * RENDER_SIZE)
                val cameraTile = camera.position.rounded().int()
                for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
                    val tile = vec(index % RENDER_SIZE, index / RENDER_SIZE) - RENDER_RADIUS + cameraTile
                    if (tile.x < 0 || tile.x >= WORLD_SIZE || tile.y < 0 || tile.y >= WORLD_SIZE) continue
                    val tileIndex = tile.let { it.x + it.y * WORLD_SIZE }
                    textureQuad(index, tile.float(), TILE_SIZE_VEC, TILES[world[tileIndex]], TILE_UV_SIZE_VEC)
                }
            }
            using(clipTextureShader, 1) {
                clearQuad(0 until 128)
                positions.keys.forEachIndexed { index, id ->
                    textureQuad(
                        index,
                        positions[id]!!, TILE_SIZE_VEC,
                        PLAYER, PLAYER_UV_SIZE_VEC
                    )
                }
            }
        }
        fixed(144) { elapsed ->
            if (keys[GLFW_KEY_W]) camera.position.y += 1.0f / 32f
            if (keys[GLFW_KEY_S]) camera.position.y -= 1.0f / 32f
            if (keys[GLFW_KEY_A]) camera.position.x -= 1.0f / 32f
            if (keys[GLFW_KEY_D]) camera.position.x += 1.0f / 32f
            camera.move()
            using(textureShader, 0) {
                textureQuad(RENDER_SIZE * RENDER_SIZE, camera.position, TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC)
            }
        }
    }
} } }