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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

val SERVICE = Executors.newCachedThreadPool()
val DISPATCHER = SERVICE.asCoroutineDispatcher()

const val IN_SHOOT = 0
const val IN_POS = 1
const val IN_CONFIRM_RESPAWN = 2
const val IN_RESPOND_QUERY = 3
const val IN_CALL_VOTE = 4
const val IN_TRY_PLANT = 5
const val IN_CANCEL_PLANT = 6

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
const val OUT_CONFIRM_PLANT = 10
const val OUT_CANCEL_PLANT = 11
const val OUT_PLANT = 12

const val FFA = 0
const val TDM = 1
const val SD = 2



interface Match {
    var mode: Int
    var querying: Boolean
    var ending: AtomicBoolean
    var planted: AtomicBoolean
    val voters: BitSet
    val responses: IntArray
    val players: MutableList<ServerPlayer>
    var map: Int
}

fun ServerPlayer(): ServerPlayer = object : ServerPlayer, Player by Player() {
    override val channel = Channel<suspend Write.() -> (Unit)>(UNLIMITED)
    override suspend fun send(block: suspend Write.() -> (Unit)) {
        channel.trySend(block)
    }
}

suspend fun MutableList<ServerPlayer>.send(block: suspend Write.(ServerPlayer) -> (Unit)) {
    forEach { it.send { block(it) } }
}

suspend fun main() = runBlocking<Unit>(DISPATCHER) {
    val group = AsynchronousChannelGroup.withThreadPool(SERVICE)
    val provider = SocketProvider(9999, group)
    val address = Address("127.0.0.1", 9999)
    var entityId = 0
    var plantId = 0
    val plantCancels = ConcurrentHashMap<Int, Int>()
    object : Match {
        override var mode = SD
        override var querying = false
        override var ending = AtomicBoolean(false)
        override var planted = AtomicBoolean(false)
        override val voters = BitSet(256)
        override val responses = IntArray(256) { -1 }
        override val players = Collections.synchronizedList(ArrayList<ServerPlayer>())
        override var map = Maps.random()
    }.apply match@{
        while (provider.isOpen && isActive) {
            val connection = provider.accept(address)
            val player = ServerPlayer().apply {
                id = entityId++
                if (mode == SD && players.any { it.alive && it.ct } || players.any { it.alive && it.t }) {
                    alive = false
                }
                if (!ending.get() && (players.none { it.alive && it.ct } || players.none { it.alive && it.t })) {
                    CoroutineScope(Dispatchers.IO).launch {
                        ending.set(true)
                        delay(5.seconds)
                        ending.set(false)
                        players.forEach { other ->
                            other.ct = players.count { it.ct } <= players.count { it.t }
                            other.send { int(OUT_TRY_RESPAWN) }
                        }
                    }
                }
            }
            players.add(player)
            println("Join(${player.id})")
            launch {
                try {
                    player.channel.consumeEach {
                        connection.write.it()
                    }
                } catch (throwable: Throwable) {
                    if (throwable !is ClosedChannelException) throwable.printStackTrace()
                } finally {
                    println("ExitWrite(${player.id})")
                    players.remove(player)
                }
            }
            launch {
                try {
                    connection.read.apply {
                        player.send {
                            int(player.id)
                            map(Maps[map])
                            vec2f(player.spawn(map, mode))
                            players.forEach {
                                if (player.id == it.id) return@forEach
                                println("sending ${it.id} join to ${player.id}")
                                int(OUT_JOIN)
                                int(it.id)
                            }
                        }
                        players.send {
                            if (player.id == it.id) return@send
                            println("sending ${player.id} join to ${it.id}")
                            int(OUT_JOIN)
                            int(player.id)
                        }
                        if (!player.alive) players.send {
                            int(OUT_DIE)
                            int(player.id)
                        }
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
//                                    println
//                                    ("Pos(${player.id}, ${player.nextPos.x}, ${player.nextPos.y})")
//                                    val min = Vector2f(player.pos.x - TILE_RADIUS.x, player.pos.y - TILE_RADIUS.y)
//                                    val max = Vector2f(player.pos.x + TILE_RADIUS.x, player.pos.y + TILE_RADIUS.y)
//                                    val corners = arrayOf(
//                                        min, max,
//                                        Vector2f(max.x, min.y),
//                                        Vector2f(min.x, max.y)
//                                    )
//                                    println("out_pos: ${player.id}")
                                    players.send {
                                        if (player.id == it.id) return@send
//                                        println("sending to: ${it.id}")
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
                                    }
                                }
                                IN_SHOOT -> {
                                    println("Shoot(${player.id})")
                                    val dir = vec2f()
                                    val start = Vector2f(player.nextPos)
                                    players.send {
                                        if (player.id == it.id) return@send
                                        int(OUT_SHOOT)
                                        vec2f(Vector2f(start).add(dir))
                                        vec2f(dir)
                                    }
                                    val playerHit = raycast<ServerPlayer>(start, Vector2f(dir).mul(0.1f), max = 100f) { ray ->
                                        val intersected = players.firstOrNull {
                                            if (mode == SD && player.t == it.t) return@firstOrNull false
                                            contains(ray, it.pos, TILE_DIM) && player.id != it.id
                                        }
                                        if (intersected != null) collision = intersected
                                        intersected != null
                                    }
                                    if (playerHit.distance == 100f) continue
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
                                        players.send {
                                            println("sending die to ${it.id}")
                                            int(OUT_DIE)
                                            int(hitPlayer.id)
                                        }
                                        if (mode == SD) {
                                            if (!ending.get()) {
                                                if (players.none { it.alive && it.ct }) {
                                                    //t wins
                                                } else if (players.none { it.alive && it.t }) {
                                                    //ct wins
                                                }
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    ending.set(true)
                                                    delay(5.seconds)
                                                    ending.set(false)
                                                    players.forEach {
                                                        it.send { int(OUT_TRY_RESPAWN) }
                                                    }
                                                }
                                            }
                                        } else CoroutineScope(Dispatchers.IO).launch {
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
                                    players.send {
                                        int(OUT_RESPAWN)
                                        int(player.id)
                                        vec2f(player.spawn(map, mode))
                                    }
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
                                    if (voters.cardinality() < players.size || querying) continue
                                    voters.clear()
                                    querying = true
                                    responses.fill(-1)
                                    players.forEach {
                                        responses[it.id] = -1
                                        it.send {
                                            println("Query(${it.id})")
                                            int(OUT_QUERY)
                                            string("Vote for a map")
                                            int(Maps.mapNames.size)
                                            Maps.mapNames.forEach { name -> string(name) }
                                        }
                                    }
                                    CoroutineScope(Dispatchers.IO).launch delay@{
                                        delay(5.seconds)
                                        querying = false
                                        val counts = Array(Maps.mapNames.size) { 0 }
                                        var max = map
                                        players.forEach {
                                            val response = responses[it.id]
                                            if (response == -1) return@forEach
                                            if (++counts[response] > counts[max]) max = response
                                        }
                                        if (max == map) {
                                            players.send {
                                                int(OUT_CANCEL_QUERY)
                                            }
                                            return@delay
                                        }
                                        val last = map
                                        map = max
                                        println("SwapMap(last = ${Maps.mapNames[last]}, next = ${Maps.mapNames[map]})")
                                        players.send {
                                            int(OUT_CANCEL_QUERY)
                                            int(OUT_MAP)
                                            map(Maps[map])
                                            int(OUT_TRY_RESPAWN)
                                        }
                                    }
                                }
                                IN_TRY_PLANT -> {
                                    if (Maps[map].plantBounds.any { contains(player.nextPos, it.min, it.max) }) player.send {
                                        int(OUT_CONFIRM_PLANT)
                                        plantCancels[plantId++] = 0
                                        CoroutineScope(Dispatchers.Default).launch {
                                            delay(500)
                                            if (plantCancels[plantId] == 0) {

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                } finally {
                    println("ExitRead(${player.id})")
                    players.remove(player)
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