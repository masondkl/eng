//package me.mason.client.testing
//
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//import me.mason.shared.timeMillis
//import org.lwjgl.openal.AL
//import org.lwjgl.openal.AL10.*
//import org.lwjgl.openal.ALC
//import org.lwjgl.openal.ALC10.*
//import java.nio.ByteBuffer
//import java.nio.IntBuffer
//import java.nio.file.Paths
//import java.util.*
//import java.util.concurrent.atomic.AtomicReferenceArray
//import java.util.concurrent.locks.LockSupport.parkNanos
//import kotlin.io.path.readBytes
//import kotlin.math.sin
//import kotlin.system.exitProcess
//import kotlin.time.Duration.Companion.milliseconds
//
//val START = timeMillis
//
//interface AudioSource {
//    val buffers: Array<Int>
//    val sources: Array<Int>
//    suspend fun submit(data: WaveData, block: suspend (Int) -> (Unit) = {})
//}
//
//fun AudioSource(size: Int) = object : AudioSource {
//    override val buffers = Array(size) { alGenBuffers() }
//    override val sources = Array(size) { alGenSources() }
//    val playing = BitSet(size)
//    val submission = Mutex()
//    override suspend fun submit(data: WaveData, block: suspend (Int) -> (Unit)) {
//        val next = submission.withLock {
//            playing.run {
//                nextClearBit(0).also { set(it) }
//            }
//        }
//        alBufferData(buffers[next], data.format, data.data, data.samplerate)
//        alSourcei(sources[next], AL_BUFFER, buffers[next])
//        alSourcef(sources[next], AL_GAIN, 0.025f)
//        block(sources[next])
//        alSourcePlay(sources[next])
//        CoroutineScope(Dispatchers.Default).launch {
//            while (alGetSourcei(sources[next], AL_SOURCE_STATE) != AL_STOPPED) parkNanos(50)
//            submission.withLock { playing.clear(next) }
//        }
//    }
//}
//
//suspend fun main() {
//    val device: Long = alcOpenDevice(null as ByteBuffer?)
//    val deviceCaps = ALC.createCapabilities(device)
//    val context: Long = alcCreateContext(device, null as IntBuffer?)
//    alcMakeContextCurrent(context)
//    AL.createCapabilities(deviceCaps)
//    val source = AudioSource(64)
//    val data = WaveData.create(Paths.get("shoot.wav").readBytes())
//    while(true) {
//        parkNanos(200.milliseconds.inWholeNanoseconds)
//        source.submit(data)
//    }
////    val bufferId = alGenBuffers()
////    if (alGetError() != AL_NO_ERROR) exitProcess(-1)
////    val waveFile = WaveData.create(Paths.get("shoot.wav").readBytes())
////    alBufferData(bufferId, waveFile.format, waveFile.data, waveFile.samplerate)
////    val sourceId = alGenSources()
////    if (alGetError() != AL_NO_ERROR) exitProcess(-1)
////    alSourcei(sourceId, AL_BUFFER, bufferId)
////    alSourcef(sourceId, AL_GAIN, 0.025f)
////    alSourcef(sourceId, AL_PITCH, 1f + sin(timeMillis - START / 10.0).toFloat() * 0.1f)
////
////    println("Waiting for sound to complete...")
////    var x = 4f
////    while (true) {
////        x -= 0.3f
////        alListener3f(AL_POSITION, 0f, x, 0f)
////        alSource3f(sourceId, AL_POSITION, 4f, 0f, 0f)
////        alSourcef(sourceId, AL_PITCH, 1f + sin(timeMillis - START / 10.0).toFloat() * 0.1f)
////        alSourcePlay(sourceId)
////        while (true) {
////            parkNanos(500.milliseconds.inWholeNanoseconds)
////            if (alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_STOPPED) break
////            print(".")
////        }
////        print("stop\n")
////    }
//}