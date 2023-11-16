package me.mason.shared

import com.github.exerosis.mynt.base.Read
import com.github.exerosis.mynt.base.Write
import org.joml.Vector2f
import org.joml.Vector2i
import java.util.ArrayList

suspend fun Write.string(string: String) {
    val bytes = string.toByteArray()
    int(bytes.size)
    bytes(bytes, bytes.size, 0)
}
suspend fun Read.string(): String {
    val size = int()
    val array = ByteArray(size)
    bytes(array, size, 0)
    return String(array)
}

suspend fun Write.vec2f(vec: Vector2f) { float(vec.x); float(vec.y) }
suspend fun Read.vec2f() = Vector2f(float(), float())

suspend fun Write.vec2i(vec: Vector2i) { int(vec.x); int(vec.y) }
suspend fun Read.vec2i() = Vector2i(int(), int())

suspend fun Write.map(map: TileMap) {
    string(map.name)
    int(map.worldSize)
    int(map.walls.size)
    int(map.plantBounds.size)
    for (wall in map.walls) {
        vec2f(wall.min)
        vec2f(wall.max)
    }
    for (bounds in map.plantBounds) {
        vec2f(bounds.min)
        vec2f(bounds.max)
    }
    bytes(map.world, map.worldSize * map.worldSize, 0)
}
suspend fun Read.map(): TileMap {
    val name = string()
    val worldSize = int()
    val wallCount = int()
    val plantBoundsCount = int()
    val walls = ArrayList<Bounds>()
    (0 until wallCount).forEach { _ -> walls.add(Bounds(vec2f(), vec2f())) }
    val plantBounds = ArrayList<Bounds>()
    (0 until plantBoundsCount).forEach { _ -> plantBounds.add(Bounds(vec2f(), vec2f())) }
    val bytes = ByteArray(worldSize * worldSize)
    bytes(bytes, worldSize * worldSize, 0)
    val colliders = Array(worldSize * worldSize) {
        if (bytes[it] in SOLIDS) {
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
    return TileMap(name, worldSize, walls, plantBounds, bytes, colliders, corners)
}