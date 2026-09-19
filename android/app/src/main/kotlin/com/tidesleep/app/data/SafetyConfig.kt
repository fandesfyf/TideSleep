package com.tidesleep.app.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class SafetyPreset {
    DEMO,
    SCIENTIFIC,
    CUSTOM,
}

/**
 * 安全与会话参数（开放环 MVP）。
 */
data class SafetyConfig(
    val preset: SafetyPreset = SafetyPreset.DEMO,
    val postSleepDelay: Duration = 30.seconds,
    val maxSessionDuration: Duration = 10.minutes,
    val pulseDurationMs: Int = 50,
    val pulseIntervalMeanSeconds: Int = 4,
    val pulseIntervalJitterSeconds: Int = 2,
    val volumePercent: Int = 20,
) {
    val pulseIntervalRange: ClosedFloatingPointRange<Double>
        get() {
            val min = (pulseIntervalMeanSeconds - pulseIntervalJitterSeconds).coerceAtLeast(1)
            val max = pulseIntervalMeanSeconds + pulseIntervalJitterSeconds
            return min.toDouble()..max.toDouble()
        }

    fun formatPostSleepDelay(): String {
        return if (postSleepDelay < 2.minutes) {
            "${postSleepDelay.inWholeSeconds} 秒"
        } else {
            "${postSleepDelay.inWholeMinutes} 分钟"
        }
    }

    fun formatMaxSession(): String {
        return if (maxSessionDuration < 2.minutes) {
            "${maxSessionDuration.inWholeSeconds} 秒"
        } else {
            "${maxSessionDuration.inWholeMinutes} 分钟"
        }
    }

    companion object {
        fun demo(): SafetyConfig = SafetyConfig(
            preset = SafetyPreset.DEMO,
            postSleepDelay = 30.seconds,
            maxSessionDuration = 10.minutes,
            volumePercent = 20,
        )

        fun scientific(): SafetyConfig = SafetyConfig(
            preset = SafetyPreset.SCIENTIFIC,
            postSleepDelay = 15.minutes,
            maxSessionDuration = 90.minutes,
            volumePercent = 25,
        )
    }
}

enum class SleepMonitorSource {
    FAKE,
    XIAOMI_WEAR,
}
