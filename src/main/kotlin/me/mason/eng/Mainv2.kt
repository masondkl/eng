package me.mason.eng

import kotlinx.coroutines.*
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*
import kotlin.time.Duration.Companion.seconds

const val WORLD_SIZE = 32
const val WORLD_RADIUS = WORLD_SIZE / 2
const val RENDER_SIZE = 82
const val RENDER_RADIUS = RENDER_SIZE / 2

const val TILE_SIZE = 16f
val TILE_SIZE_VEC = vec(TILE_SIZE)
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

suspend fun main(args: Array<String>) {
    if (args.isNotEmpty()) server()
    else client()
}

val SRV_IN_MOVE = 0
val SRV_IN_REQUEST_ID = 1
val SRV_IN_END = 2

val SRV_OUT_PLAYER_MOVE = 3
val SRV_OUT_ID = 4
val SRV_OUT_END = 5
val SRV_OUT_PLAYER_DISCONNECT = 6

val CLI_OUT_MOVE = 0
val CLI_OUT_REQUEST_ID = 1
val CLI_OUT_END = 2

val CLI_IN_PLAYER_MOVE = 3
val CLI_IN_ID = 4
val CLI_IN_END = 5
val CLI_IN_PLAYER_DISCONNECT = 6

suspend fun server() {
    var eid = 0
    val searching = AtomicBoolean(true)
    val server = ServerSocket(9999)
    val connections = CopyOnWriteArraySet<Int>()
    val broadcasts = ConcurrentHashMap<Int, ConcurrentLinkedQueue<DataOutputStream.(Int) -> (Unit)>>()

    fun broadcast(block: DataOutputStream.(Int) -> (Unit)) = connections.forEach {
        broadcasts[it]!!.add(block)
    }
    suspend fun handle(client: Socket) {
        val write = DataOutputStream(client.getOutputStream())
        val read = DataInputStream(client.getInputStream())
        Thread { runBlocking {
            if (read.readInt() != SRV_IN_REQUEST_ID) error("Did not receive id request from client")
            val id = eid++.also {
                write.writeInt(SRV_OUT_ID)
                write.writeInt(it)
                connections.add(it)
                broadcasts[it] = ConcurrentLinkedQueue()
            }
            println("Player $id connected")
            while (true) {
                when(read.readInt()) {
                    SRV_IN_MOVE -> {
                        val moving = read.readInt()
                        val x = read.readFloat()
                        val y = read.readFloat()
                        broadcast {
                            if (it != moving) {
                                writeInt(SRV_OUT_PLAYER_MOVE)
                                writeInt(moving)
                                writeFloat(x); writeFloat(y)
                            }
                        }
                    }
                    SRV_IN_END -> {
                        write.writeInt(SRV_OUT_END)
                        connections.remove(id)
                        broadcasts.remove(id)
                        broadcast {
                            if (it != id) {
                                writeInt(SRV_OUT_PLAYER_DISCONNECT)
                                writeInt(id)
                            }
                        }
                        break
                    }
                }
                while(broadcasts[id]!!.isNotEmpty()) {
                    broadcasts[id]!!.poll().invoke(write, id)
                }
                delay(50)
            }
        } }.start()
    }
    while(true) {
        if (searching.compareAndSet(true, false)) {
            println("Accepting")
            handle(server.accept())
        }; searching.set(true)
    }
}

context(Buffer)
fun IntArray.tiledRaycast(
    idx: Int,
    start: FloatVector,
    dir: FloatVector
): FloatVector {
    val slope = vec(if (dir.y == 0f) Float.MAX_VALUE else sqrt(1 + (dir.y / dir.x) * (dir.y / dir.x)), if (dir.x == 0f) Float.MAX_VALUE else sqrt(1 + (dir.x / dir.y) * (dir.x / dir.y)))
    val length = vec(
        if (dir.x < 0) (start.x / TILE_SIZE - round(start.x / TILE_SIZE)) * slope.x else (round(start.x / TILE_SIZE + 1.0f) - start.x) * slope.x,
        if (dir.x < 0) (start.x / TILE_SIZE - round(start.x / TILE_SIZE)) * slope.x else (round(start.x / TILE_SIZE + 1.0f) - start.x) * slope.x
    )
    fun startTileX() = round(start.x / TILE_SIZE)
    fun startTileY() = round(start.y / TILE_SIZE)
    fun tile2d() = ((start + length) / TILE_SIZE).rounded()
    fun tileIndex(): Int {
        val tile2d = tile2d().int()
        if (tile2d.x < 0 || tile2d.x >= WORLD_SIZE || tile2d.y < 0 || tile2d.y >= WORLD_SIZE) return -1
        return tile2d.x + tile2d.y * WORLD_SIZE
    }
    var quad = idx
    var tile = tileIndex()
    while(tile != -1 && this[tile] !in SOLIDS) {
        tile = tileIndex()
        quad(quad++, start + offset * TILE_SIZE, TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC)
    }
    return start
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

fun client() = window { input { runBlocking {
    title = "title"
    width = 1280
    height = 720
    val texture = shader("texture.vert", "texture.frag", 2, 1)
    val world = IntArray(WORLD_SIZE * WORLD_SIZE) { 2 }
    camera.position = vec(WORLD_RADIUS).float() * TILE_SIZE
    camera.move()
    world[WORLD_RADIUS + WORLD_RADIUS * WORLD_SIZE] = 0
    world[WORLD_RADIUS + WORLD_RADIUS * WORLD_SIZE + 1] = 0
    world[WORLD_RADIUS + WORLD_RADIUS * WORLD_SIZE + 2] = 0
    world[WORLD_RADIUS + WORLD_RADIUS * WORLD_SIZE + 3] = 0
    world[WORLD_RADIUS + WORLD_RADIUS * WORLD_SIZE + 4] = 0
//    val positions = ConcurrentHashMap<Int, FloatVector>()
//    try {
//        val client = Socket("127.0.0.1", 9999)
//        val write = DataOutputStream(client.getOutputStream())
//        val read = DataInputStream(client.getInputStream())
//        write.writeInt(CLI_OUT_REQUEST_ID)
//        if (read.readInt() != CLI_IN_ID) error("Could not receive id from server")
//        val id = read.readInt()
//        println("You connected as player $id")
//        //--Read--
//        Thread {
//            runBlocking {
//                while (!glfwWindowShouldClose(window)) {
//                    when (read.readInt()) {
//                        CLI_IN_PLAYER_MOVE -> {
//                            val moving = read.readInt()
//                            val x = read.readFloat()
//                            val y = read.readFloat()
//                            if (!positions.containsKey(moving))
//                                println("Player $moving connected")
//                            positions[moving] = vec(x, y)
//                        }
//                        CLI_IN_PLAYER_DISCONNECT -> {
//                            val disconnected = read.readInt()
//                            positions.remove(disconnected)
//                            println("Player $disconnected disconnected")
//                        }
//                        CLI_IN_END -> break
//                    }
//                    delay(50)
//                }
//            }
//        }.apply { start() }
//        //--Write--
//        Thread {
//            runBlocking {
//                while (!glfwWindowShouldClose(window)) {
//                    write.writeInt(CLI_OUT_MOVE)
//                    write.writeInt(id)
//                    write.writeFloat(camera.position.x)
//                    write.writeFloat(camera.position.y)
//                    delay(50)
//                }
//                write.writeInt(CLI_OUT_END)
//            }
//        }.apply { start() }
//    } catch(_: Throwable) { error("Could not communicate with server") }
    buffer(texture, 512, Paths.get("shooter.png"), camera, fixed(144) { delta, elapsed, fixedTick ->
        clearQuad(0 until RENDER_SIZE * RENDER_SIZE)
        val cameraTile = (camera.position / TILE_SIZE).rounded().int()
        for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
            val tile = vec(index % RENDER_SIZE, index / RENDER_SIZE) - RENDER_RADIUS + cameraTile
            if (tile.x < 0 || tile.x >= WORLD_SIZE || tile.y < 0 || tile.y >= WORLD_SIZE) continue
            val tileIndex = tile.let { it.x + it.y * WORLD_SIZE }
            quad(index, tile.float() * TILE_SIZE, TILE_SIZE_VEC, TILES[world[tileIndex]], TILE_UV_SIZE_VEC)
        }
        val mouseWorld = mouseWorld()
        val theta = atan2(camera.position.y - mouseWorld.y, camera.position.x - mouseWorld.x) + PI.toFloat()
        val dir = vec(cos(theta), sin(theta))
//        println("${cos(theta)}, ${sin(theta)}")
        quad(RENDER_SIZE * RENDER_SIZE + 1, camera.position + dir * 16f, TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC)
//        quad(RENDER_SIZE * RENDER_SIZE + 2, world.tiledRaycast(camera.position, vec(cos(theta), sin(theta))), TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC)
        clearQuad(RENDER_SIZE * RENDER_SIZE + 2 until MAX_QUADS)
        world.tiledRaycast(RENDER_SIZE * RENDER_SIZE + 2, camera.position, dir)

        if (fixedTick) {
            if (keys[GLFW_KEY_W]) camera.position.y += 0.4f
            if (keys[GLFW_KEY_S]) camera.position.y -= 0.4f
            if (keys[GLFW_KEY_A]) camera.position.x -= 0.4f
            if (keys[GLFW_KEY_D]) camera.position.x += 0.4f
            camera.move()
            quad(
                RENDER_SIZE * RENDER_SIZE,
                camera.position, TILE_SIZE_VEC,
                PLAYER, PLAYER_UV_SIZE_VEC
            )
            //TODO: clear quads
//            positions.keys.forEachIndexed { index, id ->
//                println("drawing ")
//                quad(
//                    RENDER_SIZE * RENDER_SIZE + 3 + index,
//                    positions[id]!!, TILE_SIZE_VEC,
//                    PLAYER, PLAYER_UV_SIZE_VEC
//                )
//            }
        }
    })
} } }