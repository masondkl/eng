package me.mason.shared

import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.ArrayList
import kotlin.time.Duration.Companion.seconds

val SECOND_NANOS = 1.seconds.inWholeNanoseconds
val timeMillis: Long get() = System.currentTimeMillis()
val timeNanos: Long get() = System.nanoTime()

inline fun BitSet.forEach(block: (Int) -> (Unit)) {
    var idx = nextSetBit(0)
    while(idx != -1) {
        block(idx)
        idx = nextSetBit(idx + 1)
    }
}
fun BitSet.first(block: (Int) -> (Boolean)): Int {
    val idx = nextSetBit(0)
    while(idx != -1) {
        if (block(idx)) return idx
    }
    return -1
}
fun BitSet.all(block: (Int) -> (Boolean)): Boolean {
    var idx = nextSetBit(0)
    while(idx != -1) {
        if (!block(idx)) return false
        idx = nextSetBit(idx + 1)
    }; return true
}
fun BitSet.any(block: (Int) -> (Boolean)): Boolean {
    var idx = nextSetBit(0)
    while(idx != -1) {
        if (block(idx)) return true
        idx = nextSetBit(idx + 1)
    }; return false
}
fun BitSet.none(block: (Int) -> (Boolean)): Boolean {
    var idx = nextSetBit(0)
    while(idx != -1) {
        if (block(idx)) return false
        idx = nextSetBit(idx + 1)
    }; return true
}
fun BitSet.count(block: (Int) -> (Boolean)): Int {
    var count = 0
    var idx = nextSetBit(0)
    while(idx != -1) {
        if (block(idx)) count++
        idx = nextSetBit(idx + 1)
    }; return count
}