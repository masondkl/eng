//package me.mason.client
//
//import com.github.exerosis.mynt.base.Connection
//import com.github.exerosis.mynt.base.Write
//import kotlinx.coroutines.channels.Channel
//import kotlinx.coroutines.coroutineScope
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import me.mason.shared.*
//import org.joml.Vector2f
//import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
//import java.nio.channels.ClosedChannelException
//import java.util.*
//
//const val OUT_JOIN = 0
//const val OUT_SHOOT = 1
//const val OUT_POS = 2
//const val OUT_CONFIRM_RESPAWN = 3
//const val OUT_RESPOND_QUERY = 4
//const val OUT_CALL_VOTE = 5
//
//const val IN_JOIN = 0
//const val IN_EXIT = 1
//const val IN_MAP = 2
//const val IN_POS = 3
//const val IN_SHOOT = 4
//const val IN_DIE = 5
//const val IN_TRY_RESPAWN = 6
//const val IN_RESPAWN = 7
//const val IN_QUERY = 8
//const val IN_CANCEL_QUERY = 9
//
//class Query(var enabled: Boolean = false, var prompt: String = "", var answers: Array<String> = emptyArray())
//interface ClientMatchState : MatchState {
//    val channel: Channel<suspend Write.() -> (Unit)>
//    var id: Int
//    var map: TileMap?
//    val bullets: BitSet
//    val bulletStates: Array<Bullet>
//    val query: Query
//    suspend fun send(block: suspend Write.() -> Unit)
//}
//
//context(Window, ClientMatchState)
//suspend fun Connection.handleConnection() {
//    var readId = -1
//    try {
//        write.int(OUT_JOIN)
//        readId = read.int()
//        id = readId
//        mode = read.int()
//        val pos = read.vec2f()
//        camera.set(pos); move()
//        playerStates[id].apply {
//            lastPos.set(pos)
//            nextPos.set(pos)
//            timePos = timeMillis
//        }
//        map = read.map()
//        join(id)
//        while (isOpen && !glfwWindowShouldClose(this@Window.window)) when (read.int()) {
//            IN_JOIN -> {
//                val id = read.int()
//                println("this sends??: ${id}")
//                join(id)
//            }
//
//            IN_EXIT -> players.clear(id)
//            IN_SHOOT -> shoot(read.vec2f(), read.vec2f())
//            IN_DIE -> {
//                playerStates[read.int()].apply {
//                    alive = false
//                }
//            }
//
//            IN_TRY_RESPAWN -> {
//                println("writing confirm respawn")
//                send { write.int(OUT_CONFIRM_RESPAWN) }
//            }
//
//            IN_RESPAWN -> respawn(read.int(), read.vec2f())
//            IN_POS -> {
//                val id = read.int()
//                val inPos = read.vec2f()
//                playerStates[id].apply {
//                    lastPos.set(nextPos)
//                    nextPos.set(inPos)
//                    timePos = timeMillis
//                }
//            }
//
//            IN_MAP -> map = read.map()
//            IN_QUERY -> {
//                println("in query")
//                val prompt = read.string()
//                val answerCount = read.int()
//                val answers = Array(answerCount) { "" }
//                for (i in 0 until answerCount)
//                    answers[i] = read.string()
//                query.enabled = true
//                query.prompt = prompt
//                query.answers = answers
//            }
//
//            IN_CANCEL_QUERY -> {
//                println("cancelled")
//                query.enabled = false
//            }
//        }
//    } catch(throwable: Throwable) {
//        if (throwable !is ClosedChannelException) throwable.printStackTrace()
//    } finally {
//        close()
//    }
//}
//
//suspend fun ClientMatchState.respondQuery(answer: Int) {
//    send {
//        int(OUT_RESPOND_QUERY)
//        int(answer)
//    }
//}
//
//fun ClientMatchState.join(id: Int) = playerStates[id].run {
//    println("${id} joined")
//    this.id = id
//    health = 1f
//    alive = true
//    planting = false
//    defusing = false
//    terrorist = false
//    players.set(id)
//}
//fun ClientMatchState.shoot(pos: Vector2f, dir: Vector2f) {
//    val nextBullet = bullets.nextClearBit(0)
//    bullets.set(nextBullet)
//    bulletStates[nextBullet].apply {
//        start.set(pos.copy())
//        this.pos.set(pos.copy())
//        this.dir.set(dir)
//    }
//}
//context(Window)
//fun ClientMatchState.respawn(id: Int, pos: Vector2f) {
//    if (this.id == id) {
//        camera.set(pos)
//        move()
//    }
//    playerStates[id].apply {
//        this.pos.set(pos)
//        lastPos.set(pos)
//        nextPos.set(pos)
//        timePos = timeMillis
//        alive = true
//    }
//}