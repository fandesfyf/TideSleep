package com.tidesleep.app.wearable

import android.content.Intent
import android.net.Uri

/**
 * 解析米家自动化 / 深链 / 广播传入的睡眠状态。
 */
object SleepStateParser {

    const val ACTION_SLEEP_STATE = "com.tidesleep.app.ACTION_SLEEP_STATE"
    const val EXTRA_STATE = "state"

    const val DEEP_LINK_SCHEME = "tidesleep"
    const val DEEP_LINK_HOST = "sleep"

    private val STATE_QUERY_REGEX = Regex("[?&]state=([^&#]+)", RegexOption.IGNORE_CASE)

    fun parseIntent(intent: Intent?): WearableSleepState? {
        if (intent == null) return null

        when (intent.action) {
            ACTION_SLEEP_STATE -> return parseStateString(intent.getStringExtra(EXTRA_STATE))
            Intent.ACTION_VIEW -> return parseUri(intent.data)
        }

        return parseStateString(intent.getStringExtra(EXTRA_STATE))
            ?: parseUri(intent.data)
    }

    fun parseUri(uri: Uri?): WearableSleepState? {
        if (uri == null) return null
        if (uri.scheme != DEEP_LINK_SCHEME || uri.host != DEEP_LINK_HOST) return null
        val stateParam = uri.getQueryParameter(EXTRA_STATE)
            ?: STATE_QUERY_REGEX.find(uri.toString())?.groupValues?.getOrNull(1)
        return parseStateString(stateParam)
    }

    fun parseStateString(raw: String?): WearableSleepState? {
        return when (raw?.trim()?.lowercase()) {
            "asleep", "sleep", "sleeping", "睡着", "睡眠中", "入睡" -> WearableSleepState.Asleep
            "awake", "wake", "waking", "清醒", "醒来", "出睡" -> WearableSleepState.Awake
            else -> null
        }
    }

    fun toDeepLink(state: WearableSleepState): String = when (state) {
        WearableSleepState.Asleep -> "tidesleep://sleep?state=asleep"
        WearableSleepState.Awake -> "tidesleep://sleep?state=awake"
        WearableSleepState.Unknown -> "tidesleep://sleep?state=awake"
    }
}
