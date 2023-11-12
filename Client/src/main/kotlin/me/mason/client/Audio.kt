package me.mason.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.lwjgl.openal.AL10.*
import java.util.*
import java.util.concurrent.locks.LockSupport.parkNanos

interface AudioSource {
    val buffers: Array<Int>
    val sources: Array<Int>
    suspend fun submit(data: WaveData, block: suspend (Int) -> (Unit) = {})
}

fun AudioSource(size: Int) = object : AudioSource {
    override val buffers = Array(size) { alGenBuffers() }
    override val sources = Array(size) { alGenSources() }
    val playing = BitSet(size)
    val submission = Mutex()
    override suspend fun submit(data: WaveData, block: suspend (Int) -> (Unit)) {
        val next = submission.withLock {
            playing.run {
                nextClearBit(0).also { set(it) }
            }
        }
        alBufferData(buffers[next], data.format, data.data, data.samplerate)
        alSourcei(sources[next], AL_BUFFER, buffers[next])
        alSourcef(sources[next], AL_GAIN, 0.025f)
        block(sources[next])
        alSourcePlay(sources[next])
        CoroutineScope(Dispatchers.Default).launch {
            while (alGetSourcei(sources[next], AL_SOURCE_STATE) != AL_STOPPED) parkNanos(50)
            submission.withLock { playing.clear(next) }
        }
    }
}