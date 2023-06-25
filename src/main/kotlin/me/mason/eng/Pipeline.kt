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
        override val quadFloats = floatCount * 6
        override val triFloats = floatCount * 3
        override val attrCount = attrCount
    }
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

//private val MESH_VERTEX_CORNERS = arrayOf(
//    1f, 0f,
//    0f, 1f,
//    1f, 1f,
//    0f, 0f
//)
//private val MESH_UV_CORNERS = arrayOf(
//    1, 1,
//    0, 0,
//    1, 0,
//    0, 1
//)
val X_CORNERS = arrayOf(1, 1, 0, 1, 0, 0)
val Y_CORNERS = arrayOf(1, 0, 1, 0, 0, 1)

interface Mesh {
    val triangleFloats: Int
    val vao: Int
    val vbo: Int
    val data: FloatArray
}

fun Shader.createMesh(triangles: Int) = object : Mesh {
    override val triangleFloats = triangles * floats * 3
    override val vao = glGenVertexArrays()
    override val vbo = glGenBuffers()
    override val data = FloatArray(triangleFloats)
    init {
        val maxBytes = triangleFloats * Float.SIZE_BYTES
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, maxBytes.toLong(), GL_DYNAMIC_DRAW)
        var offset = 0
        val sum = attrs.sum()
        attrs.forEachIndexed { index, attribute ->
            glVertexAttribPointer(index, attribute, GL_FLOAT, false, sum * Float.SIZE_BYTES, (offset * Float.SIZE_BYTES).toLong())
            offset += attribute
        }
    }
}

interface Buffer {
    fun using(shader: Shader, block: () -> (Unit))
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
        index: Int,
        a: FloatVector,
        b: FloatVector,
        c: FloatVector
    )
    fun tick(block: (Float, Float) -> (Unit))
    fun fixed(fps: Int, block: (Float) -> (Unit))
}

//fun fixed(fps: Int, block: Buffer.(Float, Float, Boolean) -> (Unit)): Buffer.(Float, Float) -> (Unit) {
//    val ms = 1f / fps
//    var offset = 0f
//    return { delta, elapsed ->
//        offset += delta
//        if (offset > ms) {
//            block(delta, elapsed, true)
//            offset %= ms
//        } else block(delta, elapsed, false)
//    }
//}

fun Window.buffer(
    atlasSize: Int,
    atlasPath: Path,
    camera: Camera,
    block: Buffer.() -> (Unit)
) {
    val atlas = atlas(atlasPath)
    val meshes = HashMap<Shader, Mesh>()
//    blocks.map { it.first }.forEach { shader ->
//        meshes.computeIfAbsent(shader) { shader.createMesh(8192) }
//    }
    val buffer = object : Buffer {
        val onTick = ArrayList<(Float, Float) -> (Unit)>()
        var shader: Shader? = null
        override fun using(shader: Shader, block: () -> Unit) {
            this.shader = shader
            block()
            this.shader = null
        }
        override fun clearQuad(index: Int) {
            assert(shader != null)
            val data = meshes.getOrPut(shader!!) { shader!!.createMesh(16384) }.data
            for (vertex in 0 until 6) {
                val quadOffset = index * shader!!.quadFloats + vertex * shader!!.floats
                data[quadOffset] = 0f
                data[quadOffset + 1] = 0f
                data[quadOffset + 2] = 0f
            }
        }
        override fun clearQuad(range: IntRange) = range.forEach { clearQuad(it) }
        override fun quad(index: Int, pos: FloatVector, dim: FloatVector, uvPos: IntVector, uvDim: IntVector) {
            assert(shader != null)
            val data = meshes.getOrPut(shader!!) { shader!!.createMesh(16384) }.data
            for (vertex in 0 until 6) {
                val quadOffset = index * shader!!.quadFloats + vertex * shader!!.floats
                val topLeftX = pos.x - (dim.x / 2f)
                val topLeftY = pos.y - (dim.y / 2f)
                val uvVertexX = uvPos.x + uvDim.x * X_CORNERS[vertex]
                val uvVertexY = uvPos.y + uvDim.y * Y_CORNERS[vertex]
                data[quadOffset] = topLeftX + dim.x * X_CORNERS[vertex]
                data[quadOffset + 1] = topLeftY + dim.y * Y_CORNERS[vertex]
                data[quadOffset + 2] = uvVertexX + uvVertexY * atlasSize.toFloat()
            }
        }
        override fun tri(index: Int, a: FloatVector, b: FloatVector, c: FloatVector) {
            assert(shader != null)
            val data = meshes.getOrPut(shader!!) { shader!!.createMesh(16384) }.data
            data[index * 6] = a.x
            data[index * 6 + 1] = a.y

            data[index * 6 + 2] = b.x
            data[index * 6 + 3] = b.y

            data[index * 6 + 4] = c.x
            data[index * 6 + 5] = c.y
        }
        override fun tick(block: (Float, Float) -> Unit) = onTick.plusAssign(block)

        override fun fixed(fps: Int, block: (Float) -> Unit) {
            val ms = 1f / fps
            var offset = 0f
            onTick += { delta, elapsed ->
                offset += delta
                if (offset > ms) {
                    block(elapsed)
                    offset %= ms
                }
            }
        }
    }.apply(block)
    val matBuf = createFloatBuffer(16)
    var elapsed = 0L
    var delta: Long
    var last = nanoTime()
    while (!glfwWindowShouldClose(window)) {
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
        val now = nanoTime()
        delta = now - last
        elapsed += delta
        last = now
        val deltaSeconds = delta / 1000000000f
        val elapsedSeconds = elapsed / 1000000000f
        buffer.onTick.forEach { it(deltaSeconds, elapsedSeconds) }
        meshes.forEach { (shader, mesh) ->
            glBindBuffer(GL_ARRAY_BUFFER, mesh.vbo)
            glBufferSubData(GL_ARRAY_BUFFER, 0, mesh.data)
            glUseProgram(shader.program)
            shader.apply {
                uniform("TEX_SAMPLER") { glUniform1i(it, 0) }
                uniform("uProjection") { camera.projection.get(matBuf); glUniformMatrix4fv(it, false, matBuf) }
                uniform("uView") { camera.view.get(matBuf); glUniformMatrix4fv(it, false, matBuf) }
            }
            glBindTexture(GL_TEXTURE_2D, atlas)
            glActiveTexture(0)
            glBindVertexArray(mesh.vao)
            (0 until shader.attrCount).forEach { glEnableVertexAttribArray(it) }
            glDrawArrays(GL_TRIANGLES, 0, mesh.triangleFloats)
            (0 until shader.attrCount).forEach { glDisableVertexAttribArray(it) }
            glBindVertexArray(0)
            glUseProgram(0)
            glBindTexture(GL_TEXTURE_2D, 0)
        }
        glfwSwapBuffers(window)
        glfwPollEvents()
    }
}