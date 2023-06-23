package me.mason.eng

import org.lwjgl.BufferUtils
import org.lwjgl.BufferUtils.createFloatBuffer
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray
import org.lwjgl.opengl.ARBVertexArrayObject.glGenVertexArrays
import org.lwjgl.opengl.GL20.*
import org.lwjgl.stb.STBImage.stbi_image_free
import org.lwjgl.stb.STBImage.stbi_load
import java.lang.System.nanoTime
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.readBytes

interface Shader {
    val program: Int
    val attrCount: Int
    val floats: Int
    val quadFloats: Int
    val triFloats: Int
    val attrs: IntArray
}

fun Int.compileShader(src: String): Int {
    glShaderSource(this, src)
    glCompileShader(this)
    if (glGetShaderi(this, GL_COMPILE_STATUS) == GL_FALSE) {
        val len = glGetShaderi(this, GL_INFO_LOG_LENGTH)
        error(glGetShaderInfoLog(this, len))
    }; return this
}

fun shader(
    vert: String,
    frag: String,
    vararg attrs: Int
): Shader {
    val attrCount = attrs.size
    val floatCount = attrs.sum()
    val vid = glCreateShader(GL_VERTEX_SHADER).compileShader(String(Paths.get(vert).readBytes()))
    val fid = glCreateShader(GL_FRAGMENT_SHADER).compileShader(String(Paths.get(frag).readBytes()))
    val pid = glCreateProgram()
    glAttachShader(pid, vid)
    glAttachShader(pid, fid)
    glLinkProgram(pid)
    if (glGetProgrami(pid, GL_LINK_STATUS) == GL_FALSE) {
        val len = glGetProgrami(pid, GL_INFO_LOG_LENGTH)
        error(glGetProgramInfoLog(pid, len))
    }
    return object : Shader {
        override val program = pid
        override val attrs = attrs
        override val floats = floatCount
        override val quadFloats = floatCount * 4
        override val triFloats = floatCount * 3
        override val attrCount = attrCount
    }
}

const val MAX_QUADS = 8192
val ELEMENT_ORDER = intArrayOf(2, 1, 0, 0, 1, 3)
val ELEMENTS = IntArray(MAX_QUADS * 6) {
    ELEMENT_ORDER[it % 6] + (it / 6) * 4
}

interface Buffer {
    val shader: Shader
    val vao: Int
    val vbo: Int
    val data: FloatArray
    fun clearQuad(index: Int)
    fun clearQuad(range: IntRange)
    fun quad(
        index: Int,
        pos: FloatVector,
        dim: FloatVector,
        uvPos: IntVector,
        uvDim: IntVector
    )
    fun tri(
        a: FloatVector,
        b: FloatVector,
        c: FloatVector
    )
}

fun Shader.uniform(name: String, block: (Int) -> (Unit)) = block(glGetUniformLocation(program, name))

fun atlas(atlasPath: Path) = glGenTextures().also { id ->
    glBindTexture(GL_TEXTURE_2D, id)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    val width = BufferUtils.createIntBuffer(1)
    val height = BufferUtils.createIntBuffer(1)
    val channels = BufferUtils.createIntBuffer(1)
    val image = stbi_load(atlasPath.absolutePathString(), width, height, channels, 0)
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

private val MESH_VERTEX_CORNERS = arrayOf(
    1f, 0f,
    0f, 1f,
    1f, 1f,
    0f, 0f
)
private val MESH_UV_CORNERS = arrayOf(
    1, 1,
    0, 0,
    1, 0,
    0, 1
)

fun fixed(fps: Int, block: Buffer.(Float, Float, Boolean) -> (Unit)): Buffer.(Float, Float) -> (Unit) {
    val ms = 1f / fps
    var offset = 0f
    return { delta, elapsed ->
        offset += delta
        if (offset > ms) {
            block(delta, elapsed, true)
            offset %= ms
        } else block(delta, elapsed, false)
    }
}

fun Window.buffer(
    shader: Shader,
    atlasSize: Int,
    atlasPath: Path,
    camera: Camera,
    block: Buffer.(Float, Float) -> (Unit)
) {
    val atlas = atlas(atlasPath)
    val vao = glGenVertexArrays()
    val vbo = glGenBuffers()
    val ebo = glGenBuffers()
    val bytes = shader.floats * Float.SIZE_BYTES
    val data = FloatArray(shader.floats * MAX_QUADS * 4)
    glBindVertexArray(vao)
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    glBufferData(GL_ARRAY_BUFFER, (MAX_QUADS * 4 * bytes).toLong(), GL_DYNAMIC_DRAW)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, BufferUtils.createIntBuffer(ELEMENTS.size).put(ELEMENTS).flip(), GL_STATIC_DRAW)
    var offset = 0
    shader.attrs.forEachIndexed { index, attribute ->
        glVertexAttribPointer(index, attribute, GL_FLOAT, false, bytes, (offset * Float.SIZE_BYTES).toLong())
        offset += attribute
    }
    val buffer = object : Buffer {
        override val shader = shader
        override val vao = vao
        override val vbo = vbo
        override val data = data

        override fun clearQuad(index: Int) {
            for (vertex in 0 until 4) {
                val quadOffset = index * shader.quadFloats + vertex * shader.floats
                data[quadOffset] = 0f
                data[quadOffset + 1] = 0f
                data[quadOffset + 2] = 0f
            }
        }

        override fun clearQuad(range: IntRange) {
            range.forEach { clearQuad(it) }
        }

        override fun quad(index: Int, pos: FloatVector, dim: FloatVector, uvPos: IntVector, uvDim: IntVector) {
            for (vertex in 0 until 4) {
                val quadOffset = index * shader.quadFloats + vertex * shader.floats
                val topLeftX = pos.x - (dim.x / 2f)
                val topLeftY = pos.y - (dim.y / 2f)
                val uvVertexX = uvPos.x + uvDim.x * MESH_UV_CORNERS[vertex * 2]
                val uvVertexY = uvPos.y + uvDim.y * MESH_UV_CORNERS[vertex * 2 + 1]
                data[quadOffset] = topLeftX + dim.x * MESH_VERTEX_CORNERS[vertex * 2]
                data[quadOffset + 1] = topLeftY + dim.y * MESH_VERTEX_CORNERS[vertex * 2 + 1]
                data[quadOffset + 2] = uvVertexX + uvVertexY * atlasSize.toFloat()
            }
        }

        override fun tri(a: FloatVector, b: FloatVector, c: FloatVector) {
            TODO("Not yet implemented")
        }
    }
    val matBuf = createFloatBuffer(16)
    var elapsed = 0L
    var delta: Long
    var last = nanoTime()
    while(!glfwWindowShouldClose(window)) {
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        glUseProgram(shader.program)
        shader.apply {
            uniform("TEX_SAMPLER") { glUniform1i(it, 0) }
            uniform("uProjection") { camera.projection.get(matBuf); glUniformMatrix4fv(it, false, matBuf) }
            uniform("uView") { camera.view.get(matBuf); glUniformMatrix4fv(it, false, matBuf) }
        }
        glBindTexture(GL_TEXTURE_2D, atlas)
        glActiveTexture(0)
        glBindVertexArray(vao)
        (0 until shader.attrCount).forEach { glEnableVertexAttribArray(it) }
        val now = nanoTime()
        delta = now - last
        elapsed += delta
        last = now
        block(buffer, delta / 1000000000f, elapsed / 1000000000f)
        glDrawElements(GL_TRIANGLES, ELEMENTS.size, GL_UNSIGNED_INT, 0)
        (0 until shader.attrCount).forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glUseProgram(0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glfwSwapBuffers(window)
        glfwPollEvents()
    }
}