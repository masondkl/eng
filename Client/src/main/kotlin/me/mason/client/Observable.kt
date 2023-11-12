package me.mason.client

interface Observable<T> {
    val default: suspend () -> (T)
    suspend operator fun invoke(): T
    suspend operator fun invoke(value: T, notify: Boolean = true)
    suspend operator fun invoke(block: suspend (T) -> (Unit))
    suspend fun change(block: suspend (T, T) -> (Unit))
    suspend fun notify()
}

suspend inline fun <reified T> Observable(noinline default: suspend () -> (T)): Observable<T> {
    var from = default()
    var to = default()
    var events: (suspend (T, T) -> (Unit))? = null
    return object : Observable<T> {
        override val default = default
        override suspend fun invoke(): T = to
        override suspend fun notify() = events?.let { it(from, to) } ?: Unit
        override suspend fun invoke(value: T, notify: Boolean) {
            from = to
            to = value
            if (notify) events?.let { it(from, to) }
        }
        override suspend fun invoke(block: suspend (T) -> Unit) {
            val before = events
            events = if (before == null) { _, to -> block(to) }
                else { from, to -> before(from, to); block(to) }
        }
        override suspend fun change(block: suspend (T, T) -> (Unit)) {
            val before = events
            events = if (before == null) { from, to -> block(from, to) }
                else { from, to -> before(from, to); block(from, to) }
        }
    }
}

suspend inline fun <T, reified R> Observable<T>.map(crossinline map: suspend (T) -> (R)): Observable<R> {
    val new = Observable { map(default()) }
    this { new(map(it)) }
    return new
}
suspend inline fun <reified T> Observable<T>.filter(crossinline predicate: suspend (T) -> (Boolean)): Observable<T> {
    val new = Observable { default() }
    this { if (predicate(it)) new(it) }
    return new
}
suspend inline fun <T, reified A> Observable<T>.fold(acc: A, crossinline fold: suspend (A, T) -> (A)): Observable<A> {
    var copy = acc
    val new = Observable { copy }
    this {
        val result = fold(copy, it)
        copy = result; new(result)
    }
    return new
}