package me.mason.client

import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

suspend fun main() = window {
    val matrix = BufferUtils.createFloatBuffer(16)
    println(Paths.get(".").absolutePathString())
    val colorShader = Shader(Paths.get("color.vert"), Paths.get("color.frag"), 3, 4)
    val colorBuffer = ShaderBuffer(colorShader, colorShader.quad * 2) { start, count ->
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, data)
        shader.program.let {
            glUseProgram(it)
            glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, projection.get(matrix))
            glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, view.get(matrix))
        }
        glBindVertexArray(vao)
        shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
        glDrawArrays(GL_TRIANGLES, start / shader.stride, count / shader.stride)
        shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
        glBindVertexArray(0)
        glUseProgram(0)
    }
    glEnable(GL_DEPTH_TEST)
    glDepthFunc(GL_LESS)
    println("Quad size: ${colorShader.quad}")
    val reserve = colorBuffer.reserve(colorShader.quad * 2) {
//        onTick { _, _ ->
//            clear()
//            colorQuad(Vector2f(1f, 1f), Vector2f(2.0f), Vector4f(0.0f, 1.0f, 0.0f, 1.0f))
//            colorQuad(Vector2f(-1f, -1f), Vector2f(2.0f), Vector4f(1.0f, 0.0f, 0.0f, 1.0f))
//        }
    }
    val a = reserve.reserve(colorShader.quad) inner@{
        onTick { _, _ ->
            clear()
            colorQuad(Vector2f(1f, 1f), Vector2f(2.0f), Vector4f(0.0f, 1.0f, 0.0f, 1.0f))
        }
    }
    val b = reserve.reserve(colorShader.quad) inner@{
        onTick { _, _ ->
            clear()
            colorQuad(Vector2f(-1f, -1f), Vector2f(2.0f), Vector4f(1.0f, 0.0f, 0.0f, 1.0f))
        }
    }
    onTick { _, _ ->
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

//        reserve.draw()
        a.draw()
        b.draw()

        glfwSwapBuffers(window)
        glfwPollEvents()
    }
}