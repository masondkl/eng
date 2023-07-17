package me.mason.client

import org.joml.Matrix4f

interface Camera {
    val projection: Matrix4f
    val view: Matrix4f
    val position: FloatVector
    fun move()
}