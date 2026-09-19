package com.tidesleep.app.data

enum class PlaybackStartMode {
    IMMEDIATE,
    COUNTDOWN,
    SCHEDULED,
    ;

    companion object {
        fun fromOrdinal(ordinal: Int): PlaybackStartMode =
            entries.getOrElse(ordinal) { IMMEDIATE }
    }
}

data class PlaybackStartPreferences(
    val mode: PlaybackStartMode = PlaybackStartMode.IMMEDIATE,
    val countdownMinutes: Int = DEFAULT_COUNTDOWN_MINUTES,
    val scheduledHour: Int = DEFAULT_SCHEDULED_HOUR,
    val scheduledMinute: Int = DEFAULT_SCHEDULED_MINUTE,
    val armedTriggerAtEpochMs: Long? = null,
) {
    companion object {
        const val DEFAULT_COUNTDOWN_MINUTES = 15
        const val DEFAULT_SCHEDULED_HOUR = 23
        const val DEFAULT_SCHEDULED_MINUTE = 0
        val COUNTDOWN_OPTIONS_MINUTES = listOf(5, 10, 15, 30, 45, 60)
    }
}
