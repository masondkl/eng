package me.mason.server

import com.github.exerosis.mynt.SocketProvider
import com.github.exerosis.mynt.base.Address
import com.github.exerosis.mynt.base.Write
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import me.mason.shared.*
import org.joml.Vector2f
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.ClosedChannelException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.locks.LockSupport
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

val SERVICE = Executors.newCachedThreadPool()
val DISPATCHER = SERVICE.asCoroutineDispatcher()

const val IN_JOIN = 0
const val IN_SHOOT = 1
const val IN_POS = 2
const val IN_CONFIRM_RESPAWN = 3
const val IN_RESPOND_QUERY = 4
const val IN_CALL_VOTE = 5

const val OUT_JOIN = 0
const val OUT_EXIT = 1
const val OUT_MAP = 2
const val OUT_POS = 3
const val OUT_SHOOT = 4
const val OUT_DIE = 5
const val OUT_TRY_RESPAWN = 6
const val OUT_RESPAWN = 7
const val OUT_QUERY = 8
const val OUT_CANCEL_QUERY = 9

const val FFA = 0
const val TDM = 1
const val SD = 2

interface Match {
    var mode: Int
    var querying: Boolean
    val voters: BitSet
    val responses: IntArray
    val players: Container<ServerPlayer>
    var map: Int
}

suspend fun main() = runBlocking<Unit>(DISPATCHER) {
    val group = AsynchronousChannelGroup.withThreadPool(SERVICE)
    val provider = SocketProvider(9999, group)
    val address = Address("127.0.0.1", 9999)
    object : Match {
        override var mode = FFA
        override var querying = false
        override val voters = BitSet(256)
        override val responses = IntArray(256) { -1 }
        override val players: Container<ServerPlayer> = Container(256) {
            object : ServerPlayer, Player by Player() {
                override lateinit var channel: Channel<suspend Write.() -> (Unit)>
                override suspend fun send(block: suspend Write.() -> (Unit)) {
                    channel.trySend(block)
                }
            }
        }
        override var map = Maps.random()
    }.apply match@{
        while (provider.isOpen && isActive) {
            val connection = provider.accept(address)
            val (nextId, player) = players.next()
            println("Join(${nextId})")
            player.apply {
                id = nextId
                channel = Channel(UNLIMITED)
                timePos = 0L
                lastPos.set(0f, 0f)
                nextPos.set(0f, 0f)
                pos.set(0f, 0f)
                lastDir = 0f
                nextDir = 0f
                health = 1f
                alive = true
                planting = false
                defusing = false
                t = false
            }
            launch {
                try {
                    player.channel.consumeEach {
                        connection.write.it()
                    }
                } catch (throwable: Throwable) {
                    if (throwable !is ClosedChannelException) throwable.printStackTrace()
                } finally {
                    println("ExitWrite(${player.id})")
                    players.clear(player.id)
                }
            }
            launch {
                try {
                    connection.read.apply {
                        player.send {
                            int(nextId)
                            map(Maps[map])
                            vec2f(player.spawn(map, mode))
                            players.forEach {
                                if (nextId == id) return@forEach
                                println("sending ${id} join to ${nextId}")
                                int(OUT_JOIN)
                                int(id)
                            }
                        }
                        players.forEach { send {
                            if (nextId == id) return@send
                            println("sending ${nextId} join to ${id}")
                            int(OUT_JOIN)
                            int(nextId)
                        } }
                        while (provider.isOpen && isActive) {
                            when (int()) {
                                IN_POS -> {
                                    player.apply {
                                        timePos = timeMillis
                                        lastPos.set(nextPos)
                                        nextPos.set(vec2f())
                                        lastDir = nextDir
                                        nextDir = float()
                                    }
//                                    println("Pos(${player.id}, ${player.nextPos.x}, ${player.nextPos.y})")
//                                    val min = Vector2f(player.pos.x - TILE_RADIUS.x, player.pos.y - TILE_RADIUS.y)
//                                    val max = Vector2f(player.pos.x + TILE_RADIUS.x, player.pos.y + TILE_RADIUS.y)
//                                    val corners = arrayOf(
//                                        min, max,
//                                        Vector2f(max.x, min.y),
//                                        Vector2f(min.x, max.y)
//                                    )
                                    players.forEach { send {
                                        if (id == player.id) return@send
                                        int(OUT_POS)
                                        int(player.id)
                                        vec2f(player.nextPos)
                                        float(player.dir)
//                                        val to = players.states[broadcastId]
////                                        if (corners.any { corner ->
//                                            val theta = atan2(to.pos.y - player.pos.y, to.pos.x - player.pos.x) + PI.toFloat()
//                                            val dirVec = Vector2f(cos(theta), sin(theta))
//                                            val playerHit = raycast<ServerPlayer>(to.pos, Vector2f(dirVec).mul(0.33f)) {
//                                                contains(it, player.pos, TILE_SIZE)
//                                            }
//                                            if (!playerHit.result) return@broadcast
//                                            val map = Maps[map]
//                                            val blockHit = map.world.tiledRaycast(map.worldSize, player.pos, dirVec, TiledRay())
//                                            println("playerhit: ${playerHit.distance}\nblockhit${blockHit.distance}")
//                                        if (playerHit.distance < blockHit.distance) {
////                                        }) {
//                                            println("gg")
//                                            int(OUT_POS)
//                                            int(player.id)
//                                            vec2f(player.nextPos)
//                                            float(player.dir)
//                                        }
                                    } }
                                }
                                IN_SHOOT -> {
                                    println("Shoot(${player.id})")
                                    val dir = vec2f()
                                    println("dist: ${dir.x * 2f}, ${dir.y * 2f}")
                                    val start = Vector2f(player.nextPos).add(dir.x * 2f, dir.y * 2f)
                                    players.forEach { send {
                                        if (id == player.id) return@send
                                        int(OUT_SHOOT)
                                        vec2f(start)
                                        vec2f(dir)
                                    } }
                                    val playerHit = raycast<ServerPlayer>(start, dir.mul(0.33f)) {
                                        val intersected = players.firstOrNull {
                                            if (mode == SD && player.t == t) return@firstOrNull false
                                            contains(it, pos, TILE_SIZE) && player.id != id
                                        }
                                        if (intersected != null) collision = intersected
                                        intersected != null
                                    }
                                    if (!playerHit.result) continue
                                    val map = Maps[map]
                                    val blockHit = map.world.tiledRaycast(map.worldSize, start, dir, TiledRay())
                                    if (playerHit.distance > blockHit.distance) continue
                                    val hitPlayer = playerHit.collision!!
                                    if (!hitPlayer.alive) continue
                                    println("Hit(${hitPlayer.id})")
                                    hitPlayer.health -= 0.3f
                                    if (hitPlayer.health <= 0f) {
                                        hitPlayer.alive = false
                                        println("Die(${hitPlayer.id})")
                                        println("\nplayers: ")
                                        players.forEach {
                                            println(id)
                                        }
                                        println()
                                        players.forEach { send {
                                            println("sending ${hitPlayer.id} die to $id")
                                            int(OUT_DIE)
                                            int(hitPlayer.id)
                                        } }
                                        if (mode == FFA) CoroutineScope(Dispatchers.IO).launch {
                                            delay(1.seconds)
                                            hitPlayer.send { int(OUT_TRY_RESPAWN) }
                                        }
                                    }
                                }
                                IN_CONFIRM_RESPAWN -> {
                                    println("Respawn(${player.id}}")
                                    player.apply {
                                        alive = true
                                        health = 1f
                                    }
                                    players.forEach { send {
                                        int(OUT_RESPAWN)
                                        int(player.id)
                                        vec2f(player.spawn(map, mode))
                                    } }
                                }
                                IN_RESPOND_QUERY -> {
                                    val response = int()
                                    if (response in Maps.mapNames.indices) {
                                        println("Respond(${player.id}, ${Maps.mapNames[response]})")
                                        responses[player.id] = response
                                    }
                                }
                                IN_CALL_VOTE -> {
                                    voters.set(player.id)
                                    println("Vote(${player.id})")
                                    if (voters.cardinality() < players.cardinality() || querying) continue
                                    voters.clear()
                                    querying = true
                                    responses.fill(-1)
                                    players.forEach {
                                        responses[id] = -1
                                        send {
                                            println("Query(${id})")
                                            int(OUT_QUERY)
                                            string("Vote for a map")
                                            int(Maps.mapNames.size)
                                            Maps.mapNames.forEach { string(it) }
                                        }
                                    }
                                    CoroutineScope(Dispatchers.IO).launch delay@{
                                        delay(5.seconds)
                                        querying = false
                                        val counts = Array(Maps.mapNames.size) { 0 }
                                        var max = map
                                        players.forEach {
                                            val response = responses[id]
                                            if (response == -1) return@forEach
                                            if (++counts[response] > counts[max]) max = response
                                        }
                                        if (max == map) {
                                            players.forEach { send {
                                                int(OUT_CANCEL_QUERY)
                                            } }
                                            return@delay
                                        }
                                        val last = map
                                        map = max
                                        println("SwapMap(last = ${Maps.mapNames[last]}, next = ${Maps.mapNames[map]})")
                                        players.forEach { send {
                                            int(OUT_CANCEL_QUERY)
                                            int(OUT_MAP)
                                            map(Maps[map])
                                            int(OUT_TRY_RESPAWN)
                                        } }
                                    }
                                }
                            }
                        }
                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                } finally {
                    println("ExitRead(${player.id})")
                    players.clear(player.id)
                }
            }
        }
    }
}

//interface ServerMatchState : MatchState {
//    var querying: Boolean
//    val voters: BitSet
//    val respondants: BitSet
//    val responses: Array<Int>
//    val connections: Array<Connection?>
//    var map: Int
//    suspend fun broadcast(block: suspend Write.(Int) -> (Unit))
//}
//
//val SERVICE = Executors.newCachedThreadPool()
//val DISPATCHER = SERVICE.asCoroutineDispatcher()
//
//fun main() = runBlocking<Unit>(DISPATCHER) {
//    object : ServerMatchState {
//        override var mode = FFA
//        override var querying = false
//        override val players = BitSet(256)
//        override val voters = BitSet()
//        override val respondants = BitSet()
//        override val responses = Array(256) { -1 }
//        override val connections = Array<Connection?>(256) { null }
//        override var map = Maps.random()
//        override val playerStates = Array<PlayerState>(256) {
//            object : PlayerState {
//                override val channel: Channel<suspend Write.() -> (Unit)> = Channel(
//                    capacity = Int.MAX_VALUE,
//                    onUndeliveredElement = {
//                        println("didnt deliver")
//                    }
//                )
//                val lerpPos = Vector2f()
//                override var id = -1
//                override var timePos = 0L
//                override val lastPos = Vector2f()
//                override val nextPos = Vector2f()
//                override val pos: Vector2f get() {
//                    val t = ((timeMillis - timePos) / LERP_POS_RATE)
//                    return lerpPos.set(
//                        lastPos.x + (nextPos.x - lastPos.x) * t,
//                        lastPos.y + (nextPos.y - lastPos.y) * t,
//                    )
//                }
//                override var health = 1f
//                override var alive = true
//                override var planting = false
//                override var defusing = false
//                override var terrorist = false
//                override suspend fun send(block: suspend Write.() -> (Unit)) {
//                    channel.trySend(block)
//                }
//            }
//        }
//        override suspend fun broadcast(block: suspend Write.(Int) -> (Unit)) = players.forEach {
//            playerStates[it].send { block(it) }
//        }
//    }.apply {
//        val group = AsynchronousChannelGroup.withThreadPool(SERVICE)
//        val provider = SocketProvider(9999, group)
//        val address = Address("127.0.0.1", 9999)
//        while (provider.isOpen && isActive) {
//            val connection = provider.accept(address)
//            launch {
//                try {
//                    println("handle connection")
//                    connection.handleConnection()
//                } catch (throwable: Throwable) {
//                    if (throwable !is ClosedChannelException) throwable.printStackTrace()
//                }
//            }
//        }
//    }
//}