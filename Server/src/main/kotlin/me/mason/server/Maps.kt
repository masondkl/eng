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
            TileMap(mapNames[map], worldSize, walls, world)
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
            if (world[tile] in SOLIDS || players.any { it != id && states[it].lerpPos(LERP_POS_RATE).distance(tilePos) < 30.0f })
                continue
            set((tile % worldSize).toFloat(), (tile / worldSize).toFloat())
        }
    } else if (terrorist) Maps.terroristSpawns[map].random()
    else Maps.counterSpawns[map].random()
}