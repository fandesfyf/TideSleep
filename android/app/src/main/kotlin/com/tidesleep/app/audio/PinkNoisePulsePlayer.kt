package com.tidesleep.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.tidesleep.app.data.SafetyConfig

/**
 * 生成并播放短促粉红噪声（1/f）脉冲。
 * 复用 STREAM AudioTrack，每脉冲仅 write，不在每次脉冲后 stop/flush。
 */
class PinkNoisePulsePlayer(
    private val config: SafetyConfig,
) : PulsePlayer {

    private val sampleRate = 44_100
    private var streamTrack: AudioTrack? = null
    private val generator = PinkNoiseGenerator()

    override fun playPulse() {
        val durationMs = config.pulseDurationMs
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val buffer = generator.generatePulse(numSamples)
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
}
