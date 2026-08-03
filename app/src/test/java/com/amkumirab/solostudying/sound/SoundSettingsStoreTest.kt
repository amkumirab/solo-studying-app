package com.amkumirab.solostudying.sound

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SoundSettingsStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteSharedPreferences(SoundSettingsStore.PREFERENCES_NAME)
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(SoundSettingsStore.PREFERENCES_NAME)
    }

    @Test
    fun `sound is enabled at full volume by default`() {
        val settings = SoundSettingsStore(context).read()

        assertTrue(settings.enabled)
        assertEquals(1f, settings.volume, 0.001f)
    }

    @Test
    fun `sound settings survive store recreation`() {
        SoundSettingsStore(context).write(
            SoundSettings(enabled = false, volume = 0.4f),
        )

        val restoredSettings = SoundSettingsStore(context).read()

        assertFalse(restoredSettings.enabled)
        assertEquals(0.4f, restoredSettings.volume, 0.001f)
    }

    @Test
    fun `invalid volume values are normalized`() {
        val store = SoundSettingsStore(context)

        store.write(SoundSettings(volume = 2f))
        assertEquals(1f, store.read().volume, 0.001f)

        store.write(SoundSettings(volume = -1f))
        assertEquals(0f, store.read().volume, 0.001f)

        store.write(SoundSettings(volume = Float.NaN))
        assertEquals(1f, store.read().volume, 0.001f)
    }
}
