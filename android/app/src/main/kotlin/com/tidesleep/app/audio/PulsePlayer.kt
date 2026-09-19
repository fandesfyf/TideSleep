package com.tidesleep.app.audio

interface PulsePlayer {
    fun playPulse()
    fun release()
}

class NoOpPulsePlayer : PulsePlayer {
    override fun playPulse() = Unit
    override fun release() = Unit
}
