package me.mason.client

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.mason.shared.*
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
import kotlin.io.path.absolutePathString
import kotlin.io.path.readBytes
import kotlin.io.path.readLines

val DEFAULT_SIZE = Vector2i(1280, 720)

interface Glyph {
    val x: Int
    val y: Int
    val width: Int
    val height: Int
    val offsetX: Int
    val offsetY: Int
    val advance: Int
}
interface Font {
    val size: Int
    val lineHeight: Int
    val chars: HashMap<Int, Glyph>
}
fun font(path: Path): Font {
    val lines = path.readLines()
    val font = object : Font {
        override val size = lines[0].trim().removePrefix("size=").toInt()
        override val lineHeight = lines[1].trim().removePrefix("lineHeight=").toInt()
        override val chars = HashMap<Int, Glyph>()
    }
    for (i in 0 until lines[2].trim().removePrefix("chars=").toInt()) {
        val line = i + 3
        val data = lines[line].split(" ").mapNotNull { value ->
            if (value.all { it == ' ' }) return@mapNotNull null
            value.trim { (it !in '0'..'9') && it != '-' }
        }
        val ascii = data[0].toInt()
        font.chars[ascii] = object : Glyph {
            override val x = data[1].toInt()
            override val y = data[2].toInt()
            override val width = data[3].toInt()
            override val height = data[4].toInt()
            override val offsetX = data[5].toInt()
            override val offsetY = data[6].toInt()
            override val advance = data[7].toInt()
        }
    }
    return font
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
    val quad = stride * 6
    val triangle = stride * 3
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

interface Reserve {
    val parent: Reserve?
    val shader: Shader

    val start: Int
    var reserve: Int
    var count: Int

    fun vec2f(vector: Vector2f)
    fun vec2f(index: Int, vector: Vector2f)
    fun vec3f(vector: Vector3f)
    fun vec3f(index: Int, vector: Vector3f)
    fun vec4f(vector: Vector4f)
    fun vec4f(index: Int, vector: Vector4f)
    fun float(float: Float)
    fun float(index: Int, float: Float)
    fun clear()
    fun clear(index: Int, count: Int)
    fun draw()
    fun draw(index: Int, count: Int)
}

suspend fun Reserve.reserve(amount: Int, block: suspend Reserve.() -> (Unit)): Reserve {
    val parent = this
    return object : Reserve {
        override val parent: Reserve? = parent
        override val shader = parent.shader
        override val start = parent.reserve
        override var reserve = 0
        override var count = 0
        override fun vec2f(vector: Vector2f) {
            parent.vec2f(start + count, vector)
            count += 2
        }
        override fun vec2f(index: Int, vector: Vector2f) {
            parent.vec2f(start + index, vector)
        }
        override fun vec3f(vector: Vector3f) {
            parent.vec3f(start + count, vector)
            count += 3
        }
        override fun vec3f(index: Int, vector: Vector3f) {
            parent.vec3f(start + index, vector)
        }
        override fun vec4f(vector: Vector4f) {
            parent.vec4f(start + count, vector)
            count += 4
        }
        override fun vec4f(index: Int, vector: Vector4f) {
            parent.vec4f(start + index, vector)
        }
        override fun float(float: Float) {
            parent.float(start + count, float)
            count++
        }
        override fun float(index: Int, float: Float) {
            parent.float(start + index, float)
        }
        override fun clear() {
            parent.clear(start + reserve, count)
            count = 0
        }
        override fun clear(index: Int, count: Int) {
            parent.clear(start + index, count)
        }
        override fun draw() {
            println("\ninner: ${start + reserve}; $count")
            parent.draw(start + reserve, count)
        }
        override fun draw(index: Int, count: Int) {
            parent.draw(start + index, count)
        }
    }.apply {
        block()
        parent.reserve += amount
    }
}

fun ShaderBuffer.drawReserves(vararg reserves: Reserve) {
    reserves.minBy {
        var offset = it.start +
        var cursor = it
        while(cursor.parent != null) {

        }
    }
    draw()
    reserves.forEach {

    }
}

class ShaderBuffer(val shader: Shader, size: Int = 200000, val draw: ShaderBuffer.(Int, Int) -> (Unit)) {
    val data: FloatArray = FloatArray(size)
    val vao: Int = glGenVertexArrays()
    val vbo: Int = glGenBuffers()
    var reserve = 0
    suspend fun reserve(amount: Int, block: suspend Reserve.() -> (Unit) = {}): Reserve {
        val buffer = this
        return object : Reserve {
            override val parent: Reserve? = null
            override val shader = buffer.shader
            override val start = buffer.reserve
            override var reserve = 0
            override var count = 0
            override fun vec2f(vector: Vector2f) {
                data[start + reserve + count++] = vector.x
                data[start + reserve + count++] = vector.y
            }
            override fun vec2f(index: Int, vector: Vector2f) {
                data[start + index] = vector.x
                data[start + index + 1] = vector.y
            }
            override fun vec3f(vector: Vector3f) {
                data[start + reserve + count++] = vector.x
                data[start + reserve + count++] = vector.y
                data[start + reserve + count++] = vector.z
            }
            override fun vec3f(index: Int, vector: Vector3f) {
                data[start + index] = vector.x
                data[start + index + 1] = vector.y
                data[start + index + 2] = vector.z
            }
            override fun vec4f(vector: Vector4f) {
                data[start + reserve + count++] = vector.x
                data[start + reserve + count++] = vector.y
                data[start + reserve + count++] = vector.z
                data[start + reserve + count++] = vector.w
            }
            override fun vec4f(index: Int, vector: Vector4f) {
                data[start + index] = vector.x
                data[start + index + 1] = vector.y
                data[start + index + 2] = vector.z
                data[start + index + 3] = vector.w
            }
            override fun float(float: Float) {
                data[start + reserve + count++] = float
            }
            override fun float(index: Int, float: Float) {
                data[start + index] = float
            }
            override fun clear() {
                data.fill(0f, start, reserve + count)
                count = 0
            }
            override fun clear(index: Int, count: Int) {
                data.fill(0f, start + index, start + index + count)
            }
            override fun draw() {
                draw(this@ShaderBuffer, start + reserve, count)
            }
            override fun draw(index: Int, count: Int) {
                println("\nouter: ${start + index}; $count")
                draw(this@ShaderBuffer, start + index, count)
            }
        }.apply {
            block()
            buffer.reserve += amount
        }
    }
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

val TOP_LEFT_CORNERS = arrayOf(
    Vector2i(0, 0),
    Vector2i(1, 0),
    Vector2i(1, 1),
    Vector2i(1, 1),
    Vector2i(0, 0),
    Vector2i(0, 1)
)

val CORNERS = arrayOf(
    Vector2f(-0.5f, -0.5f),
    Vector2f(0.5f, -0.5f),
    Vector2f(0.5f, 0.5f),
    Vector2f(0.5f, 0.5f),
    Vector2f(-0.5f, -0.5f),
    Vector2f(-0.5f, 0.5f)
)

fun Reserve.fontQuads(font: Font, start: Vector2f, string: String, size: Float = 0.025f, z: Int = 0) {
    val position = start.copy()
    string.forEach {
        if (!font.chars.containsKey(it.code)) return@forEach
        val glyph = font.chars[it.code]!!
        val glyphPosition = position.copy()
        glyphPosition.x += glyph.offsetX * size
        glyphPosition.y -= glyph.offsetY * size
        for (vertex in 0 until 6) {
            val uvX = glyph.x + glyph.width * TOP_LEFT_CORNERS[vertex].x
            val uvY = glyph.y + glyph.height - glyph.height * TOP_LEFT_CORNERS[vertex].y
            float(glyphPosition.x + (glyph.width * size) * TOP_LEFT_CORNERS[vertex].x)
            float(glyphPosition.y - (glyph.height * size) + (glyph.height * size) * TOP_LEFT_CORNERS[vertex].y)
            float(z.toFloat())
            float((uvX + uvY * 512).toFloat())
        }
        position.x += glyph.advance * size
    }
}

fun Reserve.textureQuad(pos: Vector2f, dim: Vector2f, uvPos: Vector2i, uvDim: Vector2i, z: Int = 0) {
    for (vertex in 0 until 6) {
        val uvX = uvPos.x + uvDim.x * TOP_LEFT_CORNERS[vertex].x
        val uvY = uvPos.y + uvDim.y * TOP_LEFT_CORNERS[vertex].y
        float(pos.x + dim.x * CORNERS[vertex].x)
        float(pos.y + dim.y * CORNERS[vertex].y)
        float(z.toFloat())
        float((uvX + uvY * 512).toFloat())
    }
}

fun Reserve.colorTriangle(a: Vector2f, b: Vector2f, c: Vector2f, color: Vector4f, z: Int = 0) {
    vec3f(Vector3f(a, z.toFloat()))
    vec4f(color)

    vec3f(Vector3f(b, z.toFloat()))
    vec4f(color)

    vec3f(Vector3f(c, z.toFloat()))
    vec4f(color)
}

fun Reserve.fovTriangle(a: Vector2f, b: Vector2f, c: Vector2f, z: Int = 0) {
    vec3f(Vector3f(a, z.toFloat()))
    vec3f(Vector3f(b, z.toFloat()))
    vec3f(Vector3f(c, z.toFloat()))
}

fun Reserve.colorQuad(pos: Vector2f, dim: Vector2f, color: Vector4f, z: Int = 0) {
    for (vertex in 0 until 6) {
        float(pos.x + dim.x * CORNERS[vertex].x)
        float(pos.y + dim.y * CORNERS[vertex].y)
        float(z.toFloat())
        vec4f(color)
    }
}

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

    fun move()

    suspend fun onClosed(block: suspend () -> (Unit))
    suspend fun onTick(block: suspend (Float, Float) -> (Unit))
    suspend fun onFixed(fps: Int, block: suspend (Float, Float) -> (Unit))
}

suspend fun window(block: suspend Window.() -> (Unit)) = coroutineScope {
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
    val onClosed = ArrayList<suspend () -> (Unit)>()
    val onTick = ArrayList<suspend (Float, Float) -> (Unit)>()
    val window = object : Window {
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
        override suspend fun onClosed(block: suspend () -> Unit) = onClosed.plusAssign(block)
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
        override suspend fun onTick(block: suspend (Float, Float) -> (Unit)) = onTick.plusAssign(block)
        override suspend fun onFixed(fps: Int, block: suspend (Float, Float) -> (Unit)) {
            var remainder = 0f
            val rate = 1f / fps
            onTick += { delta, elapsed ->
                val from = remainder
                val to = from + delta
                remainder = if (to > rate) { block(delta, elapsed); to % rate } else to
            }
        }
    }
    glfwSetKeyCallback(id) { _, code, _, action, _ ->
        launch { window.keyEvent.forEach { it(code, action) } }
        if (action == GLFW_PRESS) window.keyState.set(code)
        else if (action == GLFW_RELEASE) window.keyState.clear(code)
    }
    glfwSetMouseButtonCallback(id) { _, code, action, _ ->
        launch { window.mouseEvent.forEach { it(code, action) } }
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
        onTick.forEach { it(deltaSeconds, elapsedSeconds) }
    }
    onClosed.forEach { it() }
}