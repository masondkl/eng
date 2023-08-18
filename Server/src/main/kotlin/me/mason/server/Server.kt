//package me.mason.server
//
//import kotlinx.coroutines.*
//import kotlinx.coroutines.sync.withLock
//import me.mason.shared.*
//import me.mason.sockets.Connection
//import me.mason.sockets.accept
//import org.joml.Vector2f
//import org.joml.Vector2i
//import java.nio.ByteBuffer.wrap
//import java.nio.file.Path
//import java.nio.file.Paths
//import java.util.*
//import kotlin.io.path.readBytes
//import kotlin.time.Duration.Companion.seconds
//
//const val IN_JOIN = 0.toByte()
//const val IN_PING = 1.toByte()
//const val IN_POS = 2.toByte()
//const val IN_SHOOT = 3.toByte()
//const val IN_START_PLANT = 4.toByte()
//const val IN_START_DEFUSE = 5.toByte()
//const val IN_CANCEL_PLANT = 6.toByte()
//const val IN_CANCEL_DEFUSE = 7.toByte()
//
//const val OUT_JOIN = 0.toByte()
//const val OUT_EXIT = 1.toByte()
//const val OUT_MAP = 2.toByte()
//const val OUT_POS = 3.toByte()
//const val OUT_SHOOT = 4.toByte()
//const val OUT_DIE = 5.toByte()
//const val OUT_RESPAWN = 6.toByte()
//const val OUT_TELEPORT = 7.toByte()
//const val OUT_END_PLANT = 8.toByte()
//const val OUT_END_DEFUSE = 9.toByte()
//const val OUT_CONFIRM_START_DEFUSE = 10.toByte()
//const val OUT_BOMB = 11.toByte()
//const val OUT_CLEAR_BOMB = 12.toByte()
//
//val MAP_DIRECTORY: Path = Paths.get("maps")
//
//val FFA = 0
//val DM4 = 1
//val SD2 = 2
//
//val MODE = SD2
//
//suspend fun main() {
//    val players = BitSet()
//    val counters = BitSet()
//    val terrorists = BitSet()
//    val pings = Array(256) { 0L }
//    val mapNames = MAP_DIRECTORY.toFile().listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
//    val counterSpawns = Array(mapNames.size) { ArrayList<Vector2f>() }
//    val terroristSpawns = Array(mapNames.size) { ArrayList<Vector2f>() }
//    val maps = Array(mapNames.size) { map ->
//        wrap(MAP_DIRECTORY.resolve("${mapNames[map]}.map").readBytes()).run {
//            val worldSize = int
//            val world = ByteArray(worldSize * worldSize); get(world)
//            val walls = ArrayList<Bounds>()
//            for (i in 0 until int) walls.add(Bounds(
//                Vector2f(float, float),
//                Vector2f(float, float)
//            ))
//            for (i in 0 until int) counterSpawns[map].add(Vector2f(float, float))
//            for (i in 0 until int) terroristSpawns[map].add(Vector2f(float, float))
//            TileMap(mapNames[map], worldSize, walls, world)
//        }
//    }
//    val map = mapNames.indices.random()
//    val connections = Array<Connection?>(256) { null }
//    val healths = Array(256) { 1f }
//    val fromPositions = Array(256) { Vector2f() }
//    val toPositions = Array(256) { Vector2f() }
//    val updates = Array(256) { 0L }
//    val defusing = BitSet(256)
//    val planting = BitSet(256)
//    var planted = false
//    val spikeJobs = ArrayList<Job>()
//    var defuseJob: Job? = null
//    fun interpolated(id: Int): Vector2f {
//        val from = fromPositions[id]
//        val t = (timeMillis - updates[id]) / 50f
//        val diff = toPositions[id] - from
//        return from + (diff * t)
//    }
//    suspend fun broadcast(block: suspend (Int, Connection) -> (Unit)) {
//        players.forEach { id ->
//            connections[id]?.also { connection ->
//                connection.writeLock.withLock {
//                    block(id, connection)
//                }
//            }
//        }
//    }
//    suspend fun Int.send(block: suspend (Int, Connection) -> (Unit)) {
//        val id = this
//        connections[id]?.also { connection ->
//            connection.writeLock.withLock {
//                block(id, connection)
//            }
//        }
//    }
//    suspend fun sdStart() {
//        spikeJobs.removeIf { it.cancel(); true }
//        planted = false
//        counters.clear()
//        terrorists.clear()
//        players.forEachIndexed { idx, otherId ->
//            (if (idx % 2 == 0) counters else terrorists).set(otherId)
//        }
//        broadcast { playerId, player ->
//            healths[playerId] = 1f
//            broadcast { _, receive ->
//                receive.byte(OUT_RESPAWN)
//                receive.int(playerId)
//            }
//            player.byte(OUT_CLEAR_BOMB)
//            player.byte(OUT_TELEPORT)
//            player.vec2f(
//                if (counters[playerId]) counterSpawns[map].random()
//                else terroristSpawns[map].random()
//            )
//        }
//    }
//    accept(9999) { coroutineScope { launch {
//        var id = -1
//        onClose {
//            println("Player $id exited")
//            pings[id] = 0L
//            counters.clear(id)
//            terrorists.clear(id)
//            players.clear(id)
//            connections[id] = null
//            broadcast { _, player ->
//                player.byte(OUT_EXIT)
//                player.int(id)
//            }
//        }
//        try { while (open) when(byte()) {
//            IN_JOIN -> {
//                id = players.nextClearBit(0)
//                println("Player $id joined")
//                writeLock.withLock {
//                    int(id)
//                    byte(OUT_MAP)
//                    map(maps[map])
//                }
//                broadcast { playerId, player ->
//                    player.byte(OUT_JOIN)
//                    player.int(id)
//                    byte(OUT_JOIN)
//                    int(playerId)
//                }
//                connections[id] = this@accept
//                if (MODE == SD2) {
//                    if (players.cardinality() >= 2) {
//                        healths[id] = -1f
//                        broadcast { _, player ->
//                            player.byte(OUT_DIE)
//                            player.int(id)
//                        }; continue
//                    }
//                    (if (players.cardinality() % 2 == 0) counters else terrorists).set(id)
//                    players.set(id)
//                    writeLock.withLock {
//                        byte(OUT_TELEPORT)
//                        vec2f(
//                            if (counters[id]) counterSpawns[map].random()
//                            else terroristSpawns[map].random()
//                        )
//                    }
//                } else {
//                    players.set(id)
//                    val tileMap = maps[map]
//                    val worldSize = tileMap.worldSize
//                    val found = Vector2f(1f, 1f)
//                    for (tile in (0 until worldSize * worldSize).shuffled()) {
//                        val tilePos = Vector2i(tile % worldSize, tile / worldSize).float()
//                        if (tileMap.world[tile] in SOLIDS || players.any { toPositions[it].distance(tilePos) < 30.0f })
//                            continue
//                        found.set((tile % worldSize).toFloat(), (tile / worldSize).toFloat())
//                    }
//                    writeLock.withLock {
//                        byte(OUT_TELEPORT)
//                        vec2f(found)
//                    }
//                }
//            }
//            IN_PING -> {
//                val now = timeMillis
//                if (pings[id] != 0L && pings[id].let { now - it } > 2000L) {
//                    println("Player $id timed out")
//                    close(); return@launch
//                }; pings[id] = now
//            }
//            IN_POS -> {
//                val pos = vec2f()
//                updates[id] = timeMillis
//                fromPositions[id] = toPositions[id].copy()
//                toPositions[id] = pos
//                broadcast { playerId, player ->
//                    if (playerId == id) return@broadcast
//                    player.byte(OUT_POS)
//                    player.int(id)
//                    player.vec2f(pos)
//                }
//            }
//            IN_SHOOT -> {
//                println("Player ${id} shot")
//                val dir = vec2f()
//                val pos = interpolated(id)
//                broadcast { playerId, player ->
//                    if (playerId == id) return@broadcast
//                    player.byte(OUT_SHOOT)
//                    player.vec2f(pos)
//                    player.vec2f(dir)
//                }
//                val blockHit = maps[map].world.tiledRaycast(maps[map].worldSize, pos, dir)
//                val playerHit = raycast<Int>(pos, dir * 0.33f) {
//                    val intersected = players.first { player ->
//                        if (MODE == SD2 && terrorists[id] == terrorists[player]) return@first false
//                        val interpolated = interpolated(player)
//                        val min = interpolated - TILE_SIZE / 2f
//                        val max = interpolated + TILE_SIZE / 2f
//                        it.x > min.x && it.x < max.x && it.y > min.y && it.y < max.y && player != id
//                    }
//                    if (intersected != -1) data = intersected
//                    intersected != -1
//                }
//                if (!playerHit.result || playerHit.distance > blockHit.distance) continue
//                val targetId = playerHit.data!!
//                if (healths[targetId] < 0f) continue
//                healths[targetId] -= 0.3f
//                if (healths[targetId] > 0f) continue
//                broadcast { _, player ->
//                    player.byte(OUT_DIE)
//                    player.int(targetId)
//                }
//                launch { when(MODE) {
//                    FFA -> {
//                        delay(1.seconds)
//                        val tileMap = maps[map]
//                        val worldSize = tileMap.worldSize
//                        val found = Vector2f(1f, 1f)
//                        for (tile in (0 until worldSize * worldSize).shuffled()) {
//                            val tilePos = Vector2i(tile % worldSize, tile / worldSize).float()
//                            if (tileMap.world[tile] in SOLIDS || players.any { toPositions[it].distance(tilePos) < 30.0f })
//                                continue
//                            found.set((tile % worldSize).toFloat(), (tile / worldSize).toFloat())
//                        }
//                        healths[targetId] = 1f
//                        broadcast { _, player ->
//                            player.byte(OUT_RESPAWN)
//                            player.int(targetId)
//                        }
//                        connections[targetId]?.also { target ->
//                            target.writeLock.withLock {
//                                target.byte(OUT_TELEPORT)
//                                target.vec2f(found)
//                            }
//                        }
//                    }
//                    DM4 -> {
//
//                    }
//                    SD2 -> {
//                        if (counters.all { healths[it] < 0f }) {
//                            //Terrorists win
//                            delay(1.seconds)
//                            sdStart()
//                        } else if (terrorists.all { healths[it] < 0f } && !planted) {
//                            //Counters win
//                            delay(1.seconds)
//                            sdStart()
//                        }
//                    }
//                } }
//            }
//            IN_START_PLANT -> {
//                if (MODE != SD2 || counters[id]) continue
//                //TODO: Check that they can plant
//                println("Player ${id} start plant")
//                val plant = interpolated(id)
//                planting.set(id)
//                spikeJobs.add(0, launch defuse@{
//                    delay(4.seconds)
//                    if (!players[id] || !planting[id] || healths[id] < 0f) return@defuse
//                    println("Player ${id} planted")
//                    planted = true
//                    planting.clear(id)
//                    broadcast { _, player ->
//                        player.byte(OUT_BOMB)
//                        player.vec2f(plant)
//                    }
//                    spikeJobs.add(0, launch {
//                        delay(30.seconds)
//                        sdStart()
//                    })
//                })
//            }
//            IN_START_DEFUSE -> {
//                if (MODE != SD2 || terrorists[id] || defusing.cardinality() > 0) continue
//                //TODO: Check that they can defuse
//                defusing.set(id)
//                writeLock.withLock {
//                    byte(OUT_CONFIRM_START_DEFUSE)
//                }
//                defuseJob = launch defuse@{
//                    delay(4.seconds)
//                    if (!players[id] || !defusing[id] || healths[id] < 0f) return@defuse
//                    writeLock.withLock {
//                        byte(OUT_END_DEFUSE)
//                    }
//                    delay(1.seconds)
//                    sdStart()
//                }
//            }
//            IN_CANCEL_PLANT -> {
//                planting.clear(id)
//            }
//            IN_CANCEL_DEFUSE -> {
//                defusing.clear(id)
//                defuseJob?.cancel()
//            }
//        } } catch (err: Throwable) { close() }
//    } } }
//}