package me.mason.client

import me.mason.client.floor
import java.lang.Math.sqrt
import kotlin.math.*

@JvmInline
value class FloatVector(private val array: FloatArray = FloatArray(2) { 0f }) {
    var x: Float
        get() = array[0]
        set(value) = array.set(0, value)
    var y: Float
        get() = array[1]
        set(value) = array.set(1, value)
}

fun fvec(): FloatVector = vec(0f, 0f)
fun vec(x: Float, y: Float) = FloatVector(floatArrayOf(x, y))
fun vec(value: Float) = vec(value, value)
fun vec(value: FloatVector) = vec(value.x, value.y)

operator fun FloatVector.plusAssign(b: FloatVector) { x += b.x; y += b.y }
operator fun FloatVector.minusAssign(b: FloatVector) { x -= b.x; y -= b.y }
operator fun FloatVector.divAssign(b: FloatVector) { x /= b.x; y /= b.y }
operator fun FloatVector.timesAssign(b: FloatVector) { x *= b.x; y *= b.y }
operator fun FloatVector.rem(b: FloatVector) = vec(x % b.x, y % b.y)
operator fun FloatVector.rem(v: Float) = vec(x % v, y % v)

operator fun FloatVector.plusAssign(v: Float) { x += v; y += v }
operator fun FloatVector.minusAssign(v: Float) { x -= v; y -= v }
operator fun FloatVector.divAssign(v: Float) { x /= v; y /= v }
operator fun FloatVector.timesAssign(v: Float) { x *= v; y *= v }
operator fun FloatVector.remAssign(b: FloatVector) { x %= b.x; y %= b.y }
operator fun FloatVector.remAssign(v: Float) { x %= v; y %= v }

fun FloatVector.rounded(): FloatVector {
    return try {
        vec(x.roundToInt().toFloat(), y.roundToInt().toFloat())
    } catch (_: Throwable) { fvec() }
}
fun FloatVector.round() { x = x.roundToInt().toFloat(); y = y.roundToInt().toFloat() }

fun FloatVector.floored() = vec(floor(x), floor(y))
fun FloatVector.floor() { x = floor(x); y = floor(y) }

fun FloatVector.ceiled() = vec(ceil(x), ceil(y))
fun FloatVector.ceil() { x = ceil(x); y = ceil(y) }

fun FloatVector.min(b: FloatVector) = vec(min(x, b.x), min(y, b.y))
fun FloatVector.max(b: FloatVector) = vec(max(x, b.x), max(y, b.y))

operator fun FloatVector.plus(b: FloatVector) = vec(x, y).also { it += b }
operator fun FloatVector.plus(v: Float) = vec(x, y).also { it += v }
operator fun FloatVector.minus(b: FloatVector) = vec(x, y).also { it -= b }
operator fun FloatVector.minus(v: Float) = vec(x, y).also { it -= v }
operator fun FloatVector.div(b: FloatVector) = vec(x, y).also { it /= b }
operator fun FloatVector.div(v: Float) = vec(x, y).also { it /= v }
operator fun FloatVector.times(b: FloatVector) = vec(x, y).also { it *= b }
operator fun FloatVector.times(v: Float) = vec(x, y).also { it *= v }
operator fun FloatVector.unaryMinus() = vec(-x, -y)

fun FloatVector.distance(b: FloatVector) = sqrt((x - b.x).pow(2) + (y - b.y).pow(2))
fun FloatVector.distanceX(b: FloatVector) = abs(x - b.x)
fun FloatVector.distanceY(b: FloatVector) = abs(y - b.y)
fun FloatVector.set(b: FloatVector) { x = b.x; y = b.y }
fun FloatVector.int() = vec(x.toInt(), y.toInt())
fun FloatVector.clone() = vec(x, y)

@JvmInline
value class IntVector(private val array: IntArray = IntArray(2) { 0 }) {
    var x: Int
        get() = array[0]
        set(value) = array.set(0, value)
    var y: Int
        get() = array[1]
        set(value) = array.set(1, value)
}

fun ivec(): IntVector = vec(0, 0)
fun vec(x: Int, y: Int) = IntVector(intArrayOf(x, y))
fun vec(value: Int) = vec(value, value)
fun vec(value: IntVector) = vec(value.x, value.y)

operator fun IntVector.plusAssign(b: IntVector) { x += b.x; y += b.y }
operator fun IntVector.minusAssign(b: IntVector) { x -= b.x; y -= b.y }
operator fun IntVector.divAssign(b: IntVector) { x /= b.x; y /= b.y }
operator fun IntVector.timesAssign(b: IntVector) { x *= b.x; y *= b.y }

operator fun IntVector.plusAssign(v: Int) { x += v; y += v }
operator fun IntVector.minusAssign(v: Int) { x -= v; y -= v }
operator fun IntVector.divAssign(v: Int) { x /= v; y /= v }
operator fun IntVector.timesAssign(v: Int) { x *= v; y *= v }

fun IntVector.min(b: IntVector) = vec(min(x, b.x), min(y, b.y))
fun IntVector.max(b: IntVector) = vec(max(x, b.x), max(y, b.y))

operator fun IntVector.plus(b: IntVector) = vec(x, y).also { it += b }
operator fun IntVector.plus(v: Int) = vec(x, y).also { it += v }
operator fun IntVector.minus(b: IntVector) = vec(x, y).also { it -= b }
operator fun IntVector.minus(v: Int) = vec(x, y).also { it -= v }
operator fun IntVector.div(b: IntVector) = vec(x, y).also { it /= b }
operator fun IntVector.div(v: Int) = vec(x, y).also { it /= v }
operator fun IntVector.times(b: IntVector) = vec(x, y).also { it *= b }
operator fun IntVector.times(v: Int) = vec(x, y).also { it *= v }
operator fun IntVector.unaryMinus() = vec(-x, -y)

fun IntVector.distance(b: IntVector) = sqrt((x - b.x).toDouble().pow(2) + (y - b.y).toDouble().pow(2)).toFloat()
fun IntVector.set(b: IntVector) { x = b.x; y = b.y }
fun IntVector.float() = vec(x.toFloat(), y.toFloat())
fun IntVector.clone() = vec(x, y)