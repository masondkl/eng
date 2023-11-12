package me.mason.shared

import kotlinx.coroutines.runBlocking
import org.joml.Vector2f
import java.util.*
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.ArrayList
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds

val SECOND_NANOS = 1.seconds.inWholeNanoseconds
val timeMillis: Long get() = System.currentTimeMillis()
val timeNanos: Long get() = System.nanoTime()

fun Vector2f.normal(): Vector2f {
    val mag = sqrt(x * x + y * y)
    if (mag == 0f) return this
    x /= mag
    y /= mag
    return this
}

inline fun BitSet.forEach(block: (Int) -> (Unit)) {
    var idx = nextSetBit(0)
    while(idx != -1) {
        block(idx)
        idx = nextSetBit(idx + 1)
    }
}
//inline fun BitSet.clearIf(block: (Int) -> (Boolean)) {
//    var idx = nextSetBit(0)
//    while(idx != -1) {
//        if (block(idx)) clear(idx)
//        idx = nextSetBit(idx + 1)
//    }
//}
inline fun BitSet.forEachIndexed(block: (Int, Int) -> (Unit)) {
    var loopIndex = 0
    var idx = nextSetBit(0)
    while(idx != -1) {
        block(loopIndex++, idx)
        idx = nextSetBit(idx + 1)
    }
}
//inline fun BitSet.first(block: (Int) -> (Boolean)): Int {
//    var idx = nextSetBit(0)
//    while(idx != -1) {
//        if (block(idx)) return idx
//        idx = nextSetBit(idx + 1)
//    }
//    return -1
//}
//inline fun BitSet.all(block: (Int) -> (Boolean)): Boolean {
//    var idx = nextSetBit(0)
//    while(idx != -1) {
//        if (!block(idx)) return false
//        idx = nextSetBit(idx + 1)
//    }; return true
//}
//inline fun BitSet.any(block: (Int) -> (Boolean)): Boolean {
//    var idx = nextSetBit(0)
//    while(idx != -1) {
//        if (block(idx)) return true
//        idx = nextSetBit(idx + 1)
//    }; return false
//}
//inline fun BitSet.none(block: (Int) -> (Boolean)): Boolean {
//    var idx = nextSetBit(0)
//    while(idx != -1) {
//        if (block(idx)) return false
//        idx = nextSetBit(idx + 1)
//    }; return true
//}
//inline fun BitSet.count(block: (Int) -> (Boolean)): Int {
//    var count = 0
//    var idx = nextSetBit(0)
//    while(idx != -1) {
//        if (block(idx)) count++
//        idx = nextSetBit(idx + 1)
//    }; return count
//}