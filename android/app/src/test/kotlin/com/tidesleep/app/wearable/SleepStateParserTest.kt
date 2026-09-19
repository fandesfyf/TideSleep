package com.tidesleep.app.wearable

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class SleepStateParserTest {

    @Test
    fun parseDeepLink_asleep() {
        val uri = Uri.parse("tidesleep://sleep?state=asleep")
        assertEquals(WearableSleepState.Asleep, SleepStateParser.parseUri(uri))
    }

    @Test
    fun parseDeepLink_awake() {
        val uri = Uri.parse("tidesleep://sleep?state=awake")
        assertEquals(WearableSleepState.Awake, SleepStateParser.parseUri(uri))
    }

    @Test
    fun parseDeepLink_invalidHost_returnsNull() {
        val uri = Uri.parse("tidesleep://other?state=asleep")
        assertNull(SleepStateParser.parseUri(uri))
    }

    @Test
    fun parseBroadcastIntent() {
        val intent = Intent(SleepStateParser.ACTION_SLEEP_STATE).apply {
            putExtra(SleepStateParser.EXTRA_STATE, "asleep")
        }
        assertEquals(WearableSleepState.Asleep, SleepStateParser.parseIntent(intent))
    }

    @Test
    fun parseStateString_chineseAliases() {
        assertEquals(WearableSleepState.Asleep, SleepStateParser.parseStateString("睡着"))
        assertEquals(WearableSleepState.Awake, SleepStateParser.parseStateString("出睡"))
    }

    @Test
    fun parseStateString_unknown_returnsNull() {
        assertNull(SleepStateParser.parseStateString("invalid"))
    }

    @Test
    fun toDeepLink_roundTrip() {
        assertEquals(
            "tidesleep://sleep?state=asleep",
            SleepStateParser.toDeepLink(WearableSleepState.Asleep),
        )
    }
}
