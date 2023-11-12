package me.mason.client

import org.lwjgl.opengl.GL30.*
import java.nio.file.Path
import kotlin.io.path.readBytes


interface Shader {
    val program: Int
    val attrs: IntArray
    val stride: Int
    val tri: Int
    val quad: Int
}

fun Shader(vertex: Path, fragment: Path, vararg attrs: Int) = object : Shader {
    val vertexId = glCreateShader(GL_VERTEX_SHADER).compile(String(vertex.readBytes()))
    val fragmentId = glCreateShader(GL_FRAGMENT_SHADER).compile(String(fragment.readBytes()))
    override val program = glCreateProgram()
    override val attrs = attrs
    override val stride = attrs.sum()
    override val tri = stride * 3
    override val quad = stride * 4
    fun Int.compile(src: String): Int {
        glShaderSource(this, src)
        glCompileShader(this)
        if (glGetShaderi(this, GL_COMPILE_STATUS) == GL_FALSE) {
            val len = glGetShaderi(this, GL_INFO_LOG_LENGTH)
            error(glGetShaderInfoLog(this, len))
        }; return this
    }

    init {
        glAttachShader(program, vertexId)
        glAttachShader(program, fragmentId)
        glLinkProgram(program)
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            val len = glGetProgrami(program, GL_INFO_LOG_LENGTH)
            error(glGetProgramInfoLog(program, len))
        }
    }
}