package com.tidesleep.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.tidesleep.app.data.SafetyConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 连续粉红噪声播放器：点击即播，循环写入 STREAM AudioTrack，直至 stop。
 */
class ContinuousPinkNoisePlayer(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val sampleRate = 44_100
    private val generator = PinkNoiseGenerator()
    private val isPlaying = AtomicBoolean(false)
    private var audioTrack: AudioTrack? = null
    private var writeJob: Job? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var volumePercent: Int = SafetyConfig.demo().volumePercent

    val playing: Boolean
        get() = isPlaying.get()

    fun start(config: SafetyConfig) {
        if (isPlaying.get()) {
            updateVolume(config.volumePercent)
            return
        }
        volumePercent = config.volumePercent
        if (!requestAudioFocus()) return

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = minBuffer.coerceAtLeast(sampleRate / 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.setVolume(volumePercent / 100f)
        track.play()
        audioTrack = track
        isPlaying.set(true)

        val chunk = ShortArray(bufferSize / 2)
        writeJob = scope.launch(Dispatchers.IO) {
            while (isActive && isPlaying.get()) {
                generator.fillBuffer(chunk)
                val written = track.write(chunk, 0, chunk.size)
                if (written < 0) break
            }
        }
    }

    fun updateVolume(percent: Int) {
        volumePercent = percent.coerceIn(0, 100)
        audioTrack?.setVolume(volumePercent / 100f)
    }

    fun stop() {
        if (!isPlaying.getAndSet(false)) return
        writeJob?.cancel()
        writeJob = null
        audioTrack?.let { track ->
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.stop()
                track.flush()
            }
            track.release()
        }
        audioTrack = null
        abandonAudioFocus()
    }

    private fun requestAudioFocus(): Boolean {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = manager
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { focusChange ->
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                ) {
                    stop()
                }
            }
            .build()
        focusRequest = request
        return manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { manager.abandonAudioFocusRequest(it) }
        }
        focusRequest = null
        audioManager = null
    }
}
