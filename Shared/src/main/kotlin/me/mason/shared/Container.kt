package me.mason.shared

import java.util.*

//interface Container<T> {
//    val enabled: BitSet
//    val states: Array<T>
//}
//inline fun <reified T> Container(size: Int, crossinline default: () -> T) = object : Container<T> {
//    override val enabled = BitSet(size)
//    override val states = Array(size) { default() }
//}
////fun <T> Container<T>.set(index: Int, value: T) {
////    enabled.set(index)
////    states[index] = value
////}
//fun Container<*>.clear(id: Int) {
//    enabled.clear(id)
//}
//fun <T> Container<T>.next(): Pair<Int, T> {
//    val nextIndex = enabled.nextClearBit(0)
//    enabled.set(nextIndex)
//    return Pair(nextIndex, states[nextIndex])
//}
//
//fun Container<*>.cardinality() = enabled.cardinality()
//inline fun <T> Container<T>.forEach(block: T.() -> (Unit)) = enabled.forEach { states[it].block() }
//inline fun <T> Container<T>.clearIf(block: T.() -> (Boolean)) = enabled.clearIf { states[it].block() }
//inline fun <T> Container<T>.forEachIndexed(block: T.(Int) -> (Unit)) = enabled.forEachIndexed { idx, it -> states[it].block(idx) }
//inline fun <T> Container<T>.first(block: T.() -> (Boolean)) = states[enabled.first { states[it].block() }]
//inline fun <T> Container<T>.firstOrNull(block: T.() -> (Boolean)) = enabled.first { states[it].block() }.let {
//    if (it == -1) null else states[it]
//}
//inline fun <T> Container<T>.all(block: T.() -> (Boolean)) = enabled.all { states[it].block() }
//inline fun <T> Container<T>.any(block: T.() -> (Boolean)) = enabled.any { states[it].block() }
//inline fun <T> Container<T>.none(block: T.() -> (Boolean)) = enabled.none { states[it].block() }
//inline fun <T> Container<T>.count(block: T.() -> (Boolean)) = enabled.count { states[it].block() }
