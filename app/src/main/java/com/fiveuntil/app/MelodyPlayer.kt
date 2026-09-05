package com.fiveuntil.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.sin

/**
 * Plays a short monophonic melody once through AudioTrack, then stops.
 * Not an OS alarm — no looping, no persistent ringing.
 */
object MelodyPlayer {
    @Volatile private var track: AudioTrack? = null
    private val handler = Handler(Looper.getMainLooper())

    fun playOnce() {
        stop()
        Thread {
            try {
                val sampleRate = 22050
                val notes = intArrayOf(523, 659, 784, 659, 880, 784) // C E G E A G
                val msPerNote = 180
                val gapMs = 40
                val totalSamples = notes.size * ((sampleRate * (msPerNote + gapMs)) / 1000)
                val buf = ShortArray(totalSamples)
                var idx = 0
                for (freq in notes) {
                    val nSamples = sampleRate * msPerNote / 1000
                    for (i in 0 until nSamples) {
                        if (idx >= buf.size) break
                        val t = i.toDouble() / sampleRate
                        val env = when {
                            i < nSamples / 10 -> i.toDouble() / (nSamples / 10)
                            i > nSamples * 8 / 10 -> (nSamples - i).toDouble() / (nSamples / 5)
                            else -> 1.0
                        }.coerceIn(0.0, 1.0)
                        val sample = (sin(2.0 * Math.PI * freq * t) * 0.35 * env * Short.MAX_VALUE).toInt()
                        buf[idx++] = sample.toShort()
                    }
                    val gap = sampleRate * gapMs / 1000
                    idx = (idx + gap).coerceAtMost(buf.size)
                }
                val at = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buf.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                at.write(buf, 0, idx)
                track = at
                at.play()
                val durationMs = (idx * 1000L / sampleRate) + 50L
                handler.postDelayed({ stop() }, durationMs)
            } catch (_: Exception) {
                stop()
            }
        }.start()
    }

    fun stop() {
        try {
            track?.stop()
        } catch (_: Exception) {
        }
        try {
            track?.release()
        } catch (_: Exception) {
        }
        track = null
    }
}
