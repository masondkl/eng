package me.mason.client

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import me.mason.shared.*
import me.mason.sockets.Connection
import org.joml.Vector2f
import java.util.*

const val OUT_JOIN = 0
const val OUT_SHOOT = 1
const val OUT_POS = 2
const val OUT_CONFIRM_RESPAWN = 3
const val OUT_RESPOND_QUERY = 4
const val OUT_CALL_VOTE = 5

const val IN_JOIN = 0
const val IN_EXIT = 1
const val IN_MAP = 2
const val IN_POS = 3
const val IN_SHOOT = 4
const val IN_DIE = 5
const val IN_TRY_RESPAWN = 6
const val IN_RESPAWN = 7
const val IN_QUERY = 8
const val IN_CANCEL_QUERY = 9

class Query(var type: Int = -1, var prompt: String = "", var answers: Array<String> = emptyArray())
interface ClientMatchState : MatchState {
    var id: Int
    var connection: Connection?
    var map: TileMap?
    val bullets: BitSet
    val bulletStates: Array<Bullet>
    val query: Query
    suspend fun send(block: suspend Connection.() -> Unit)
}

context(Window, ClientMatchState)
suspend fun Connection.connect() {
    int(OUT_JOIN)
    id = int()
    mode = int()
    val pos = vec2f()
    camera.set(pos); move()
    playerStates[id].apply {
        lastPos.set(pos)
        nextPos.set(pos)
        timePos = timeMillis
    }
    map = map()
    join(id)
    coroutineScope {
        launch {
            while(open) when(read.int()) {
                IN_JOIN -> join(int())
                IN_EXIT -> players.clear(id)
                IN_SHOOT -> shoot(vec2f(), vec2f())
                IN_DIE -> {
                    playerStates[int()].apply {
                        alive = false
                    }
                }
                IN_TRY_RESPAWN -> send { int(OUT_CONFIRM_RESPAWN) }
                IN_RESPAWN -> respawn(int(), vec2f())
                IN_POS -> {
                    val id = int()
                    val inPos = vec2f()
                    playerStates[id].apply {
                        lastPos.set(nextPos)
                        nextPos.set(inPos)
                        timePos = timeMillis
                    }
                }
                IN_MAP -> map = map()
                IN_QUERY -> query()
                IN_CANCEL_QUERY -> {
                    println("cancelled")
                    query.type = -1
                }
            }
        }
    }
}

suspend fun ClientMatchState.query() {
//    sendQuery(VOTE_MAP, "Vote for a map", *Maps.mapNames.toTypedArray())
    connection?.apply {
        println("hey")
        val type = int()
        println("type: ${type}")
        val prompt = string()
        println("prompt: ${prompt}")
        val answerCount = int()
        println("answerCount: ${answerCount}")
        val answers = Array(answerCount) { "" }
        for (i in 0 until answerCount) {
            answers[i] = string()
            println("answer: ${answers[i]}")
        }
        query.type = type
        query.prompt = prompt
        query.answers = answers
    }
}

suspend fun ClientMatchState.respondQuery(type: Int, answer: Int) {
    connection?.apply {
        send {
            int(OUT_RESPOND_QUERY)
            println("type: ${type}")
            println("answer: ${answer}")
            int(type)
            int(answer)
        }
    }
}

fun ClientMatchState.join(id: Int) = playerStates[id].run {
    println("${id} joined")
    this.id = id
    health = 1f
    alive = true
    planting = false
    defusing = false
    terrorist = false
    players.set(id)
}
fun ClientMatchState.shoot(pos: Vector2f, dir: Vector2f) {
    val nextBullet = bullets.nextClearBit(0)
    bullets.set(nextBullet)
    bulletStates[nextBullet].apply {
        start.set(pos.copy())
        this.pos.set(pos.copy())
        this.dir.set(dir)
    }
}
context(Window)
fun ClientMatchState.respawn(id: Int, pos: Vector2f) {
    if (this.id == id) {
        camera.set(pos)
        move()
    }
    playerStates[id].apply {
        this.pos.set(pos)
        lastPos.set(pos)
        nextPos.set(pos)
        timePos = timeMillis
        alive = true
    }
}