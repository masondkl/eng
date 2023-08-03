package me.mason.client

import org.joml.*
import org.lwjgl.BufferUtils.createIntBuffer
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback.createPrint
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.stb.STBImage.stbi_image_free
import org.lwjgl.stb.STBImage.stbi_load
import org.lwjgl.system.MemoryUtil.NULL
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.io.path.readBytes

val DEFAULT_SIZE = Vector2i(1280, 720)

fun Int.compileShader(src: String): Int {
    glShaderSource(this, src)
    glCompileShader(this)
    if (glGetShaderi(this, GL_COMPILE_STATUS) == GL_FALSE) {
        val len = glGetShaderi(this, GL_INFO_LOG_LENGTH)
        error(glGetShaderInfoLog(this, len))
    }; return this
}

class Shader(vert: Path, frag: Path, vararg val attrs: Int) {
    private val vertId = glCreateShader(GL_VERTEX_SHADER).compileShader(String(vert.readBytes()))
    private val fragId = glCreateShader(GL_FRAGMENT_SHADER).compileShader(String(frag.readBytes()))
    val program = glCreateProgram()
    val stride = attrs.sum()
    init {
        glAttachShader(program, vertId)
        glAttachShader(program, fragId)
        glLinkProgram(program)
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            val len = glGetProgrami(program, GL_INFO_LOG_LENGTH)
            error(glGetProgramInfoLog(program, len))
        }
    }
}

fun atlas(path: Path) = glGenTextures().also { id ->
    glBindTexture(GL_TEXTURE_2D, id)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    val width = createIntBuffer(1)
    val height = createIntBuffer(1)
    val channels = createIntBuffer(1)
    val image = stbi_load(path.absolutePathString(), width, height, channels, 0)
    if (image != null) {
        when(channels.get(0)) {
            3 -> glTexImage2D(
                GL_TEXTURE_2D, 0, GL_RGB, width.get(0), height.get(0),
                0, GL_RGB, GL_UNSIGNED_BYTE, image
            )
            4 -> glTexImage2D(
                GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0),
                0, GL_RGBA, GL_UNSIGNED_BYTE, image
            )
            else -> error("Unknown number of channels")
        }
    } else error("Could not load image")
    stbi_image_free(image)
}

class Buffer(val shader: Shader, size: Int = 200000, private val draw: Buffer.(IntRange) -> (Unit)) {
    val data: FloatArray = FloatArray(size)
    val vao: Int = glGenVertexArrays()
    val vbo: Int = glGenBuffers()
    val triangle = shader.stride * 3
    val quad = shader.stride * 6
    fun vec2f(pos: Int, vector: Vector2f) {
        data[pos] = vector.x
        data[pos + 1] = vector.y
    }
    fun vec3f(pos: Int, vector: Vector3f) {
        data[pos] = vector.x
        data[pos + 1] = vector.y
        data[pos + 2] = vector.z
    }
    fun vec4f(pos: Int, vector: Vector4f) {
        data[pos] = vector.x
        data[pos + 1] = vector.y
        data[pos + 2] = vector.z
        data[pos + 3] = vector.w
    }
    fun float(pos: Int, float: Float) {
        data[pos] = float
    }
    fun clear(range: IntRange) {
        data.fill(0f, range.first, range.last)
    }
    fun draw(range: IntRange) = draw(this, range)
    init {
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, size * 4L, GL_DYNAMIC_DRAW)
        var offset = 0L
        shader.attrs.forEachIndexed { index, attrSize ->
            glVertexAttribPointer(index, attrSize, GL_FLOAT, false, shader.stride * 4, offset * 4L)
            offset += attrSize
        }
    }
}

val CORNERS = arrayOf(
    Vector2i(1, 1),
    Vector2i(1, 0),
    Vector2i(0, 1),
    Vector2i(1, 0),
    Vector2i(0, 0),
    Vector2i(0, 1),
)

fun Buffer.textureQuad(index: Int, pos: Vector2f, dim: Vector2f, uvPos: Vector2i, uvDim: Vector2i, z: Int = 0) {
    val topLeftX = pos.x - (dim.x / 2f)
    val topLeftY = pos.y - (dim.y / 2f)
    for (vertex in 0 until 6) {
        val offset = index + vertex * shader.stride
        val uvX = uvPos.x + uvDim.x * CORNERS[vertex].x
        val uvY = uvPos.y + uvDim.y * CORNERS[vertex].y
        float(offset, topLeftX + dim.x * CORNERS[vertex].x)
        float(offset + 1, topLeftY + dim.y * CORNERS[vertex].y)
        float(offset + 2, z.toFloat())
        float(offset + 3, uvX + uvY * 512.toFloat())
    }
}

fun Buffer.colorTriangle(index: Int, a: Vector2f, b: Vector2f, c: Vector2f, color: Vector4f, z: Int = 0) {
    vec3f(index + 0, Vector3f(a, z.toFloat()))
    vec4f(index + 3, color)

    vec3f(index + 7, Vector3f(b, z.toFloat()))
    vec4f(index + 10, color)

    vec3f(index + 14, Vector3f(c, z.toFloat()))
    vec4f(index + 17, color)
}

fun Buffer.fovTriangle(index: Int, a: Vector2f, b: Vector2f, c: Vector2f, z: Int = 0) {
    vec3f(index + 0, Vector3f(a, z.toFloat()))
    vec3f(index + 3, Vector3f(b, z.toFloat()))
    vec3f(index + 6, Vector3f(c, z.toFloat()))
}


fun Buffer.colorQuad(index: Int, pos: Vector2f, dim: Vector2f, color: Vector4f, z: Int = 0) {
    val topLeftX = pos.x - (dim.x / 2f)
    val topLeftY = pos.y - (dim.y / 2f)
    for (vertex in 0 until 6) {
        val offset = index + vertex * shader.stride
        float(offset, topLeftX + dim.x * CORNERS[vertex].x)
        float(offset + 1, topLeftY + dim.y * CORNERS[vertex].y)
        float(offset + 2, z.toFloat())
        vec4f(offset + 3, color)
    }
}

interface Window {
    val keyEvent: ArrayList<(Int, Int) -> (Unit)>
    val mouseEvent: ArrayList<(Int, Int) -> (Unit)>
    val keys: BitSet
    val buttons: BitSet

    val window: Long
    var dim: Vector2i
    var title: Any

    var camera: Vector2f
    val projection: Matrix4f
    val view: Matrix4f

    fun move()

    fun onClosed(block: () -> (Unit))
    fun onTick(block: (Float, Float) -> (Unit))
    fun fixed(fps: Int): AtomicBoolean
}

fun window(block: Window.() -> (Unit)) {
    createPrint(System.err).set()
    if (!glfwInit()) error("Unable to initialize GLFW")
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
    val id = glfwCreateWindow(DEFAULT_SIZE.x, DEFAULT_SIZE.y, "", NULL, NULL)
    if (id == NULL) error("Failed to create the GLFW window")
    val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
    glfwSetWindowPos(
        id,
        (videoMode.width() - DEFAULT_SIZE.x) / 2,
        (videoMode.height() - DEFAULT_SIZE.y) / 2
    )
    glfwMakeContextCurrent(id)
    glfwSwapInterval(0)
    glfwShowWindow(id)
    createCapabilities()
    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    glfwSetWindowSize(id, DEFAULT_SIZE.x, DEFAULT_SIZE.y)
    glViewport(0, 0, DEFAULT_SIZE.x, DEFAULT_SIZE.y)
    glfwSetWindowSizeCallback(id) { _, nextWidth, nextHeight ->
        glfwSetWindowSize(id, nextWidth, nextHeight)
        glViewport(0, 0, nextWidth, nextHeight)
    }
    val onClosed = ArrayList<() -> (Unit)>()
    val onTick = ArrayList<(Float, Float) -> (Unit)>()
    object : Window {
        val remainders = HashMap<Int, Float>()
        override val projection = Matrix4f().setOrtho(-40f, 40f, -22.5f, 22.5f, 0f, 100f)
        override val view = Matrix4f().identity().lookAt(
            Vector3f(0f, 0f, 20f),
            Vector3f(0f, 0f, -1f),
            Vector3f(0f, 1f, 0f)
        )
        override fun move() {
            view.identity().lookAt(
                Vector3f((camera.x * 32f).toInt() / 32f, (camera.y * 32f).toInt() / 32f, 20f),
                Vector3f((camera.x * 32f).toInt() / 32f, (camera.y * 32f).toInt() / 32f, -1f),
                Vector3f(0f, 1f, 0f)
            )
        }
        override fun onClosed(block: () -> Unit) = onClosed.plusAssign(block)
        override val keyEvent = ArrayList<(Int, Int) -> Unit>()
        override val mouseEvent = ArrayList<(Int, Int) -> Unit>()
        override val keys = BitSet()
        override val buttons = BitSet()
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
        override fun onTick(block: (Float, Float) -> (Unit)) = onTick.plusAssign(block)
        override fun fixed(fps: Int): AtomicBoolean {
            val isFixed = AtomicBoolean(false)
            val rate = 1f / fps
            onTick.plusAssign { delta, elapsed ->
                val from = remainders.getOrPut(fps) { 0f }
                val to = (from + delta) % rate
                if (to < from) isFixed.set(true)
                else isFixed.set(false)
                remainders[fps] = to
            }
            return isFixed
        }
        init {
            glfwSetKeyCallback(window) { _, code, _, action, _ ->
                keyEvent.forEach { it(code, action) }
                if (action == GLFW_PRESS) keys.set(code)
                else if (action == GLFW_RELEASE) keys.clear(code)
            }
            glfwSetMouseButtonCallback(window) { _, code, action, _ ->
                mouseEvent.forEach { it(code, action) }
                if (action == GLFW_PRESS) buttons.set(code)
                else if (action == GLFW_RELEASE) buttons.clear(code)
            }
            block()
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
                onTick.forEach { it(deltaSeconds, elapsedSeconds) }
            }
            onClosed.forEach { it() }
        }
    }
}