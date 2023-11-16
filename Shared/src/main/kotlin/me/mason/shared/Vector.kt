package me.mason.shared

import org.joml.Vector2f
import org.joml.Vector2i
import java.lang.Math.sqrt
import kotlin.math.*

fun Vector2f.copy() = Vector2f(x, y)
fun Vector2i.copy() = Vector2i(x, y)
fun Vector2f.int() = Vector2i(x.toInt(), y.toInt())
fun Vector2i.float() = Vector2f(x.toFloat(), y.toFloat())
