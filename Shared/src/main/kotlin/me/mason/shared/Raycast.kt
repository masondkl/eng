package me.mason.shared

import org.joml.Vector2f
import org.joml.Vector2i
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt


data class TiledRay(
    val hit: Vector2f = Vector2f(),
    val tile: Vector2i = Vector2i(),
    var distance: Float = 0f
)

fun ByteArray.tiledRaycast(
    worldSize: Int,
    start: Vector2f,
    dir: Vector2f,
    result: TiledRay,
    max: Float = RENDER_RAD.toFloat()
): TiledRay {
    val currentTileCoord = Vector2i(start.x.roundToInt(), start.y.roundToInt())
    val bottomLeft = Vector2f(currentTileCoord.x - 0.5f, currentTileCoord.y - 0.5f)
    val rayMaxStepSize = Vector2f(abs(1 / dir.x), abs(1 / dir.y))
    val rayStepLength = Vector2f(0f, 0f)
    val mapStep = Vector2i(0, 0)
    val tempDir = Vector2f()

    if (dir.x < 0) {
        mapStep.x = -1
        rayStepLength.x = (start.x - bottomLeft.x) * rayMaxStepSize.x
    } else {
        mapStep.x = 1
        rayStepLength.x = (bottomLeft.x + 1 - start.x) * rayMaxStepSize.x
    }

    if (dir.y < 0) {
        mapStep.y = -1
        rayStepLength.y = (start.y - bottomLeft.y) * rayMaxStepSize.y
    } else {
        mapStep.y = 1
        rayStepLength.y = (bottomLeft.y + 1 - start.y) * rayMaxStepSize.y
    }

    var fDistance = 0f
    var currentCoord = Vector2f(0.0f, 0.0f)

    while (fDistance < max) {
        currentCoord = start.copy()
        if (rayStepLength.x < rayStepLength.y) {
            currentCoord.add(tempDir.set(dir).mul(rayStepLength.x))
            currentTileCoord.x += mapStep.x
            fDistance = rayStepLength.x
            rayStepLength.x += rayMaxStepSize.x
        } else {
            currentCoord.add(tempDir.set(dir).mul(rayStepLength.y))
            currentTileCoord.y += mapStep.y
            fDistance = rayStepLength.y
            rayStepLength.y += rayMaxStepSize.y
        }
        val index = (currentTileCoord.y * worldSize) + currentTileCoord.x
        if(currentTileCoord.x >= worldSize || currentTileCoord.x < 0 ||
            currentTileCoord.y >= worldSize || currentTileCoord.y < 0 ||
            this[index] in SOLIDS
        ) break
    }
//    quad(shader, idx, if (hitSomething) currentCoord else start + dir * max, TILE_SIZE_VEC, PLAYER, PLAYER_UV_SIZE_VEC)
    return result.apply {
        distance = fDistance
        hit.set(currentCoord)
        tile.set(currentTileCoord)
    }
}

interface Ray<T> {
    val hit: Vector2f
    val distance: Float
    var collision: T?
}

fun <T> raycastBlocking(
    start: Vector2f,
    dir: Vector2f,
    max: Float = RENDER_RAD.toFloat(),
    test: Ray<T>.(Vector2f) -> (Boolean)
): Ray<T> {
    val position = start.copy()
    val ray = object : Ray<T> {
        override var distance = 0f
        override var collision: T? = null
        override val hit = position
    }
    if (ray.test(position)) {
        return ray
    }
    var distance = sqrt((position.x - start.x).pow(2) + (position.y - start.y).pow(2))
    while (distance < max) {
        position.add(dir)
        distance = sqrt((position.x - start.x).pow(2) + (position.y - start.y).pow(2))
        ray.distance = distance
        if (ray.test(position)) return ray
    }
    ray.collision = null
    ray.distance = max
    return ray
}

suspend fun <T> raycast(
    start: Vector2f,
    dir: Vector2f,
    max: Float,
    test: suspend Ray<T>.(Vector2f) -> (Boolean)
): Ray<T> {
    val position = start.copy()
    val ray = object : Ray<T> {
        override var distance = 0f
        override var collision: T? = null
        override val hit = position
    }
    if (ray.test(position)) {
        return ray
    }
    var distance = sqrt((position.x - start.x).pow(2) + (position.y - start.y).pow(2))
    while (distance < max) {
        position.add(dir)
        distance = sqrt((position.x - start.x).pow(2) + (position.y - start.y).pow(2))
        ray.distance = distance
        if (ray.test(position)) return ray
    }
    ray.collision = null
    ray.distance = max
    return ray
}