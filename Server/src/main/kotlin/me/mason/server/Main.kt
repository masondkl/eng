package me.mason.server

import kotlinx.coroutines.isActive
import me.mason.sockets.Read
import me.mason.sockets.Write
import me.mason.sockets.accept
import java.lang.System.currentTimeMillis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CopyOnWriteArraySet

val SRV_IN_MOVE = 0.toByte()
val SRV_IN_REQUEST_ID = 1.toByte()
val SRV_IN_SHOOT = 2.toByte()
val SRV_IN_KILL = 3.toByte()


val SRV_OUT_DISCONNECT = 32.toByte()
val SRV_OUT_MOVE = 33.toByte()
val SRV_OUT_ID = 34.toByte()
val SRV_OUT_SHOOT = 35.toByte()
val SRV_OUT_DIE = 36.toByte()
val SRV_OUT_RESET = 37.toByte()

suspend fun main() {
    var eid = 0
    val dead = CopyOnWriteArraySet<Int>()
    val connections = CopyOnWriteArraySet<Int>()
    val broadcasts = ConcurrentHashMap<Int, ConcurrentLinkedQueue<suspend Write.(Int) -> (Unit)>>()
    val moves = ConcurrentHashMap<Int, Long>()
    fun broadcast(block: suspend Write.(Int) -> (Unit)) = connections.forEach {
        broadcasts[it]!!.add(block)
    }
    accept(9999) { context ->
        try {
            var id = 0
            while (context.isActive && open) {
                when (read.byte()) {
                    SRV_IN_REQUEST_ID -> {
                        id = eid++.also {
                            println("$it connected")
                            write.byte(SRV_OUT_ID)
                            write.int(it)
                            connections.add(it)
                            broadcasts[it] = ConcurrentLinkedQueue()
                        }
                    }
                    SRV_IN_MOVE -> {
                        val moving = read.int()
                        val x = read.float()
                        val y = read.float()
                        moves[moving] = currentTimeMillis()
                        broadcast {
                            if (it != moving) {
                                byte(SRV_OUT_MOVE)
                                int(moving)
                                float(x); float(y)
                            }
                        }
                    }
                    SRV_IN_SHOOT -> {
                        val shooting = read.int()
                        val posX = read.float(); val posY = read.float()
                        val dirX = read.float(); val dirY = read.float()
                        broadcast {
                            if (it != shooting) {
                                byte(SRV_OUT_SHOOT)
                                int(shooting)
                                float(posX); float(posY)
                                float(dirX); float(dirY)
                            }
                        }
                    }
                    SRV_IN_KILL -> {
                        val killer = read.int()
                        val died = read.int()
                        broadcast {
                            if (it != killer) {
                                byte(SRV_OUT_DIE)
                                int(died)
                            }
                        }
                        dead.add(died)
                        if (dead.size >= connections.size - 1) {
                            dead.clear()
                            broadcast {
                                byte(SRV_OUT_RESET)
                            }
                        }
                    }
                }
                while (broadcasts[id]!!.isNotEmpty()) {
                    broadcasts[id]!!.poll().invoke(write, id)
                }
                val now = currentTimeMillis()
                moves.filter { (_, last) -> now - last > 5000 }.forEach { (id, _) ->
                    moves.remove(id)
                    connections.remove(id)
                    broadcast {
                        byte(SRV_OUT_DISCONNECT)
                        int(id)
                    }
                }
            }
        } catch(_: Throwable) { }
    }
//    GlobalScope.launch(dispatcher) {
//        while (provider.isOpen) {
//            println("wtf")
//            provider.accept(address).apply {
//                launch {
//                    try {
//                        println("a")
//                        if (read.byte() != SRV_IN_REQUEST_ID) error("Did not receive id request from client")
//                        println("b")
//                        val id = eid++.also {
//                            write.byte(SRV_OUT_ID)
//                            write.int(it)
//                            connections.add(it)
//                            broadcasts[it] = ConcurrentLinkedQueue()
//                        }
//                        println("Player $id connected")
//                        launch {
//                            try {
//                                while (isActive && isOpen) {
//                                    when (read.byte()) {
//                                        SRV_IN_MOVE -> {
//                                            val moving = read.int()
//                                            val x = read.float()
//                                            val y = read.float()
//                                            moves[moving] = currentTimeMillis()
//                                            broadcast {
//                                                if (it != moving) {
//                                                    byte(SRV_OUT_PLAYER_MOVE)
//                                                    int(moving)
//                                                    float(x); float(y)
//                                                }
//                                            }
//                                        }
////                                SRV_IN_END -> {
////                                    write.byte(SRV_OUT_END)
////                                    connections.remove(id)
////                                    broadcasts.remove(id)
////                                    broadcast {
////                                        if (it != id) {
////                                            byte(SRV_OUT_PLAYER_DISCONNECT)
////                                            int(id)
////                                        }
////                                    }
////                                    break
////                                }
//                                    }
//                                    while (broadcasts[id]!!.isNotEmpty()) {
//                                        broadcasts[id]!!.poll().invoke(write, id)
//                                    }
//                                }
//                            } catch (_: ClosedChannelException) {
//                                println("closedchannel")
//                            } catch (throwable: Throwable) {
//                                throwable.printStackTrace(); close()
//                            }
//                        }
//                        println(isActive)
//                        println(isOpen)
//                    } catch (_: ClosedChannelException) {
//                        println("closedchannel")
//                    } catch (throwable: Throwable) {
//                        throwable.printStackTrace(); close()
//                    }
//                }
//            }
//        }
//    }
}