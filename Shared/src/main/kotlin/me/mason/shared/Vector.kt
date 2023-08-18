package me.mason.shared

import org.joml.Vector2f
import org.joml.Vector2i
import java.lang.Math.sqrt
import kotlin.math.*

fun Vector2f.normal(): Vector2f {
    val mag = sqrt(x * x + y * y)
    if (mag == 0f) return this
    x /= mag
    y /= mag
    return this
}
fun Vector2f.copy() = Vector2f(x, y)
fun Vector2i.copy() = Vector2i(x, y)
fun Vector2f.int() = Vector2i(x.toInt(), y.toInt())
fun Vector2i.float() = Vector2f(x.toFloat(), y.toFloat())

//operator fun Vector2f.plusAssign(b: Vector2f) { x += b.x; y += b.y }
//operator fun Vector2f.minusAssign(b: Vector2f) { x -= b.x; y -= b.y }
//operator fun Vector2f.divAssign(b: Vector2f) { x /= b.x; y /= b.y }
//operator fun Vector2f.timesAssign(b: Vector2f) { x *= b.x; y *= b.y }
//operator fun Vector2f.rem(b: Vector2f) = Vector2f(x % b.x, y % b.y)
//operator fun Vector2f.rem(v: Float) = Vector2f(x % v, y % v)
//
//operator fun Vector2f.plusAssign(v: Float) { x += v; y += v }
//operator fun Vector2f.minusAssign(v: Float) { x -= v; y -= v }
//operator fun Vector2f.divAssign(v: Float) { x /= v; y /= v }
//operator fun Vector2f.timesAssign(v: Float) { x *= v; y *= v }
//operator fun Vector2f.remAssign(b: Vector2f) { x %= b.x; y %= b.y }
//operator fun Vector2f.remAssign(v: Float) { x %= v; y %= v }
//
//operator fun Vector2f.plus(b: Vector2f) = Vector2f(x, y).also { it += b }
//operator fun Vector2f.plus(v: Float) = Vector2f(x, y).also { it += v }
//operator fun Vector2f.minus(b: Vector2f) = Vector2f(x, y).also { it -= b }
//operator fun Vector2f.minus(v: Float) = Vector2f(x, y).also { it -= v }
//operator fun Vector2f.div(b: Vector2f) = Vector2f(x, y).also { it /= b }
//operator fun Vector2f.div(v: Float) = Vector2f(x, y).also { it /= v }
//operator fun Vector2f.times(b: Vector2f) = Vector2f(x, y).also { it *= b }
//operator fun Vector2f.times(v: Float) = Vector2f(x, y).also { it *= v }
//operator fun Vector2f.unaryMinus() = Vector2f(-x, -y)
//
//
//operator fun Vector2i.plusAssign(b: Vector2i) { x += b.x; y += b.y }
//operator fun Vector2i.minusAssign(b: Vector2i) { x -= b.x; y -= b.y }
//operator fun Vector2i.divAssign(b: Vector2i) { x /= b.x; y /= b.y }
//operator fun Vector2i.timesAssign(b: Vector2i) { x *= b.x; y *= b.y }
//
//operator fun Vector2i.plusAssign(v: Int) { x += v; y += v }
//operator fun Vector2i.minusAssign(v: Int) { x -= v; y -= v }
//operator fun Vector2i.divAssign(v: Int) { x /= v; y /= v }
//operator fun Vector2i.timesAssign(v: Int) { x *= v; y *= v }
//
//operator fun Vector2i.plus(b: Vector2i) = Vector2i(x, y).also { it += b }
//operator fun Vector2i.plus(v: Int) = Vector2i(x, y).also { it += v }
//operator fun Vector2i.minus(b: Vector2i) = Vector2i(x, y).also { it -= b }
//operator fun Vector2i.minus(v: Int) = Vector2i(x, y).also { it -= v }
//operator fun Vector2i.div(b: Vector2i) = Vector2i(x, y).also { it /= b }
//operator fun Vector2i.div(v: Int) = Vector2i(x, y).also { it /= v }
//operator fun Vector2i.times(b: Vector2i) = Vector2i(x, y).also { it *= b }
//operator fun Vector2i.times(v: Int) = Vector2i(x, y).also { it *= v }
//operator fun Vector2i.unaryMinus() = Vector2i(-x, -y)
//
//fun Vector2i.float() = Vector2f(x.toFloat(), y.toFloat())
//fun Vector2i.copy() = Vector2i(x, y)
