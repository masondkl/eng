package me.mason.client

import org.joml.*
import org.lwjgl.BufferUtils.createFloatBuffer
import org.lwjgl.BufferUtils.createIntBuffer
import org.lwjgl.opengl.GL30.*
import java.lang.Float.intBitsToFloat
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.max
import kotlin.math.min

private val ELEMENT_ORDER = intArrayOf(0, 1, 2, 2, 0, 3)

val TOP_LEFT_CORNERS = arrayOf(
    Vector2i(0, 0),
    Vector2i(1, 0),
    Vector2i(1, 1),
    Vector2i(0, 1),
)
val TOP_LEFT_UV_CORNERS = arrayOf(
    Vector2i(0, 1),
    Vector2i(1, 1),
    Vector2i(1, 0),
    Vector2i(0, 0),
)
val CENTER_CORNERS = arrayOf(
    Vector2f(-1f, -1f),
    Vector2f(1f, -1f),
    Vector2f(1f, 1f),
    Vector2f(-1f, 1f),
)

interface GraphicsBuffer {
    val shader: Shader
    val vao: Int
    val vbo: Int
    val ebo: Int
    val size: Int
}

interface Cursor {
    val buffer: GraphicsBuffer
    val vData: FloatBuffer
    val eData: IntBuffer
    val eDataTemp: IntBuffer
    val texQuad: TexQuad
    val fntQuad: FontQuad
    val texTri: TexTri
    var maxElement: Int
    fun clear()
}

const val TOP_LEFT = 0
const val CENTER = 1

interface Quad {
    var x: Float
    var y: Float
    var dimX: Float
    var dimY: Float
    fun pos(pos: Vector2f)
    fun dim(dim: Vector2f)
}

fun Quad() = object : Quad {
    override var x = 0f
    override var y = 0f
    override var dimX = 0f
    override var dimY = 0f
    override fun pos(pos: Vector2f) {
        x = pos.x
        y = pos.y
    }
    override fun dim(dim: Vector2f) {
        dimX = dim.x
        dimY = dim.y
    }
}

interface Tri {
    var ax: Float; var ay: Float; var az: Float
    var bx: Float; var by: Float; var bz: Float
    var cx: Float; var cy: Float; var cz: Float
    fun a(a: Vector3f)
    fun b(b: Vector3f)
    fun c(c: Vector3f)
    fun a(a: Vector2f, z: Int = 0)
    fun b(b: Vector2f, z: Int = 0)
    fun c(c: Vector2f, z: Int = 0)
}

fun Tri() = object : Tri {
    override var ax = 0f; override var ay = 0f; override var az = 0f
    override var bx = 0f; override var by = 0f; override var bz = 0f
    override var cx = 0f; override var cy = 0f; override var cz = 0f
    override fun a(a: Vector3f) {
        ax = a.x; ay = a.y; az = a.z
    }
    override fun a(a: Vector2f, z: Int) {
        ax = a.x; ay = a.y; az = z.toFloat()
    }
    override fun b(b: Vector3f) {
        bx = b.x; by = b.y; bz = b.z
    }
    override fun b(b: Vector2f, z: Int) {
        bx = b.x; by = b.y; bz = z.toFloat()
    }
    override fun c(c: Vector3f) {
        cx = c.x; cy = c.y; cz = c.z
    }
    override fun c(c: Vector2f, z: Int) {
        cx = c.x; cy = c.y; cz = z.toFloat()
    }
}

interface TexQuad : Quad {
    var type: Int
    var uvX: Int
    var uvY: Int
    var uvDimX: Int
    var uvDimY: Int
    var character: Boolean
    var fill: Boolean
    var ui: Boolean
    var r: Int
    var g: Int
    var b: Int
    var a: Int
    var z: Int
    var texture: Int
    fun rgb(rgb: Vector4i)
    fun uvPos(uvPos: Vector2i)
    fun uvDim(uvDim: Vector2i)
    operator fun invoke(block: TexQuad.() -> (Unit))
}
interface TexTri : Tri {
    var type: Int
    var character: Boolean
    var fill: Boolean
    var ui: Boolean
    var r: Int
    var g: Int
    var b: Int
    var a: Int
    var texture: Int
    operator fun invoke(block: TexTri.() -> (Unit))
}

interface FontQuad {
    var font: Font
    var centerX: Float
    var centerY: Float
    var text: String
    var point: Float
    var ui: Boolean
    var z: Int
    fun center(center: Vector2f)
    operator fun invoke(block: FontQuad.() -> (Unit))
}

fun Cursor(size: Int, buffer: GraphicsBuffer) = object : Cursor {
    override val buffer = buffer
    override val vData = createFloatBuffer(size)
    override val eData = createIntBuffer(size)
    override val eDataTemp = createIntBuffer(size)
    override var maxElement = 0
    override val texQuad = object : TexQuad, Quad by Quad() {
        override var type = CENTER
        override var uvX = 0
        override var uvY = 0
        override var uvDimX = 0
        override var uvDimY = 0
        override var character = false
        override var fill = false
        override var ui = false
        override var r = 0
        override var g = 0
        override var b = 0
        override var a = 0
        override var z = 0
        override var texture = GAME_TEXTURE
        override fun rgb(rgb: Vector4i) {
            r = rgb.x
            g = rgb.y
            b = rgb.z
            a = rgb.w
        }
        override fun uvPos(uvPos: Vector2i) {
            uvX = uvPos.x
            uvY = uvPos.y
        }
        override fun uvDim(uvDim: Vector2i) {
            uvDimX = uvDim.x
            uvDimY = uvDim.y
        }
        override fun invoke(block: TexQuad.() -> (Unit)) {
            type = CENTER
            x = 0f; y = 0f
            dimX = 0f; dimY = 0f
            uvX = 0; uvY = 0
            uvDimX = 0; uvDimY = 0
            character = false
            ui = false
            fill = false
            z = 0
            r = 0; g = 0; b = 0; a = 0
            texture = GAME_TEXTURE
            block()
            val rad = Vector2f(dimX / 2f, dimY / 2f)
            var flags = 0
            if (ui) flags = flags or 1
            if (character) flags = flags or (1 shl 1)
            if (fill) flags = flags or (1 shl 2)
            for (vertex in 0 until 4) {
                val uvX = max(0, min(127, uvX + uvDimX * TOP_LEFT_UV_CORNERS[vertex].x))
                val uvY = max(0, min(127, uvY + uvDimY * TOP_LEFT_UV_CORNERS[vertex].y))
                var pack = 0
                pack = pack or uvX
                pack = (pack shl 8) or uvY
                pack = (pack shl 4) or flags
                pack = (pack shl 4) or TEXTURE_INDICES[texture]!!
                pack = (pack shl 8) or z
                var packedPos = 0
                if (type == CENTER) {
                    val xAligned = min(32766, ((x + rad.x * CENTER_CORNERS[vertex].x + 256) * 16f).toInt())
                    val yAligned = min(32766, ((y + rad.y * CENTER_CORNERS[vertex].y + 256) * 16f).toInt())
                    packedPos = xAligned
                    packedPos = (packedPos shl 16) or yAligned
                } else if (type == TOP_LEFT) {
                    val xAligned = min(32766, ((x + dimX * TOP_LEFT_CORNERS[vertex].x + 256) * 16f).toInt())
                    val yAligned = min(32766, ((y - dimY * TOP_LEFT_CORNERS[vertex].y + 256) * 16f).toInt())
                    packedPos = xAligned
                    packedPos = (packedPos shl 16) or yAligned
                }
                val red = ((r / 255f) * 128f).toInt()
                val green = ((g / 255f) * 128f).toInt()
                val blue = ((b / 255f) * 128f).toInt()
                val alpha = ((a / 255f) * 128f).toInt()
                var rgb = red
                rgb = (rgb shl 8) or green
                rgb = (rgb shl 8) or blue
                rgb = (rgb shl 8) or alpha
                vData.put(intBitsToFloat(packedPos))
                vData.put(intBitsToFloat(pack))
                vData.put(intBitsToFloat(rgb))
            }
            ELEMENT_ORDER.forEach {
                eData.put(maxElement + it)
            }
            maxElement += 4
        }
    }
    override val texTri = object : TexTri, Tri by Tri() {
        override var type = CENTER
        override var character = false
        override var fill = false
        override var ui = false
        override var r = 0
        override var g = 0
        override var b = 0
        override var a = 0
        override var texture = GAME_TEXTURE
        override fun invoke(block: TexTri.() -> (Unit)) {
            type = CENTER
            character = false
            ui = false
            fill = false
            r = 0; g = 0; b = 0; a = 0
            texture = GAME_TEXTURE
            block()
            var flags = 0
            if (ui) flags = flags or 1
            if (character) flags = flags or (1 shl 1)
            if (fill) flags = flags or (1 shl 2)
            for (pos in listOf(Vector3f(ax, ay, az), Vector3f(bx, by, bz), Vector3f(cx, cy, cz))) {
                var pack = 0
                pack = pack or flags
                pack = (pack shl 4) or TEXTURE_INDICES[texture]!!
                pack = (pack shl 8) or pos.z.toInt()
                var packedPos = 0
                val xAligned = min(32766, ((pos.x + 256) * 16f).toInt())
                val yAligned = min(32766, ((pos.y + 256) * 16f).toInt())
                packedPos = xAligned
                packedPos = (packedPos shl 16) or yAligned
                val red = ((r / 255f) * 128f).toInt()
                val green = ((g / 255f) * 128f).toInt()
                val blue = ((b / 255f) * 128f).toInt()
                val alpha = ((a / 255f) * 128f).toInt()
                var rgb = red
                rgb = (rgb shl 8) or green
                rgb = (rgb shl 8) or blue
                rgb = (rgb shl 8) or alpha
                vData.put(intBitsToFloat(packedPos))
                vData.put(intBitsToFloat(pack))
                vData.put(intBitsToFloat(rgb))
            }
            (0 until 3).forEach { _ -> eData.put(maxElement++) }
        }
    }
    override val fntQuad = object : FontQuad {
        val self = this
        override var font = FONT
        override var centerX = 0f
        override var centerY = 0f
        override var text = ""
        override var point = 1f
        override var ui = false
        override var z = 0
        override fun center(center: Vector2f) {
            centerX = center.x
            centerY = center.y
        }
        override fun invoke(block: FontQuad.() -> (Unit)) {
            font = FONT
            centerX = 0f; centerY = 0f
            text = ""
            point = 1f
            ui = false
            z = 0
            block()
            var rad = 0f
            text.forEach {
                if (!font.chars.containsKey(it.code)) return@forEach
                val glyph = font.chars[it.code]!!
                rad += glyph.advance * point
            }
            rad /= 2
            var posX = centerX - rad
            val posY = centerY + ((font.lineHeight * point) / 2f)
            text.forEach {
                if (!font.chars.containsKey(it.code)) return@forEach
                val glyph = font.chars[it.code]!!
                var glyphPosX = posX
                var glyphPosY = posY
                glyphPosY -= glyph.offsetY * point
                glyphPosX += glyph.offsetX * point
                texQuad {
                    type = TOP_LEFT
                    x = glyphPosX
                    y = glyphPosY
                    dimX = 0.1f
                    dimY = 0.1f
                    uvX = glyph.x
                    uvY = 128 - glyph.y - glyph.height
                    uvDimX = glyph.width
                    uvDimY = glyph.height
                    texture = FONT_TEXTURE
                    z = self.z
                    ui = self.ui
                    character = true
                }
                posX += glyph.advance * point
            }
        }
    }
    override fun clear() {
        vData.clear()
        eData.clear()
        maxElement = 0
    }
}

fun GraphicsBuffer.draw(method: GraphicsBuffer.(Int) -> (Unit), vararg cursors: Cursor) {
//    val start = markNow()
    var vertexCount = 0
    var elementCount = 0
    var element = 0
    cursors.forEach { cursor ->
        cursor.eDataTemp.clear()
//        val beforeIteration = markNow()
        if (element != 0) (0 until cursor.eData.position()).forEach {
            cursor.eDataTemp.put(element + cursor.eData[it])
        }
//        println("\nIteration: ${beforeIteration.elapsedNow()}")
        val vBefore = cursor.vData.position()
        val eBefore = cursor.eData.position()
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, vertexCount * 4L, cursor.vData.flip())
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, elementCount * 4L, if (element != 0) cursor.eDataTemp.flip() else cursor.eData.flip())
        cursor.vData.apply { position(vBefore); limit(capacity()) }
        if (element == 0) cursor.eData.apply { position(eBefore); limit(capacity()) }
        element += cursor.maxElement
        vertexCount += cursor.vData.position()
        elementCount += cursor.eData.position()
    }
    method(elementCount)
//    println("Full: ${start.elapsedNow()}")
}

fun GraphicsBuffer(shader: Shader, size: Int) = object : GraphicsBuffer {
    override val shader = shader
    override val vao = glGenVertexArrays()
    override val vbo = glGenBuffers()
    override val ebo = glGenBuffers()
    override val size = size
    init {
        glBindVertexArray(vao)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, size * 4L, GL_DYNAMIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, size * 4L, GL_DYNAMIC_DRAW)
        var offset = 0L
        shader.attrs.forEachIndexed { index, size ->
            glVertexAttribPointer(index, size, GL_FLOAT, false, shader.stride * 4, offset * 4L)
            offset += size
        }
    }
}