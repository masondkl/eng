package me.mason.server

import me.mason.shared.*
import org.joml.Vector2f
import org.joml.Vector2i
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import kotlin.io.path.readBytes


val MAP_DIRECTORY: Path = Paths.get("maps")

object Maps {
    val mapNames = MAP_DIRECTORY.toFile().listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
    val counterSpawns = Array(mapNames.size) { ArrayList<Vector2f>() }
    val terroristSpawns = Array(mapNames.size) { ArrayList<Vector2f>() }
    val maps = Array(mapNames.size) { map ->
        ByteBuffer.wrap(MAP_DIRECTORY.resolve("${mapNames[map]}.map").readBytes()).run {
            val worldSize = int
            val world = ByteArray(worldSize * worldSize); get(world)
            val walls = ArrayList<Bounds>()
            for (i in 0 until int) walls.add(Bounds(
                Vector2f(float, float),
                Vector2f(float, float)
            ))
            for (i in 0 until int) counterSpawns[map].add(Vector2f(float, float))
            for (i in 0 until int) terroristSpawns[map].add(Vector2f(float, float))
            val colliders = Array(worldSize * worldSize) {
                if (world[it] in SOLIDS) {
                    val x = it % worldSize
                    val y = it / worldSize
                    Collider(Vector2f(x.toFloat(), y.toFloat()), TILE_SIZE)
                } else Collider(Vector2f(0f, 0f), Vector2f(0f, 0f))
            }
            val corners = Array(walls.size * 4) { Vector2f() }
            walls.indices.forEach {
                val wall = walls[it]
                val min = wall.min
                val max = wall.max
                corners[it * 4] = Vector2f(min.x, min.y)
                corners[it * 4 + 1] = Vector2f(max.x, min.y)
                corners[it * 4 + 2] = Vector2f(min.x, max.y)
                corners[it * 4 + 3] = Vector2f(max.x, max.y)
            }
            TileMap(mapNames[map], worldSize, walls, world, colliders, corners)
        }
    }
    fun random() = maps.indices.random()
    operator fun get(map: Int) = maps[map]
}

context(ServerMatchState)
fun PlayerState.spawn(map: Int, mode: Int) = Maps.maps[map].run {
    if (mode == FFA || mode == TDM) Vector2f(1f, 1f).apply {
        for (tile in (0 until worldSize * worldSize).shuffled()) {
            val tilePos = Vector2i(tile % worldSize, tile / worldSize).float()
            if (world[tile] in SOLIDS || players.any { it != id && playerStates[it].pos.distance(tilePos) < 30.0f })
                continue
            set((tile % worldSize).toFloat(), (tile / worldSize).toFloat())
        }
    } else if (terrorist) Maps.terroristSpawns[map].random()
    else Maps.counterSpawns[map].random()
}