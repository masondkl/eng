package me.mason.eng

import kotlinx.coroutines.*
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.lang.Math.toRadians
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Paths
import java.util.*
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

fun client() = window { input { runBlocking {
    title = "title"
    width = 1280
    height = 720
    val texture = shader("texture.vert", "texture.frag", 2, 1)
    val triangle = shader("fov.vert", "fov.frag", 2)
    val world = IntArray(WORLD_SIZE * WORLD_SIZE) { 6 }
    camera.position = vec(WORLD_RADIUS).float()
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

//    buffer(512, Paths.get("shooter.png"), camera, texture to fixed(144) { delta, elapsed, fixedTick ->
//        clearQuad(texture, 0 until RENDER_SIZE * RENDER_SIZE)
//        val cameraTile = camera.position.rounded().int()
//        for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
//            val tile = vec(index % RENDER_SIZE, index / RENDER_SIZE) - RENDER_RADIUS + cameraTile
//            if (tile.x < 0 || tile.x >= WORLD_SIZE || tile.y < 0 || tile.y >= WORLD_SIZE) continue
//            val tileIndex = tile.let { it.x + it.y * WORLD_SIZE }
//            quad(texture, index, tile.float(), TILE_SIZE_VEC, TILES[world[tileIndex]], TILE_UV_SIZE_VEC)
//        }
////        println("${cos(theta)}, ${sin(theta)}")
////        quad(RENDER_SIZE * RENDER_SIZE + 2, world.tiledRaycast(camera.position, vec(cos(theta), sin(theta))), TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC)
//        clearQuad(texture, RENDER_SIZE * RENDER_SIZE + 3 until MAX_QUADS)
//
//
//        if (fixedTick) {
//            if (keys[GLFW_KEY_W]) camera.position.y += 1.0f / 32f
//            if (keys[GLFW_KEY_S]) camera.position.y -= 1.0f / 32f
//            if (keys[GLFW_KEY_A]) camera.position.x -= 1.0f / 32f
//            if (keys[GLFW_KEY_D]) camera.position.x += 1.0f / 32f
//            camera.move()
////            val topLeft
////            if ()
//            quad(
//                texture,
//                RENDER_SIZE * RENDER_SIZE,
//                camera.position, TILE_SIZE_VEC,
//                PLAYER, PLAYER_UV_SIZE_VEC
//            )
//            //TODO: clear quads
////            positions.keys.forEachIndexed { index, id ->
////                println("drawing ")
////                quad(
////                    RENDER_SIZE * RENDER_SIZE + 3 + index,
////                    positions[id]!!, TILE_SIZE_VEC,
////                    PLAYER, PLAYER_UV_SIZE_VEC
////                )
////            }
//        }
//    }, triangle to fixed(144) { delta, elapsed, fixedTick ->
//        val fov = 90
//        val fovRad = toRadians(fov.toDouble())
//        val mouseWorld = mouseWorld()
//        val theta = atan2(camera.position.y - mouseWorld.y, camera.position.x - mouseWorld.x) + PI.toFloat()
//        val startRad = theta + fovRad / 2f
//////        val a = world.tiledRaycast(texture, RENDER_SIZE * RENDER_SIZE + 3, camera.position, theta)
//////        val b = world.tiledRaycast(texture, RENDER_SIZE * RENDER_SIZE + 4, camera.position, theta)
//        val list: Deque<FloatVector> = LinkedList()
//
//        val pointList = ArrayList<FloatVector>()
//
//        if(pointList.isNotEmpty())
//            pointList.clear()
//
//        var j = 0
//        val camPos = camera.position;
//        val fovResolutionMultiplier = 0.25
//
//        //tri(triangle, 0, vec(-1.0f, -1.0f), vec(0.0f, 0.0f), vec(1.0f, -1.0f))
//        //tri(triangle, 1, vec(6.0f, -1.0f), vec(7.0f, 0.0f), vec(8.0f, -1.0f))
//
//        tri(triangle, 0, vec(0f, 0f), vec(0f, 1f), vec(1f, 0f))
//        tri(triangle, 1, vec(5f, 0f), vec(5f, 1f), vec(6f, 0f))
//
////        for (i in 0 until (fov*fovResolutionMultiplier).toInt())
////        {
////            pointList.add(world.tiledRaycast(texture, i + RENDER_SIZE * RENDER_SIZE + 2, camPos, (startRad - toRadians((i.toFloat().todo / fovResolutionMultiplier))).toFloat()))
////            if(i > 0)
////                tri(triangle, i-1, pointList[i-1], pointList[i], camPos)
////        }
//
////        for (i in 0 until fov-1)
////        {
////            tri(triangle, j++, camera.position, pointList[i], pointList[i+1])
////        }
//
//
////
//    val fov = 90
//    val fovRad = toRadians(fov.toDouble())
//    val mouseWorld = mouseWorld()
//    val theta = atan2(camera.position.y - mouseWorld.y, camera.position.x - mouseWorld.x) + PI.toFloat()
//    val startRad = theta + fovRad / 2f
//    val list: Deque<FloatVector> = LinkedList()
//        for (i in 0 until fov) {
//            list.add(world.tiledRaycast(i + RENDER_SIZE * RENDER_SIZE + 3, camera.position, (startRad - toRadians(i.toDouble())).toFloat()))
//        }
////        var i = 0
////        while(list.isNotEmpty())
////            tri(triangle, i++, list.pollFirst(), list.pollFirst(), camera.position)
//
//    })
    buffer(512, Paths.get("shooter.png"), camera) {
        tick { delta, elapsed ->
//            println(1.0/delta)
            using(texture) {
                clearQuad(0 until RENDER_SIZE * RENDER_SIZE)
                val cameraTile = camera.position.rounded().int()
                for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
                    val tile = vec(index % RENDER_SIZE, index / RENDER_SIZE) - RENDER_RADIUS + cameraTile
                    if (tile.x < 0 || tile.x >= WORLD_SIZE || tile.y < 0 || tile.y >= WORLD_SIZE) continue
                    val tileIndex = tile.let { it.x + it.y * WORLD_SIZE }
                    quad(index, tile.float(), TILE_SIZE_VEC, TILES[world[tileIndex]], TILE_UV_SIZE_VEC)
                }
            }
            using(triangle) {
                val fov = 90
                val fovRad = toRadians(fov.toDouble())
                val mouseWorld = mouseWorld()
                val theta = atan2(camera.position.y - mouseWorld.y, camera.position.x - mouseWorld.x) + PI.toFloat()
//                println(theta)

                val fovResolutionMultiplier = 1

                val startRad = theta + fovRad / 2f

                val list = Array(fov * fovResolutionMultiplier) {
                    world.tiledRaycast(it + RENDER_SIZE * RENDER_SIZE + 3, camera.position, (startRad - toRadians(it.toDouble() / fovResolutionMultiplier)).toFloat())
                }

                var previous: RayResult
                var current: RayResult

                for (index in 0 until fov * fovResolutionMultiplier) {
                    current = list[index]
                    if(index > 0) {
                        previous = list[index - 1]

                        if(current.hitTile != previous.hitTile)
                        {
                            val newDistance = current.hitTile.float().distance(camera.position)
                            val prevDistance = previous.hitTile.float().distance(camera.position)

                            if(abs(newDistance - prevDistance) > 1.3f)
                            {
                                if(prevDistance > newDistance)
                                {
                                    // if bottom edge, get left corner, if left edge, get top corner
                                }
                                else
                                {

                                }
                            }
                        }
                    }
                }

                println(list[0].hitTile)
                for (index in 1 until fov * fovResolutionMultiplier) {
                    tri(index, camera.position, list[index - 1].hit, list[index].hit)
                }
            }
        }
        fixed(144) { elapsed ->
            if (keys[GLFW_KEY_W]) camera.position.y += 1.0f / 32f
            if (keys[GLFW_KEY_S]) camera.position.y -= 1.0f / 32f
            if (keys[GLFW_KEY_A]) camera.position.x -= 1.0f / 32f
            if (keys[GLFW_KEY_D]) camera.position.x += 1.0f / 32f
            camera.move()
            using(texture) {
                quad(RENDER_SIZE * RENDER_SIZE, camera.position, TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC)
            }
        }
    }
} } }