package me.mason.client

import kotlinx.coroutines.runBlocking
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.BufferUtils.createIntBuffer
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback.createPrint
import org.lwjgl.openal.AL
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil.NULL
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

interface Window {
    val keyEvent: ArrayList<suspend (Int, Int) -> (Unit)>
    val mouseEvent: ArrayList<suspend (Int, Int) -> (Unit)>
    val keyState: BitSet
    val mouseState: BitSet
    val window: Long
    var dim: Vector2i
    var title: Any
    var camera: Vector2f
    val projection: Matrix4f
    val view: Matrix4f
    val uiProjection: Matrix4f
    val uiView: Matrix4f
    val matrix: FloatArray
    val delta: Observable<Float>
    val elapsed: Observable<Float>
    suspend fun fixed(fps: suspend () -> (Int), static: Boolean = false): Observable<Float>
    suspend fun fixed(fps: Int): Observable<Float>
    fun move()
}

suspend fun window(block: suspend Window.() -> (Unit)) {
    val device: Long = alcOpenDevice(null as ByteBuffer?)
    val deviceCaps = ALC.createCapabilities(device)
    val context: Long = alcCreateContext(device, null as IntBuffer?)
    alcMakeContextCurrent(context)
    AL.createCapabilities(deviceCaps)
    val dim = Vector2i(1280, 720)
    createPrint(System.err).set()
    if (!glfwInit()) error("Unable to initialize GLFW")
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
    val id = glfwCreateWindow(dim.x, dim.y, "", NULL, NULL)
    if (id == NULL) error("Failed to create the GLFW window")
    val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
    glfwSetWindowPos(
        id,
        (videoMode.width() - dim.x) / 2,
        (videoMode.height() - dim.y) / 2
    )
    glfwMakeContextCurrent(id)
    glfwSwapInterval(0)
    glfwShowWindow(id)
    createCapabilities()
    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    glfwSetWindowSize(id, dim.x, dim.y)
    glViewport(0, 0, dim.x, dim.y)
    glfwSetWindowSizeCallback(id) { _, nextWidth, nextHeight ->
        glfwSetWindowSize(id, nextWidth, nextHeight)
        glViewport(0, 0, nextWidth, nextHeight)
    }
    val windowDelta = Observable { 0f }
    val windowElapsed = Observable { 0f }
    val window = object : Window {
        override val projection = Matrix4f().setOrtho(-40f, 40f, -22.5f, 22.5f, 0f, 100f)
        override val view = Matrix4f().identity().lookAt(
            Vector3f(0f, 0f, 20f),
            Vector3f(0f, 0f, -1f),
            Vector3f(0f, 1f, 0f)
        )
        override val uiProjection = Matrix4f().setOrtho(-40f, 40f, -22.5f, 22.5f, 0f, 100f)
        override val uiView = Matrix4f().identity().lookAt(
            Vector3f(0f, 0f, 20f),
            Vector3f(0f, 0f, -1f),
            Vector3f(0f, 1f, 0f)
        )
        override val matrix = FloatArray(16)
        override val delta: Observable<Float> = windowDelta
        override val elapsed: Observable<Float> = windowElapsed
        override suspend fun fixed(fps: suspend () -> Int, static: Boolean): Observable<Float> {
            var remainder = 0f
            var rate = 1f / fps()
            val fixedDelta = Observable { 0f }
            delta {
                if (!static) rate = 1f / fps()
                val from = remainder
                val to = from + it
                remainder = if (to > rate) {
                    fixedDelta(to)
                    to % rate
                } else to
            }
            return fixedDelta
        }
        override suspend fun fixed(fps: Int): Observable<Float> = fixed({ fps }, static = true)
        override fun move() {
            view.identity().lookAt(
                Vector3f((camera.x * 16f).toInt() / 16f, (camera.y * 16f).toInt() / 16f, 20f),
                Vector3f((camera.x * 16f).toInt() / 16f, (camera.y * 16f).toInt() / 16f, -1f),
                Vector3f(0f, 1f, 0f)
            )
        }
        override val keyEvent = ArrayList<suspend (Int, Int) -> Unit>()
        override val mouseEvent = ArrayList<suspend (Int, Int) -> Unit>()
        override val keyState = BitSet()
        override val mouseState = BitSet()
        override val window = id
        override var camera = Vector2f(0f, 0f)
            set(value) {
                field = value
                move()
            }
        override var dim: Vector2i
            get() {
                val width = createIntBuffer(1)
                val height = createIntBuffer(1)
                glfwGetWindowSize(id, width, height)
                return Vector2i(width.get(), height.get())
            }
            set(value) {
                glfwSetWindowSize(id, value.x, value.y)
                glViewport(0, 0, value.x, value.y)
            }
        override var title: Any = ""
            set(value) {
                glfwSetWindowTitle(id, value.toString())
                field = value
            }
    }
    glfwSetKeyCallback(id) { _, code, _, action, _ ->
        runBlocking { window.keyEvent.forEach { it(code, action) } }
        if (action == GLFW_PRESS) window.keyState.set(code)
        else if (action == GLFW_RELEASE) window.keyState.clear(code)
    }
    glfwSetMouseButtonCallback(id) { _, code, action, _ ->
        runBlocking { window.mouseEvent.forEach { it(code, action) } }
        if (action == GLFW_PRESS) window.mouseState.set(code)
        else if (action == GLFW_RELEASE) window.mouseState.clear(code)
    }
    block(window)
    var elapsed = 0L
    var delta: Long
    var last = System.nanoTime()
    while (!glfwWindowShouldClose(id)) {
        val now = System.nanoTime()
        delta = now - last
        elapsed += delta
        last = now
        val deltaSeconds = delta / 1000000000f
        val elapsedSeconds = elapsed / 1000000000f
        windowDelta(deltaSeconds, notify = false)
        windowElapsed(elapsedSeconds, notify = false)
        windowDelta.notify()
        windowElapsed.notify()
    }
    exitProcess(0)
}