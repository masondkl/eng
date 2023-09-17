//package me.mason.server
//
//import com.github.exerosis.mynt.base.Connection
//import kotlinx.coroutines.*
//import kotlinx.coroutines.channels.consumeEach
//import me.mason.shared.*
//import org.joml.Vector2f
//import java.nio.channels.ClosedChannelException
//import kotlin.time.Duration.Companion.seconds
//
//const val IN_JOIN = 0
//const val IN_SHOOT = 1
//const val IN_POS = 2
//const val IN_CONFIRM_RESPAWN = 3
//const val IN_RESPOND_QUERY = 4
//const val IN_CALL_VOTE = 5
//
//const val OUT_JOIN = 0
//const val OUT_EXIT = 1
//const val OUT_MAP = 2
//const val OUT_POS = 3
//const val OUT_SHOOT = 4
//const val OUT_DIE = 5
//const val OUT_TRY_RESPAWN = 6
//const val OUT_RESPAWN = 7
//const val OUT_QUERY = 8
//const val OUT_CANCEL_QUERY = 9
//
//suspend fun PlayerState.sendQuery(prompt: String, vararg answers: String) {
//    send {
//        println("sending to ${this@sendQuery.id}")
//        int(OUT_QUERY)
//        string(prompt)
//        int(answers.size)
//        answers.forEach { string(it) }
//    }
//}
//
//
//context(ServerMatchState)
//suspend fun Connection.handleConnection() {
//    var nextId = -1
//    try {
//        if (read.int() != IN_JOIN) {
//            close(); return
//        }
//        nextId = players.nextClearBit(0)
//        playerStates[nextId].apply {
//            id = nextId
//            connections[id] = this@handleConnection
//            CoroutineScope(Dispatchers.Default).launch {
//                try { channel.consumeEach { write.it() } }
//                catch(throwable: Throwable) {
//                    println("this 3?")
//                    throwable.printStackTrace()
//                } finally {
//                    playerStates[nextId].apply {
//                        exit()
//                        clear()
//                    }
//                }
//            }
//            join()
//            players.set(id)
//            while (true) when (read.int()) {
//                IN_POS -> {
//                    timePos = timeMillis
//                    lastPos.set(nextPos)
//                    nextPos.set(read.vec2f())
//                    broadcast {
//                        if (it == id) return@broadcast
//                        int(OUT_POS)
//                        int(id)
//                        vec2f(nextPos)
//                    }
//                }
//                IN_SHOOT -> {
//                    println("Shoot(${id}}")
//                    shoot(read.vec2f())
//                }
//                IN_CONFIRM_RESPAWN -> {
//                    println("Respawn(${id}}")
//                    respawn()
//                }
//                IN_RESPOND_QUERY -> {
//                    println("Respond(${id}}")
//                    val mapIndex = read.int()
//                    responses[id] = mapIndex
//                    respondants.set(id)
//                }
//                IN_CALL_VOTE -> {
//                    voters.set(id)
//                    println("Vote(${id})")
//                    if (voters.cardinality() < players.cardinality() || querying) continue
//                    voters.clear()
//                    querying = true
//                    respondants.clear()
//                    players.forEach {
//                        responses[it] = -1
//                        playerStates[it].sendQuery("Vote for a map", *Maps.mapNames.toTypedArray())
//                    }
//                    CoroutineScope(Dispatchers.IO).launch {
//                        println("start vote delay")
//                        delay(5.seconds)
//                        println("end vote delay")
//                        querying = false
//                        val counts = Array(Maps.mapNames.size) { 0 }
//                        var max = map
//                        respondants.forEach {
//                            val response = responses[it]
//                            println("player ${it} responded ${response}")
//                            if (response !in Maps.mapNames.indices) return@forEach
//                            if (++counts[response] > counts[max]) max = response
//                        }
//                        try {
//                            if (max == map) {
//                                broadcast {
//                                    int(OUT_CANCEL_QUERY)
//                                }
//                                return@launch
//                            }
//                            map = max
//                            println("SwapMap(next = ${Maps.mapNames[map]})")
//                            broadcast {
//                                int(OUT_CANCEL_QUERY)
//                                int(OUT_MAP)
//                                map(Maps[map])
//                                int(OUT_TRY_RESPAWN)
//                            }
//                        } catch(throwable: Throwable) {
//                            println("this 2?")
//                            throwable.printStackTrace()
//                        } finally {
//                            playerStates[nextId].apply {
//                                exit()
//                                clear()
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    } catch(throwable: Throwable) {
//        println("this 1?")
//        throwable.printStackTrace()
//    } finally {
//        playerStates[nextId].apply {
//            exit()
//            clear()
//        }
//    }
//}
//
//context(ServerMatchState)
//suspend fun PlayerState.exit() {
//    println("Exit($id)")
//    if (id == -1) return
//    connections[id]?.close()
//    players.clear(id)
//    connections[id] = null
//    broadcast {
//        int(OUT_EXIT)
//        int(id)
//    }
//}
//
//context(ServerMatchState)
//suspend fun PlayerState.join() {
//    println("Join($id)")
//    alive = true
//    health = 1f
//    send {
//        int(id)
//        int(mode)
//        vec2f(spawn(map, mode))
//        map(Maps[map])
//        players.forEach {
//            int(OUT_JOIN)
//            int(it)
//        }
//    }
//    broadcast {
//        int(OUT_JOIN)
//        int(id)
//    }
//}
//
//context(ServerMatchState)
//suspend fun PlayerState.die() {
//    println("Die(${id})")
//    broadcast {
//        int(OUT_DIE)
//        int(id)
//    }
//    if (mode == FFA) CoroutineScope(Dispatchers.IO).launch {
//        println("start try respawn")
//        delay(5.seconds)
//        try {
//            send { int(OUT_TRY_RESPAWN) }
//        } catch(throwable: Throwable) {
//            println("this 4?")
//            throwable.printStackTrace()
//        } finally {
//            playerStates[id].apply {
//                exit()
//                clear()
//            }
//        }
//        println("end try respawn")
//    }
//}
//
//context(ServerMatchState)
//suspend fun PlayerState.respawn() {
//    alive = true
//    health = 1f
//    broadcast {
//        int(OUT_RESPAWN)
//        int(id)
//        vec2f(spawn(map, mode))
//    }
//}
//
//context(ServerMatchState)
//suspend fun PlayerState.shoot(dir: Vector2f) {
//    val lerped = pos
//    broadcast {
//        if (id == it) return@broadcast
//        int(OUT_SHOOT)
//        vec2f(lerped)
//        vec2f(dir)
//    }
//    val playerHit = raycast<Int>(lerped, dir.mul(0.33f)) {
//        val intersected = players.first { playerId ->
//            val player = playerStates[playerId]
//            if (mode == SD && terrorist == player.terrorist) return@first false
//            val interpolated = player.pos
//            val min = Vector2f(interpolated.x - TILE_RADIUS.x, interpolated.y - TILE_RADIUS.y)
//            val max = Vector2f(interpolated.x + TILE_RADIUS.x, interpolated.y + TILE_RADIUS.y)
//            it.x > min.x && it.x < max.x && it.y > min.y && it.y < max.y && playerId != id
//        }
//        if (intersected != -1) collision = intersected
//        intersected != -1
//    }
//    if (!playerHit.result) return
//    val blockHit = Maps[map].world.tiledRaycast(Maps[map].worldSize, lerped, dir, TiledRay())
//    if (playerHit.distance > blockHit.distance) {
//        return
//    }
//    val hitPlayer = playerStates[playerHit.collision!!]
//    if (!hitPlayer.alive) {
//        return
//    }
//    println(hitPlayer.health)
//    hitPlayer.health -= 0.3f
//    if (hitPlayer.health <= 0f) {
//        hitPlayer.alive = false
//        hitPlayer.die()
//    }
//}