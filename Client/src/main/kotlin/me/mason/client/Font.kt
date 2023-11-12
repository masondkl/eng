package me.mason.client

import java.nio.file.Path
import kotlin.io.path.readLines

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

fun Font(path: Path): Font {
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
fun Font.lineHeight(size: Float = 0.025f) = lineHeight * size
fun Font.textWidth(string: String, size: Float = 0.025f): Float {
    var width = 0f
    string.forEach {
        if (!chars.containsKey(it.code)) return@forEach
        val glyph = chars[it.code]!!
        width += glyph.advance * size
    }
    return width
}
fun Font.textRadius(string: String, size: Float = 0.025f): Float {
    var width = 0f
    string.forEach {
        if (!chars.containsKey(it.code)) return@forEach
        val glyph = chars[it.code]!!
        width += glyph.advance * size
    }
    return width / 2f
}