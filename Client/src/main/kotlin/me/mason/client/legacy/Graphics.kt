package me.mason.client.legacy//package me.mason.client
//
//import kotlinx.coroutines.runBlocking
//import me.mason.client.testing.ATLASES
//import me.mason.shared.*
//import org.joml.*
//import org.lwjgl.BufferUtils.createIntBuffer
//import org.lwjgl.glfw.GLFW.*
//import org.lwjgl.glfw.GLFWErrorCallback.createPrint
//import org.lwjgl.opengl.GL.createCapabilities
//import org.lwjgl.opengl.GL20.*
//import org.lwjgl.opengl.GL30.glBindVertexArray
//import org.lwjgl.opengl.GL30.glGenVertexArrays
//import org.lwjgl.stb.STBImage.*
//import org.lwjgl.system.MemoryUtil.NULL
//import java.nio.file.Path
//import java.util.*
//import kotlin.io.path.absolutePathString
//import kotlin.io.path.readBytes
//import kotlin.io.path.readLines
//import kotlin.system.exitProcess
//
//val DEFAULT_SIZE = Vector2i(1280, 720)
//
//interface Glyph {
//    val x: Int
//    val y: Int
//    val width: Int
//    val height: Int
//    val offsetX: Int
//    val offsetY: Int
//    val advance: Int
//}
//interface Font {
//    val size: Int
//    val lineHeight: Int
//    val chars: HashMap<Int, Glyph>
//}
//fun font(path: Path): Font {
//    val lines = path.readLines()
//    val font = object : Font {
//        override val size = lines[0].trim().removePrefix("size=").toInt()
//        override val lineHeight = lines[1].trim().removePrefix("lineHeight=").toInt()
//        override val chars = HashMap<Int, Glyph>()
//    }
//    for (i in 0 until lines[2].trim().removePrefix("chars=").toInt()) {
//        val line = i + 3
//        val data = lines[line].split(" ").mapNotNull { value ->
//            if (value.all { it == ' ' }) return@mapNotNull null
//            value.trim { (it !in '0'..'9') && it != '-' }
//        }
//        val ascii = data[0].toInt()
//        font.chars[ascii] = object : Glyph {
//            override val x = data[1].toInt()
//            override val y = data[2].toInt()
//            override val width = data[3].toInt()
//            override val height = data[4].toInt()
//            override val offsetX = data[5].toInt()
//            override val offsetY = data[6].toInt()
//            override val advance = data[7].toInt()
//        }
//    }
//    return font
//}
//fun atlas(path: Path) = glGenTextures().also { id ->
//    println("id: ${id}")
//    glBindTexture(GL_TEXTURE_2D, id)
//    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
//    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
//    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
//    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
//    val width = createIntBuffer(1)
//    val height = createIntBuffer(1)
//    val channels = createIntBuffer(1)
////    stbi_set_flip_vertically_on_load(true)
//    val image = stbi_load(path.absolutePathString(), width, height, channels, 0)
//    if (image != null) {
//        when(channels.get(0)) {
//            3 -> glTexImage2D(
//                GL_TEXTURE_2D, 0, GL_RGB, width.get(0), height.get(0),
//                0, GL_RGB, GL_UNSIGNED_BYTE, image
//            )
//            4 -> glTexImage2D(
//                GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0),
//                0, GL_RGBA, GL_UNSIGNED_BYTE, image
//            )
//            else -> error("Unknown number of channels")
//        }
//    } else error("Could not load image")
//    stbi_image_free(image)
//}
//
//fun Int.compileShader(src: String): Int {
//    glShaderSource(this, src)
//    glCompileShader(this)
//    if (glGetShaderi(this, GL_COMPILE_STATUS) == GL_FALSE) {
//        val len = glGetShaderi(this, GL_INFO_LOG_LENGTH)
//        error(glGetShaderInfoLog(this, len))
//    }; return this
//}
//
//class Shader(vert: Path, frag: Path, vararg val attrs: Int) {
//    private val vertId = glCreateShader(GL_VERTEX_SHADER).compileShader(String(vert.readBytes()))
//    private val fragId = glCreateShader(GL_FRAGMENT_SHADER).compileShader(String(frag.readBytes()))
//    val program = glCreateProgram()
//    val stride = attrs.sum()
//    val quad = stride * 6
//    val fastQuad = stride * 4
//    val triangle = stride * 3
//    init {
//        glAttachShader(program, vertId)
//        glAttachShader(program, fragId)
//        glLinkProgram(program)
//        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
//            val len = glGetProgrami(program, GL_INFO_LOG_LENGTH)
//            error(glGetProgramInfoLog(program, len))
//        }
//    }
//}
//
//class Aggregator(
//    val buffer: ShaderBuffer,
//    var pos: Int,
//    var max: Int
//) {
//    var len = 0
//    private var next = pos
//    fun aggregator(size: Int): Aggregator {
//        val result = Aggregator(buffer, next, size)
//        next += size
//        len += size
//        return result
//    }
//}
//
//fun Aggregator.draw(draw: ShaderBuffer.(Int, Int) -> (Unit)) = buffer.draw(pos, len)
//
//fun Aggregator.clear() {
////    buffer.data.fill(0f, pos, pos + len)
//    len = 0
//}
//
//private val ELEMENT_ORDER = intArrayOf(0, 1, 2, 2, 0, 3)
//class ShaderBuffer(val shader: Shader, size: Int, fastQuads: Boolean = false) {
//    val data: FloatArray = FloatArray(size)
//    val elements: IntArray = IntArray(size) {
//        ELEMENT_ORDER[it % 6] + (it / 6) * 4
//    }
//    val vao: Int = glGenVertexArrays()
//    val vbo: Int = glGenBuffers()
//    val ebo: Int? = if (fastQuads) glGenBuffers() else null
//    private var next = 0
//    fun aggregator(size: Int): Aggregator {
//        val result = Aggregator(this, next, size)
//        next += size
//        return result
//    }
//    fun vec2f(index: Int, vector: Vector2f) {
//        data[index] = vector.x
//        data[index + 1] = vector.y
//    }
//    fun vec3f(index: Int, vector: Vector3f) {
//        data[index] = vector.x
//        data[index + 1] = vector.y
//        data[index + 2] = vector.z
//    }
//    fun vec4f(index: Int, vector: Vector4f) {
//        data[index] = vector.x
//        data[index + 1] = vector.y
//        data[index + 2] = vector.z
//        data[index + 3] = vector.w
//    }
//    fun float(index: Int, float: Float) {
//        data[index] = float
//    }
//    fun clear(index: Int, count: Int) {
//        data.fill(0f, index, index + count)
//    }
//    init {
//        glBindVertexArray(vao)
//        if (fastQuads) {
//            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo!!)
//            glBufferData(GL_ELEMENT_ARRAY_BUFFER, elements, GL_STATIC_DRAW)
//        }
//        glBindBuffer(GL_ARRAY_BUFFER, vbo)
//        glBufferData(GL_ARRAY_BUFFER, size * 4L, GL_DYNAMIC_DRAW)
//        var offset = 0L
//        shader.attrs.forEachIndexed { index, attrSize ->
//            glVertexAttribPointer(index, attrSize, GL_FLOAT, false, shader.stride * 4, offset * 4L)
//            offset += attrSize
//        }
//    }
//}
//
//val TOP_LEFT_CORNERS = arrayOf(
//    Vector2i(0, 0),
//    Vector2i(1, 0),
//    Vector2i(1, 1),
//    Vector2i(1, 1),
//    Vector2i(0, 0),
//    Vector2i(0, 1)
//)
//val CORNERS = arrayOf(
//    Vector2f(-0.5f, -0.5f),
//    Vector2f(0.5f, -0.5f),
//    Vector2f(0.5f, 0.5f),
//    Vector2f(0.5f, 0.5f),
//    Vector2f(-0.5f, -0.5f),
//    Vector2f(-0.5f, 0.5f)
//)
//
//data class Slider(
//    val pos: Vector2f,
//    val dim: Vector2f,
//    val min: Float,
//    val max: Float,
//    var value: Float,
//    var cursor: Float = (value - min) / (max - min),
//    val rad: Vector2f = Vector2f(dim.x / 2f, dim.y / 2f),
//    val start: Vector2f = Vector2f(pos).sub(rad),
//    val end: Vector2f = Vector2f(pos).add(rad)
//)
//
//context(Window)
//fun Slider.update(mouse: Vector2f) {
//    if (mouseState[GLFW_MOUSE_BUTTON_1] && contains(mouse, pos, dim)) {
//        val width = end.x - start.x
//        cursor = (mouse.x - start.x) / width
//        value = min + cursor * (max - min)
//    }
//}
//
//val BUTTON = Vector2i(26, 496)
//val BUTTON_PRESSED = Vector2i(26, 496)
//val BUTTON_DIM = Vector2i(9, 9)
//val BUTTON_UV_INSETS = Vector2i(4, 4)
//val BUTTON_INSETS = Vector2f(1f)
//
////val SLIDER_
//
//fun sliderQuads(
//    font: Font,
//    uiColorEntity: Aggregator,
//    fontEntity: Aggregator,
//    pos: Vector2f,
//    dim: Vector2f,
//    state: Float,
//    min: Float, max: Float,
//    z: Int = 0,
//    cursorZ: Int = z + 1
//) {
//    val stopperDim = Vector2f(0.4f, dim.y * 1.25f)
//    val stopperRad = Vector2f(stopperDim.x / 2f, stopperDim.y / 2f)
//    val radX = dim.x / 2f
//    val radY = dim.y / 2f
//    val start = Vector2f(pos).sub(radX, 0f)
//    val end = Vector2f(pos).add(radX, 0f)
//    val cursor = Vector2f(pos).sub(radX, 0f).add(state * (end.x - start.x), 0f)
//    uiColorEntity.colorQuad(pos, Vector2f(dim.x, dim.y), Vector4f(51f/255f, 51f/255f, 51f/255f, 1f), z = z)
//    uiColorEntity.colorQuad(start.sub(stopperRad.x, 0f), Vector2f(stopperDim.x, dim.y), Vector4f(0f, 0f, 0f, 1f), z = z)
//    uiColorEntity.colorQuad(end.add(stopperRad.x, 0f), Vector2f(stopperDim.x, dim.y), Vector4f(0f, 0f, 0f, 1f), z = z)
//    uiColorEntity.colorQuad(cursor, stopperDim, Vector4f(252f/255f, 160f/255f, 68f/255f, 1f), z = cursorZ)
//    val fontSize = 0.01f * dim.y
//    fontEntity.fontQuads(font, start.sub(0f, radY).sub(0f, font.lineHeight(fontSize) / 2f), min.toString(), Vector3f(0f, 0f, 0f), size = fontSize, z = z)
//    fontEntity.fontQuads(font, Vector2f(pos).sub(0f, radY).sub(0f, font.lineHeight(fontSize) / 2f), "%.2f".format(min + (max - min) * state), Vector3f(0f, 0f, 0f), size = fontSize, z = z)
//    fontEntity.fontQuads(font, end.sub(0f, radY).sub(0f, font.lineHeight(fontSize) / 2f), max.toString(), Vector3f(0f, 0f, 0f), size = fontSize, z = z)
//}
//
////suspend fun Window.buttonQuads(
////    uiEntity: Aggregator,
////    fontEntity: Aggregator,
////    pos: Vector2f, dim: Vector2f,
////    font: Font? = null, label: String? = null,
////    fontSize: Float = 1f,
////    uiZ: Int = 0,
////    fontZ: Int = uiZ + 1
////) {
//////    val buttonUiEntity = uiEntity.aggregator(uiEntity.buffer.shader.quad * 9)
//////    val buttonFontEntity = fontEntity.aggregator(fontEntity.buffer.shader.quad * 128) //64 chars
//////    mouseEvent += { code, action ->
//////        val mouse = mouseWorld()
//////        val rad = Vector2f(dim.x / 2f, dim.y / 2f)
//////        val min = Vector2f(pos).sub(rad)
//////        val max = Vector2f(pos).add(rad)
//////        if (code == GLFW_MOUSE_BUTTON_1 && action == GLFW_RELEASE &&
//////            mouse.x > min.x && mouse.x < max.x &&
//////            mouse.y > min.y && mouse.y < max.y
//////        ) onPress()
//////    }
//////    onTick { _, _ ->
////        val mouse = mouseWorld()
////        val rad = Vector2f(dim.x / 2f, dim.y / 2f)
////        val min = Vector2f(pos).sub(rad)
////        val max = Vector2f(pos).add(rad)
////        val uv = if (mouseState[GLFW_MOUSE_BUTTON_1] &&
////            mouse.x > min.x && mouse.x < max.x &&
////            mouse.y > min.y && mouse.y < max.y
////        ) BUTTON_PRESSED else BUTTON
//////        buttonUiEntity.clear()
//////        buttonFontEntity.clear()
////        if (font != null && label != null) {
////            fontEntity.fontQuads(font, pos, label, Vector3f(252f / 255f, 160f / 255f, 68f / 255f), size = fontSize * 0.01f * dim.y, z = fontZ)
////            fontEntity.fontQuads(font, pos.sub(0.1f, 0.1f), label, Vector3f(0.15f, 0.15f, 0.15f), size = fontSize * 0.01f * dim.y, z = fontZ)
////        }
////        uiEntity.slicedQuads(
////            pos, Vector2f(BUTTON_INSETS).mul(2f, 2f).add(dim), BUTTON_INSETS,
////            uv, BUTTON_DIM, BUTTON_UV_INSETS,
////            z = uiZ
////        )
//////    }
////}
//
//fun Font.lineHeight(size: Float = 0.025f) = lineHeight * size
//fun Font.textWidth(string: String, size: Float = 0.025f): Float {
//    var width = 0f
//    string.forEach {
//        if (!chars.containsKey(it.code)) return@forEach
//        val glyph = chars[it.code]!!
//        width += glyph.advance * size
//    }
//    return width
//}
//fun Font.textRadius(string: String, size: Float = 0.025f): Float {
//    var width = 0f
//    string.forEach {
//        if (!chars.containsKey(it.code)) return@forEach
//        val glyph = chars[it.code]!!
//        width += glyph.advance * size
//    }
//    return width / 2f
//}
//
//fun Aggregator.fontQuads(font: Font, center: Vector2f, string: String, color: Vector3f, size: Float = 0.025f, z: Int = 0) {
//    if (len + buffer.shader.quad > max) {
//        println("out of bounds")
//        return
//    }
//    var rad = 0f
//    string.forEach {
//        if (!font.chars.containsKey(it.code)) return@forEach
//        val glyph = font.chars[it.code]!!
//        rad += glyph.advance * size
//    }
//    rad /= 2
//    val position = Vector2f(center).add(-rad, font.lineHeight * size / 2f)
//    string.forEach {
//        if (!font.chars.containsKey(it.code)) return@forEach
//        val glyph = font.chars[it.code]!!
//        val glyphPosition = position.copy()
//        glyphPosition.x += glyph.offsetX * size
//        glyphPosition.y -= glyph.offsetY * size
//        for (vertex in 0 until 6) {
//            val uvX = glyph.x + glyph.width * TOP_LEFT_CORNERS[vertex].x
//            val uvY = glyph.y + glyph.height - glyph.height * TOP_LEFT_CORNERS[vertex].y
//            buffer.float(pos + len++, glyphPosition.x + (glyph.width * size) * TOP_LEFT_CORNERS[vertex].x)
//            buffer.float(pos + len++, glyphPosition.y - (glyph.height * size) + (glyph.height * size) * TOP_LEFT_CORNERS[vertex].y)
//            buffer.float(pos + len++, z.toFloat())
//            buffer.float(pos + len++, (uvX + uvY * 512).toFloat())
//            buffer.vec3f(pos + len, color); len += 3
//        }
//        position.x += glyph.advance * size
//    }
//}
//
////fun Aggregator.textureQuad(pos: Vector2f, dim: Vector2f, uvPos: Vector2i, uvDim: Vector2i, z: Int = 0) {
////    if (len + buffer.shader.quad > max) {
////        println("out of bounds")
////        return
////    }
////    val acc = this@Aggregator
////    for (vertex in 0 until 6) {
////        val uvX = uvPos.x + uvDim.x * TOP_LEFT_CORNERS[vertex].x
////        val uvY = uvPos.y + uvDim.y * TOP_LEFT_CORNERS[vertex].y
////        buffer.float(acc.pos + acc.len++, pos.x + dim.x * CORNERS[vertex].x)
////        buffer.float(acc.pos + acc.len++, pos.y + dim.y * CORNERS[vertex].y)
////        buffer.float(acc.pos + acc.len++, z.toFloat())
////        buffer.float(acc.pos + acc.len++, (uvX + uvY * 512).toFloat())
////    }
////}
//
////val TOP_LEFT_CORNERS = arrayOf(
////    Vector2i(0, 0),
////    Vector2i(1, 0),
////    Vector2i(1, 1),
////    Vector2i(1, 1),
////    Vector2i(0, 0),
////    Vector2i(0, 1)
////)
////val CORNERS = arrayOf(
////    Vector2f(-0.5f, -0.5f),
////    Vector2f(0.5f, -0.5f),
////    Vector2f(0.5f, 0.5f),
////    Vector2f(0.5f, 0.5f),
////    Vector2f(-0.5f, -0.5f),
////    Vector2f(-0.5f, 0.5f)
////)
//
//val FAST_TOP_LEFT_CORNERS = arrayOf(
//    Vector2i(0, 0),
//    Vector2i(1, 0),
//    Vector2i(1, 1),
//    Vector2i(0, 1),
//)
//val FAST_CORNERS = arrayOf(
//    Vector2f(-0.5f, -0.5f),
//    Vector2f(0.5f, -0.5f),
//    Vector2f(0.5f, 0.5f),
//    Vector2f(-0.5f, 0.5f),
//)
//fun Aggregator.fastTexturedQuad(pos: Vector2f, dim: Vector2f, uvPos: Vector2i, uvDim: Vector2i, z: Int = 0) {
//    if (len + buffer.shader.fastQuad > max) {
//        println("out of bounds")
//        return
//    }
//    val rad = Vector2f(dim.x / 2f, dim.y / 2f)
//    val acc = this@Aggregator
//    for (vertex in 0 until 4) {
//        val uvX = uvPos.x + uvDim.x * FAST_TOP_LEFT_CORNERS[vertex].x
//        val uvY = uvPos.y + uvDim.y * FAST_TOP_LEFT_CORNERS[vertex].y
//        buffer.float(acc.pos + acc.len++, pos.x + rad.x * FAST_CORNERS[vertex].x)
//        buffer.float(acc.pos + acc.len++, pos.y + rad.y * FAST_CORNERS[vertex].y)
//        buffer.float(acc.pos + acc.len++, z.toFloat())
//        buffer.float(acc.pos + acc.len++, (uvX + uvY * 512).toFloat())
//    }
//}
//fun Aggregator.texturedQuad(atlas: Int, pos: Vector2f, dim: Vector2f, uvPos: Vector2i, uvDim: Vector2i, rotation: Float = 0f, pivot: Vector2f = pos, z: Int = 0) {
//    if (len + buffer.shader.quad > max) {
//        println("out of bounds")
//        return
//    }
//    val acc = this@Aggregator
//    for (vertex in 0 until 6) {
//        val uvX = uvPos.x + uvDim.x * TOP_LEFT_CORNERS[vertex].x
//        val uvY = uvPos.y + uvDim.y * TOP_LEFT_CORNERS[vertex].y
//        buffer.float(acc.pos + acc.len++, atlas.toFloat())
//        buffer.float(acc.pos + acc.len++, pos.x + dim.x * CORNERS[vertex].x)
//        buffer.float(acc.pos + acc.len++, pos.y + dim.y * CORNERS[vertex].y)
//        buffer.float(acc.pos + acc.len++, z.toFloat())
//        buffer.float(acc.pos + acc.len++, pivot.x)
//        buffer.float(acc.pos + acc.len++, pivot.y)
//        buffer.float(acc.pos + acc.len++, rotation)
//        buffer.float(acc.pos + acc.len++, (uvX + uvY * 512).toFloat())
//    }
//}
//
//
//fun Aggregator.slicedQuads(
//    atlas: Int,
//    pos: Vector2f, dim: Vector2f, insets: Vector2f,
//    uvTopLeft: Vector2i, uvDim: Vector2i, uvInsets: Vector2i,
//    z: Int = 0
//) {
//    if (len + buffer.shader.quad * 9 > max) {
//        println("out of bounds")
//        return
//    }
//    val rad = Vector2f(dim.x / 2f, dim.y / 2f)
//    val insetsRad = Vector2f(insets.x / 2f, insets.y / 2f)
//    for (i in 0 until 9) {
//        val x = i % 3 - 1
//        val y = i / 3 - 1
//        val insetX = 2 - i % 3 - 1
//        val insetY = 2 - i / 3 - 1
//        val slicePos = Vector2f().set(pos).add(rad.x * x, rad.y * y).add(insetsRad.x * insetX, insetsRad.y * insetY)
//        val sliceDim = Vector2f(
//            when(x) { -1, 1 -> insets.x else -> dim.x - insets.x * 2 },
//            when(y) { -1, 1 -> insets.y else -> dim.y - insets.y * 2 }
//        )
//        val sliceUvPos = Vector2i(
//            when(x) {
//                -1 -> uvTopLeft.x
//                0 -> uvTopLeft.x + uvInsets.x
//                else -> uvTopLeft.x + uvDim.x - uvInsets.x
//            },
//            when(y) {
//                -1 -> uvTopLeft.y
//                0 -> uvTopLeft.y + uvInsets.y
//                else -> uvTopLeft.y + uvDim.y - uvInsets.y
//            },
//        )
//        val sliceUvDim = Vector2i(
//            when(x) { -1, 1 -> uvInsets.x else -> uvDim.x - uvInsets.x * 2 },
//            when(y) { -1, 1 -> uvInsets.y else -> uvDim.y - uvInsets.y * 2 }
//        )
//        texturedQuad(atlas, slicePos, sliceDim, sliceUvPos, sliceUvDim, z = z)
//    }
//}
//
//fun Aggregator.colorTriangle(a: Vector2f, b: Vector2f, c: Vector2f, color: Vector4f, z: Int = 0) {
//    if (len + 21 > max) {
//        println("out of bounds")
//        return
//    }
//    buffer.vec3f(pos + len, Vector3f(a, z.toFloat())); len += 3
//    buffer.vec4f(pos + len, color); len += 4
//    buffer.vec3f(pos + len, Vector3f(b, z.toFloat())); len += 3
//    buffer.vec4f(pos + len, color); len += 4
//    buffer.vec3f(pos + len, Vector3f(c, z.toFloat())); len += 3
//    buffer.vec4f(pos + len, color); len += 4
//}
//
//fun Aggregator.fovTriangle(a: Vector2f, b: Vector2f, c: Vector2f, z: Int = 0) {
//    if (len + 9 > max) {
//        println("out of bounds")
//        return
//    }
//    buffer.vec3f(pos + len, Vector3f(a, z.toFloat())); len += 3
//    buffer.vec3f(pos + len, Vector3f(b, z.toFloat())); len += 3
//    buffer.vec3f(pos + len, Vector3f(c, z.toFloat())); len += 3
//}
//
//fun Aggregator.colorQuad(pos: Vector2f, dim: Vector2f, color: Vector4f, z: Int = 0) {
//    if (len + buffer.shader.quad > max) {
//        println("out of bounds")
//        return
//    }
//    val acc = this@Aggregator
//    for (vertex in 0 until 6) {
//        buffer.float(acc.pos + acc.len++, pos.x + dim.x * CORNERS[vertex].x)
//        buffer.float(acc.pos + acc.len++, pos.y + dim.y * CORNERS[vertex].y)
//        buffer.float(acc.pos + acc.len++, z.toFloat())
//        buffer.vec4f(acc.pos + len, color); acc.len += 4
//    }
//}
//
//val TEXTURE_IDS = intArrayOf(
//    GL_TEXTURE0, GL_TEXTURE1, GL_TEXTURE2, GL_TEXTURE3,
//    GL_TEXTURE4, GL_TEXTURE5, GL_TEXTURE6, GL_TEXTURE7
//)
//
//fun Window.drawFastQuadTextured(ui: Boolean, atlas: Int): ShaderBuffer.(Int, Int) -> (Unit) = { start, count ->
//    println()
//    println("Shader program: ${shader.program}, ${shader.stride}")
//    for (i in 0 until shader.stride) {
//        println(data[i])
//    }
//    glBindBuffer(GL_ARRAY_BUFFER, vbo)
//    glBufferSubData(GL_ARRAY_BUFFER, 0, data)
//    shader.program.let { program ->
//        glUseProgram(program)
//        glUniform1i(glGetUniformLocation(program, "TEX_SAMPLER"), 0)
//        if (ui) {
//            uiProjection.get(matrix)
//            uiView.get(matrix)
//        } else {
//            projection.get(matrix)
//            view.get(matrix)
//        }
//        glUniformMatrix4fv(glGetUniformLocation(program, "projection"), false, matrix)
//        glUniformMatrix4fv(glGetUniformLocation(program, "view"), false, matrix)
//    }
//    glActiveTexture(GL_TEXTURE0)
//    glBindTexture(GL_TEXTURE_2D, atlas)
//    glBindVertexArray(vao)
//    shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
//    println("ye: ${(count / shader.stride / 4) * 6}")
//    glDrawElements(GL_TRIANGLES, (count / shader.stride / 4) * 6, GL_UNSIGNED_INT, 0)
//    shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
//    glBindVertexArray(0)
//    glBindTexture(GL_TEXTURE_2D, 0)
//    glActiveTexture(0)
//    glUseProgram(0)
//}
//
//fun Window.drawTextured(ui: Boolean, vararg atlases: Int): ShaderBuffer.(Int, Int) -> (Unit) = { start, count ->
//    println()
//    println("Shader program: ${shader.program}, ${shader.stride}")
//    println("drawing texture using atlas ${data[0]}")
//    glBindBuffer(GL_ARRAY_BUFFER, vbo)
//    glBufferSubData(GL_ARRAY_BUFFER, 0, data)
//    shader.program.let { program ->
//        glUseProgram(program)
//        glUniform1iv(glGetUniformLocation(program, "SAMPLERS"), IntArray(8) { it })
//        if (ui) {
//            uiProjection.get(matrix)
//            uiView.get(matrix)
//        } else {
//            projection.get(matrix)
//            view.get(matrix)
//        }
//        glUniformMatrix4fv(glGetUniformLocation(program, "projection"), false, matrix)
//        glUniformMatrix4fv(glGetUniformLocation(program, "view"), false, matrix)
//    }
//    atlases.forEach { atlas ->
//        glActiveTexture(TEXTURE_IDS[ATLASES.indexOf(atlas)])
//        glBindTexture(GL_TEXTURE_2D, atlas)
//    }
//    glBindVertexArray(vao)
//    shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
//    glDrawArrays(GL_TRIANGLES, start / shader.stride, count / shader.stride)
//    shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
//    glBindVertexArray(0)
//    glBindTexture(GL_TEXTURE_2D, 0)
//    glActiveTexture(0)
//    glUseProgram(0)
//}
//
//fun Window.draw(ui: Boolean): ShaderBuffer.(Int, Int) -> (Unit) = { start, count ->
//    glBindBuffer(GL_ARRAY_BUFFER, vbo)
//    glBufferSubData(GL_ARRAY_BUFFER, 0, data)
//    shader.program.let {
//        glUseProgram(it)
//        if (ui) {
//            uiProjection.get(matrix)
//            uiView.get(matrix)
//        } else {
//            projection.get(matrix)
//            view.get(matrix)
//        }
//        glUniformMatrix4fv(glGetUniformLocation(it, "projection"), false, matrix)
//        glUniformMatrix4fv(glGetUniformLocation(it, "view"), false, matrix)
//    }
//    glBindVertexArray(vao)
//    shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
//    glDrawArrays(GL_TRIANGLES, start / shader.stride, count / shader.stride)
//    shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
//    glBindVertexArray(0)
//}
//
//interface Window {
//    val keyEvent: ArrayList<suspend (Int, Int) -> (Unit)>
//    val mouseEvent: ArrayList<suspend (Int, Int) -> (Unit)>
//    val keyState: BitSet
//    val mouseState: BitSet
//    val window: Long
//    var dim: Vector2i
//    var title: Any
//    var camera: Vector2f
//    val projection: Matrix4f
//    val view: Matrix4f
//    val uiProjection: Matrix4f
//    val uiView: Matrix4f
//    val matrix: FloatArray
//
//    fun move()
//
//    suspend fun onTick(block: suspend (Float, Float) -> (Unit))
//    suspend fun onFixed(fps: Int, block: suspend (Float, Float) -> (Unit))
//    suspend fun onFixed(fps: () -> (Int), block: suspend (Float, Float) -> (Unit))
//}
//
//suspend fun window(block: suspend Window.() -> (Unit)) {
//    createPrint(System.err).set()
//    if (!glfwInit()) error("Unable to initialize GLFW")
//    glfwDefaultWindowHints()
//    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
//    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
//    val id = glfwCreateWindow(DEFAULT_SIZE.x, DEFAULT_SIZE.y, "", NULL, NULL)
//    if (id == NULL) error("Failed to create the GLFW window")
//    val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
//    glfwSetWindowPos(
//        id,
//        (videoMode.width() - DEFAULT_SIZE.x) / 2,
//        (videoMode.height() - DEFAULT_SIZE.y) / 2
//    )
//    glfwMakeContextCurrent(id)
//    glfwSwapInterval(0)
//    glfwShowWindow(id)
//    createCapabilities()
//    glEnable(GL_BLEND)
//    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
//    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
//    glfwSetWindowSize(id, DEFAULT_SIZE.x, DEFAULT_SIZE.y)
//    glViewport(0, 0, DEFAULT_SIZE.x, DEFAULT_SIZE.y)
//    glfwSetWindowSizeCallback(id) { _, nextWidth, nextHeight ->
//        glfwSetWindowSize(id, nextWidth, nextHeight)
//        glViewport(0, 0, nextWidth, nextHeight)
//    }
//    val onTick = ArrayList<suspend (Float, Float) -> (Unit)>()
//    val window = object : Window {
//        override val projection = Matrix4f().setOrtho(-40f, 40f, -22.5f, 22.5f, 0f, 100f)
//        override val view = Matrix4f().identity().lookAt(
//            Vector3f(0f, 0f, 20f),
//            Vector3f(0f, 0f, -1f),
//            Vector3f(0f, 1f, 0f)
//        )
//        override val uiProjection = Matrix4f().setOrtho(-40f, 40f, -22.5f, 22.5f, 0f, 100f)
//        override val uiView = Matrix4f().identity().lookAt(
//            Vector3f(0f, 0f, 20f),
//            Vector3f(0f, 0f, -1f),
//            Vector3f(0f, 1f, 0f)
//        )
//        override val matrix = FloatArray(16)
//        override fun move() {
//            view.identity().lookAt(
//                Vector3f((camera.x * 32f).toInt() / 32f, (camera.y * 32f).toInt() / 32f, 20f),
//                Vector3f((camera.x * 32f).toInt() / 32f, (camera.y * 32f).toInt() / 32f, -1f),
//                Vector3f(0f, 1f, 0f)
//            )
//        }
//        override val keyEvent = ArrayList<suspend (Int, Int) -> Unit>()
//        override val mouseEvent = ArrayList<suspend (Int, Int) -> Unit>()
//        override val keyState = BitSet()
//        override val mouseState = BitSet()
//        override val window = id
//        override var camera = Vector2f(0f, 0f)
//            set(value) {
//                field = value
//                move()
//            }
//        override var dim: Vector2i
//            get() {
//                val width = createIntBuffer(1)
//                val height = createIntBuffer(1)
//                glfwGetWindowSize(id, width, height)
//                return Vector2i(width.get(), height.get())
//            }
//            set(value) {
//                glfwSetWindowSize(id, value.x, value.y)
//                glViewport(0, 0, value.x, value.y)
//            }
//        override var title: Any = ""
//            set(value) {
//                glfwSetWindowTitle(id, value.toString())
//                field = value
//            }
//        override suspend fun onTick(block: suspend (Float, Float) -> (Unit)) = onTick.plusAssign(block)
//        override suspend fun onFixed(fps: Int, block: suspend (Float, Float) -> (Unit)) {
//            var remainder = 0f
//            val rate = 1f / fps
//            onTick += { delta, elapsed ->
//                val from = remainder
//                val to = from + delta
//                remainder = if (to > rate) { block(to, elapsed); to % rate } else to
//            }
//        }
//        override suspend fun onFixed(fps: () -> (Int), block: suspend (Float, Float) -> (Unit)) {
//            var remainder = 0f
//            val rate = 1f / fps()
//            onTick += { delta, elapsed ->
//                val from = remainder
//                val to = from + delta
//                remainder = if (to > rate) { block(to, elapsed); to % rate } else to
//            }
//        }
//    }
//    glfwSetKeyCallback(id) { _, code, _, action, _ ->
//        runBlocking { window.keyEvent.forEach { it(code, action) } }
//        if (action == GLFW_PRESS) window.keyState.set(code)
//        else if (action == GLFW_RELEASE) window.keyState.clear(code)
//    }
//    glfwSetMouseButtonCallback(id) { _, code, action, _ ->
//        runBlocking { window.mouseEvent.forEach { it(code, action) } }
//        if (action == GLFW_PRESS) window.mouseState.set(code)
//        else if (action == GLFW_RELEASE) window.mouseState.clear(code)
//    }
//    block(window)
//    var elapsed = 0L
//    var delta: Long
//    var last = System.nanoTime()
//    while (!glfwWindowShouldClose(id)) {
//        val now = System.nanoTime()
//        delta = now - last
//        elapsed += delta
//        last = now
//        val deltaSeconds = delta / 1000000000f
//        val elapsedSeconds = elapsed / 1000000000f
//        onTick.forEach { it(deltaSeconds, elapsedSeconds) }
//    }
//    exitProcess(0)
//}