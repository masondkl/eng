package me.mason.eng

import org.joml.Matrix4f

interface Camera {
    val projection: Matrix4f
    val view: Matrix4f
    var position: FloatVector
    fun move()
}