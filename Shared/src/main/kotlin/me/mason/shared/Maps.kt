package me.mason.shared

import org.joml.Vector2f
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
            val plantBounds = ArrayList<Bounds>()
            for (i in 0 until int) walls.add(Bounds(
                Vector2f(float, float),
                Vector2f(float, float)
            ))
            for (i in 0 until int) counterSpawns[map].add(Vector2f(float, float))
            for (i in 0 until int) terroristSpawns[map].add(Vector2f(float, float))
            for (i in 0 until int) plantBounds.add(Bounds(
                Vector2f(float, float),
                Vector2f(float, float)
            ))
            val colliders = Array(worldSize * worldSize) {
                if (world[it] in SOLIDS) {
                    val x = it % worldSize
                    val y = it / worldSize
                    Collider(Vector2f(x.toFloat(), y.toFloat()), TILE_DIM)
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
            TileMap(mapNames[map], worldSize, walls, plantBounds, world, colliders, corners)
        }
    }
    fun random() = maps.indices.random()
    operator fun get(map: Int) = maps[map]
}