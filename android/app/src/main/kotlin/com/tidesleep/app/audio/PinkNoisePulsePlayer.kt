package com.tidesleep.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.tidesleep.app.data.SafetyConfig
import kotlin.math.pow
import kotlin.random.Random

/**
 * 生成并播放短促粉红噪声（1/f）脉冲。
 * 复用 STREAM AudioTrack，每脉冲仅 write，不在每次脉冲后 stop/flush。
 */
class PinkNoisePulsePlayer(
    private val config: SafetyConfig,
) : PulsePlayer {

    private val sampleRate = 44_100
    private var streamTrack: AudioTrack? = null

    override fun playPulse() {
        val durationMs = config.pulseDurationMs
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val buffer = generatePinkNoisePulse(numSamples)
        val track = obtainStreamTrack()
        track.write(buffer, 0, buffer.size)
    }

    override fun release() {
        streamTrack?.let { track ->
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.stop()
                track.flush()
            }
            track.release()
        }
        streamTrack = null
    }

    private fun obtainStreamTrack(): AudioTrack {
        val existing = streamTrack
        if (existing != null && existing.state == AudioTrack.STATE_INITIALIZED) {
            if (existing.playState != AudioTrack.PLAYSTATE_PLAYING) {
                existing.play()
            }
            return existing
        }

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
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
            .setBufferSizeInBytes(minBuffer.coerceAtLeast(sampleRate / 5))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.setVolume(config.volumePercent / 100f)
        track.play()
        streamTrack = track
        return track
    }

    private fun generatePinkNoisePulse(numSamples: Int): ShortArray {
        val output = ShortArray(numSamples)
        var b0 = 0.0
        var b1 = 0.0
        var b2 = 0.0
        var b3 = 0.0
        var b4 = 0.0
        var b5 = 0.0
        var b6 = 0.0

        val amplitude = Short.MAX_VALUE * 0.35

        for (i in 0 until numSamples) {
            val white = Random.nextDouble(-1.0, 1.0)
            b0 = 0.99886 * b0 + white * 0.0555179
            b1 = 0.99332 * b1 + white * 0.0750759
            b2 = 0.96900 * b2 + white * 0.1538520
            b3 = 0.86650 * b3 + white * 0.3104856
            b4 = 0.55000 * b4 + white * 0.5329522
            b5 = -0.7616 * b5 - white * 0.0168980
            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
            b6 = white * 0.115926

            val envelope = fadeEnvelope(i, numSamples)
            val sample = (pink / 5.0 * amplitude * envelope).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output[i] = sample.toShort()
        }
        return output
    }

    private fun fadeEnvelope(index: Int, total: Int): Double {
        val fadeSamples = (total * 0.15).toInt().coerceAtLeast(1)
        return when {
            index < fadeSamples -> index.toDouble() / fadeSamples
            index > total - fadeSamples -> (total - index).toDouble() / fadeSamples
            else -> 1.0
        }.pow(0.8)
    }
}
