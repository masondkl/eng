package me.mason.client

import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.BufferUtils.createIntBuffer
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback.createPrint
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL20.*
import org.lwjgl.system.MemoryUtil.NULL
import kotlin.math.round

interface Window {
    val window: Long
    val camera: Camera
    var height: Int
    var width: Int
    var title: Any
}

const val DEFAULT_WIDTH = 1280
const val DEFAULT_HEIGHT = 720

fun window(block: Window.() -> (Unit)) {
    createPrint(System.err).set()
    check(glfwInit()) { "Unable to initialize GLFW" }
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
    val id = glfwCreateWindow(DEFAULT_WIDTH, DEFAULT_HEIGHT, "", NULL, NULL)
    if (id == NULL) throw RuntimeException("Failed to create the GLFW window")
    val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
    glfwSetWindowPos(
        id,
        (videoMode.width() - DEFAULT_WIDTH) / 2,
        (videoMode.height() - DEFAULT_HEIGHT) / 2
    )
//    glfwSetInputMode(id, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
    glfwMakeContextCurrent(id)
    glfwSwapInterval(0)
    glfwShowWindow(id)
    createCapabilities()
    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    glfwSetWindowSize(id, DEFAULT_WIDTH, DEFAULT_HEIGHT)
    glViewport(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT)
    glfwSetWindowSizeCallback(id) { _, nextWidth, nextHeight ->
        glfwSetWindowSize(id, nextWidth, nextHeight)
        glViewport(0, 0, nextWidth, nextHeight)
    }
    val camera = object : Camera {
        override val projection = Matrix4f().setOrtho(-40f, 40f, -22.5f, 22.5f, 0f, 100f)
        override val view = Matrix4f().identity().lookAt(
            Vector3f(0f, 0f, 20f),
            Vector3f(0f, 0f, -1f),
            Vector3f(0f, 1f, 0f)
        )
        override var position = fvec()
        override fun move() {
            val x = (position.x * 32f).toInt() / 32f
            val y = (position.y * 32f).toInt() / 32f
            view.identity().lookAt(
                Vector3f(x, y, 20f),
                Vector3f(x, y, -1f),
                Vector3f(0f, 1f, 0f)
            )
        }
    }
    val window = object : Window {
        override val window = id
        override val camera = camera
        override var height: Int
            get() {
                val width = createIntBuffer(1)
                val height = createIntBuffer(1)
                glfwGetWindowSize(id, width, height)
                return height[0]
            }
            set(value) = glfwSetWindowSize(id, width, value)
        override var width: Int
            get() {
                val width = createIntBuffer(1)
                val height = createIntBuffer(1)
                glfwGetWindowSize(id, width, height)
                return width[0]
            }
            set(value) = glfwSetWindowSize(id, value, height)
        override var title: Any = ""
            set(value) {
                glfwSetWindowTitle(id, value.toString())
                field = value
            }
    }; block(window)
}