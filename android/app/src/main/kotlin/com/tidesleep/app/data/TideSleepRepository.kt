package com.tidesleep.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tidesleep.app.data.NightSummaryDto
import com.tidesleep.app.data.toDto
import com.tidesleep.app.data.toNightSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tidesleep_prefs")

data class UserPreferences(
    val safetyConfig: SafetyConfig = SafetyConfig.demo(),
    val monitorSource: SleepMonitorSource = SleepMonitorSource.FAKE,
    val disclaimerAccepted: Boolean = false,
    val nightHistory: List<NightSummary> = emptyList(),
    val playbackStart: PlaybackStartPreferences = PlaybackStartPreferences(),
)

class TideSleepRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            safetyConfig = readSafetyConfig(prefs),
            monitorSource = SleepMonitorSource.entries.getOrElse(
                prefs[Keys.MONITOR_SOURCE] ?: 0
            ) { SleepMonitorSource.FAKE },
            disclaimerAccepted = prefs[Keys.DISCLAIMER_ACCEPTED] ?: false,
            nightHistory = readNightHistory(prefs),
            playbackStart = readPlaybackStart(prefs),
        )
    }

    suspend fun loadOnce(): UserPreferences = preferences.first()

    suspend fun saveSafetyConfig(config: SafetyConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAFETY_PRESET] = config.preset.name
            prefs[Keys.POST_SLEEP_DELAY_MS] = config.postSleepDelay.inWholeMilliseconds
            prefs[Keys.MAX_SESSION_MS] = config.maxSessionDuration.inWholeMilliseconds
            prefs[Keys.VOLUME_PERCENT] = config.volumePercent
            prefs[Keys.PULSE_INTERVAL_MEAN] = config.pulseIntervalMeanSeconds
            prefs[Keys.PULSE_INTERVAL_JITTER] = config.pulseIntervalJitterSeconds
        }
    }

    suspend fun saveMonitorSource(source: SleepMonitorSource) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MONITOR_SOURCE] = source.ordinal
        }
    }

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISCLAIMER_ACCEPTED] = accepted
        }
    }

    suspend fun appendNightSummary(summary: NightSummary) {
        context.dataStore.edit { prefs ->
            val current = readNightHistory(prefs)
            val updated = (listOf(summary) + current).take(30)
            prefs[Keys.NIGHT_HISTORY] = json.encodeToString(updated.map { it.toDto() })
        }
    }

    suspend fun savePlaybackStartPreferences(prefs: PlaybackStartPreferences) {
        context.dataStore.edit { store ->
            store[Keys.PLAYBACK_START_MODE] = prefs.mode.ordinal
            store[Keys.COUNTDOWN_MINUTES] = prefs.countdownMinutes
            store[Keys.SCHEDULED_HOUR] = prefs.scheduledHour
            store[Keys.SCHEDULED_MINUTE] = prefs.scheduledMinute
            if (prefs.armedTriggerAtEpochMs != null) {
                store[Keys.ARMED_TRIGGER_AT_MS] = prefs.armedTriggerAtEpochMs
            } else {
                store.remove(Keys.ARMED_TRIGGER_AT_MS)
            }
        }
    }

    private fun readPlaybackStart(prefs: Preferences): PlaybackStartPreferences {
        return PlaybackStartPreferences(
            mode = PlaybackStartMode.fromOrdinal(prefs[Keys.PLAYBACK_START_MODE] ?: 0),
            countdownMinutes = prefs[Keys.COUNTDOWN_MINUTES]
                ?: PlaybackStartPreferences.DEFAULT_COUNTDOWN_MINUTES,
            scheduledHour = prefs[Keys.SCHEDULED_HOUR]
                ?: PlaybackStartPreferences.DEFAULT_SCHEDULED_HOUR,
            scheduledMinute = prefs[Keys.SCHEDULED_MINUTE]
                ?: PlaybackStartPreferences.DEFAULT_SCHEDULED_MINUTE,
            armedTriggerAtEpochMs = prefs[Keys.ARMED_TRIGGER_AT_MS],
        )
    }

    private fun readSafetyConfig(prefs: Preferences): SafetyConfig {
        val presetName = prefs[Keys.SAFETY_PRESET]
        val preset = presetName?.let { runCatching { SafetyPreset.valueOf(it) }.getOrNull() }
            ?: SafetyPreset.DEMO

        val delayMs = prefs[Keys.POST_SLEEP_DELAY_MS]
        val maxMs = prefs[Keys.MAX_SESSION_MS]
        val volume = prefs[Keys.VOLUME_PERCENT]

        if (delayMs == null && preset == SafetyPreset.DEMO) return SafetyConfig.demo()
        if (delayMs == null && preset == SafetyPreset.SCIENTIFIC) return SafetyConfig.scientific()

        return SafetyConfig(
            preset = preset,
            postSleepDelay = (delayMs ?: SafetyConfig.demo().postSleepDelay.inWholeMilliseconds).milliseconds,
            maxSessionDuration = (maxMs ?: SafetyConfig.demo().maxSessionDuration.inWholeMilliseconds).milliseconds,
            volumePercent = volume ?: SafetyConfig.demo().volumePercent,
            pulseIntervalMeanSeconds = prefs[Keys.PULSE_INTERVAL_MEAN] ?: 4,
            pulseIntervalJitterSeconds = prefs[Keys.PULSE_INTERVAL_JITTER] ?: 2,
        )
    }

    private fun readNightHistory(prefs: Preferences): List<NightSummary> {
        val raw = prefs[Keys.NIGHT_HISTORY] ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<NightSummaryDto>>(raw).map { it.toNightSummary() }
        }.getOrDefault(emptyList())
    }

    private object Keys {
        val SAFETY_PRESET = stringPreferencesKey("safety_preset")
        val POST_SLEEP_DELAY_MS = longPreferencesKey("post_sleep_delay_ms")
        val MAX_SESSION_MS = longPreferencesKey("max_session_ms")
        val VOLUME_PERCENT = intPreferencesKey("volume_percent")
        val PULSE_INTERVAL_MEAN = intPreferencesKey("pulse_interval_mean")
        val PULSE_INTERVAL_JITTER = intPreferencesKey("pulse_interval_jitter")
        val MONITOR_SOURCE = intPreferencesKey("monitor_source")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val NIGHT_HISTORY = stringPreferencesKey("night_history")
        val PLAYBACK_START_MODE = intPreferencesKey("playback_start_mode")
        val COUNTDOWN_MINUTES = intPreferencesKey("countdown_minutes")
        val SCHEDULED_HOUR = intPreferencesKey("scheduled_hour")
        val SCHEDULED_MINUTE = intPreferencesKey("scheduled_minute")
        val ARMED_TRIGGER_AT_MS = longPreferencesKey("armed_trigger_at_ms")
    }
}
