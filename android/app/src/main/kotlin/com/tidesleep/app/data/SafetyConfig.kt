package com.tidesleep.app.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 安全与会话默认参数（开放环 MVP）。
 * 对齐 docs/调研与实施方案.md 附录 B。
 */
data class SafetyConfig(
    val postSleepDelay: Duration = 15.minutes,
    val maxSessionDuration: Duration = 90.minutes,
    val pulseDurationMs: Int = 50,
    val pulseIntervalMeanSeconds: Int = 4,
    val pulseIntervalJitterSeconds: Int = 2,
    val volumePercent: Int = 25,
) {
    val pulseIntervalRange: ClosedFloatingPointRange<Double>
        get() {
            val min = (pulseIntervalMeanSeconds - pulseIntervalJitterSeconds).coerceAtLeast(1)
            val max = pulseIntervalMeanSeconds + pulseIntervalJitterSeconds
            return min.toDouble()..max.toDouble()
        }
}

enum class SleepMonitorSource {
    FAKE,
    XIAOMI_WEAR,
}
