package me.mason.client

import org.lwjgl.openal.AL10
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.system.exitProcess


/*
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


/**
 *
 * Utitlity class for loading wavefiles.
 *
 * @author Brian Matzon <brian></brian>@matzon.dk>
 * @version $Revision$
 * $Id$
 */
class WaveData private constructor(
    /** actual wave data  */
    val data: ByteBuffer,
    /** format type of data  */
    val format: Int, val samplerate: Int
) {
    fun dispose() {
        data.clear()
    }
    companion object {

        fun create(ais: AudioInputStream): WaveData? {
            //get format of data
            val audioformat = ais.format

            // get channels
            var channels = 0
            if (audioformat.channels == 1) {
                if (audioformat.sampleSizeInBits == 8) {
                    channels = AL10.AL_FORMAT_MONO8
                } else if (audioformat.sampleSizeInBits == 16) {
                    channels = AL10.AL_FORMAT_MONO16
                } else {
                    assert(false) { "Illegal sample size" }
                }
            } else if (audioformat.channels == 2) {
                if (audioformat.sampleSizeInBits == 8) {
                    channels = AL10.AL_FORMAT_STEREO8
                } else if (audioformat.sampleSizeInBits == 16) {
                    channels = AL10.AL_FORMAT_STEREO16
                } else {
                    assert(false) { "Illegal sample size" }
                }
            } else {
                assert(false) { "Only mono or stereo is supported" }
            }

            //read data into buffer
            var buffer: ByteBuffer? = null
            try {
                var available = ais.available()
                if (available <= 0) {
                    available = ais.format.channels * ais.frameLength.toInt() * ais.format.sampleSizeInBits / 8
                }
                val buf = ByteArray(ais.available())
                var read = 0
                var total = 0
                while (ais.read(buf, total, buf.size - total).also { read = it } != -1
                    && total < buf.size) {
                    total += read
                }
                buffer = convertAudioBytes(
                    buf,
                    audioformat.sampleSizeInBits == 16,
                    if (audioformat.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
                )
            } catch (ioe: IOException) {
                return null
            }


            //create our result
            val wavedata = WaveData(buffer, channels, audioformat.sampleRate.toInt())

            //close stream
            try {
                ais.close()
            } catch (ioe: IOException) {
            }
            return wavedata
        }

        fun create(buffer: ByteArray): WaveData {
            return try {
                create(
                    AudioSystem.getAudioInputStream(
                        BufferedInputStream(ByteArrayInputStream(buffer))
                    )
                )!!
            } catch (e: Exception) {
                println(("Unable to create from byte array, ${e.message}"))
                exitProcess(-1)
            }
        }

        private fun convertAudioBytes(audio_bytes: ByteArray, two_bytes_data: Boolean, order: ByteOrder): ByteBuffer {
            val dest = ByteBuffer.allocateDirect(audio_bytes.size)
            dest.order(ByteOrder.nativeOrder())
            val src = ByteBuffer.wrap(audio_bytes)
            src.order(order)
            if (two_bytes_data) {
                val dest_short = dest.asShortBuffer()
                val src_short = src.asShortBuffer()
                while (src_short.hasRemaining()) dest_short.put(src_short.get())
            } else {
                while (src.hasRemaining()) dest.put(src.get())
            }
            dest.rewind()
            return dest
        }
    }
}