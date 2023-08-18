package me.mason.server

import me.mason.shared.*
import org.joml.Vector2f

context(ServerMatchState)
suspend fun PlayerState.exit() {
    println("Exit($id)")
    broadcast {
        int(OUT_EXIT)
        int(id)
    }
}

context(ServerMatchState)
suspend fun PlayerState.join() {
    println("Join($id)")
    send {
        int(OUT_JOIN)
        int(id)
    }
    broadcast {
        int(OUT_JOIN)
        int(id)
    }
}

context(ServerMatchState)
suspend fun PlayerState.die() {
    broadcast {
        int(OUT_DIE)
        int(id)
    }
}

context(ServerMatchState)
suspend fun PlayerState.respawn() {
    broadcast {
        int(OUT_RESPAWN)
        int(id)
    }
}

context(ServerMatchState)
suspend fun PlayerState.shoot(dir: Vector2f) {
    println("Shoot($id)")
    val lerped = lerpPos(LERP_POS_RATE)
    broadcast {
        if (id == it) return@broadcast
        int(OUT_SHOOT)
        vec2f(lerped)
        vec2f(dir)
    }
    val playerHit = raycast<Int>(lerped, dir.mul(0.33f)) {
        val intersected = players.first { playerId ->
            val player = states[playerId]
            if (mode == SD && terrorist == player.terrorist) return@first false
            val interpolated = player.lerpPos(LERP_POS_RATE)
            val min = Vector2f(interpolated.x - TILE_RADIUS.x, interpolated.y - TILE_RADIUS.y)
            val max = Vector2f(interpolated.x + TILE_RADIUS.x, interpolated.y + TILE_RADIUS.y)
            it.x > min.x && it.x < max.x && it.y > min.y && it.y < max.y && playerId != id
        }
        if (intersected != -1) collision = intersected
        intersected != -1
    }
    if (!playerHit.result) return
    val blockHit = Maps[map].world.tiledRaycast(Maps[map].worldSize, lerped, dir)
    if (blockHit.result && playerHit.distance > blockHit.distance) return
    val hitPlayer = states[playerHit.collision!!]
    if (!hitPlayer.alive) return
    hitPlayer.health -= 0.3f
    if (hitPlayer.health < 0f) {
        hitPlayer.alive = false
        hitPlayer.die()
    }
}

//context(ServerMatchState)
//suspend fun PlayerState.shoot(dir: Vector2f) {
//    println("Shoot($id)")
////    println("Player ${id} shot")
////    val dir = vec2f()
////    val pos = interpolated(id)
////    broadcast { playerId, player ->
////        if (playerId == id) return@broadcast
////        player.byte(OUT_SHOOT)
////        player.vec2f(pos)
////        player.vec2f(dir)
////    }
////    val blockHit = maps[map].world.tiledRaycast(maps[map].worldSize, pos, dir)
//    val playerHit = raycast<Int>(pos, dir * 0.33f) {
//        val intersected = players.first { player ->
//            if (MODE == SD2 && terrorists[id] == terrorists[player]) return@first false
//            val interpolated = interpolated(player)
//            val min = interpolated - TILE_SIZE / 2f
//            val max = interpolated + TILE_SIZE / 2f
//            it.x > min.x && it.x < max.x && it.y > min.y && it.y < max.y && player != id
//        }
//        if (intersected != -1) data = intersected
//        intersected != -1
//    }
////    if (!playerHit.result || playerHit.distance > blockHit.distance) continue
////    val targetId = playerHit.data!!
////    if (healths[targetId] < 0f) continue
////    healths[targetId] -= 0.3f
////    if (healths[targetId] > 0f) continue
////    broadcast { _, player ->
////        player.byte(OUT_DIE)
////        player.int(targetId)
////    }
//    broadcast broadcast@{ id ->
//        if (this@shoot.id == id) return@broadcast
//        int(OUT_SHOOT)
//        vec2f(dir)
//
//    }
//}
