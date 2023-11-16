package me.mason.client

import me.mason.client.engine.Window
import org.joml.Vector2f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW

fun Window.mouseWorld(): Vector2f {
    val mouseX = BufferUtils.createDoubleBuffer(1)
    val mouseY = BufferUtils.createDoubleBuffer(1)
    val width = BufferUtils.createIntBuffer(1)
    val height = BufferUtils.createIntBuffer(1)
    GLFW.glfwGetCursorPos(window, mouseX, mouseY)
    GLFW.glfwGetWindowSize(window, width, height)
    return Vector2f(
        (mouseX.get().toFloat() - 640f) / (1280 / 80f) + camera.x,
        (height.get().toFloat() - mouseY.get().toFloat() - 360f) / (720f / 45f) + camera.y
    )
}

fun Window.mouseUi(): Vector2f {
    val mouseX = BufferUtils.createDoubleBuffer(1)
    val mouseY = BufferUtils.createDoubleBuffer(1)
    val width = BufferUtils.createIntBuffer(1)
    val height = BufferUtils.createIntBuffer(1)
    GLFW.glfwGetCursorPos(window, mouseX, mouseY)
    GLFW.glfwGetWindowSize(window, width, height)
    return Vector2f(
        (mouseX.get().toFloat() - 640f) / (1280 / 80f),
        (height.get().toFloat() - mouseY.get().toFloat() - 360f) / (720f / 45f)
    )
}