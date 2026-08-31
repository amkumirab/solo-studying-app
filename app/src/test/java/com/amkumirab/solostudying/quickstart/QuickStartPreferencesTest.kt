package com.amkumirab.solostudying.quickstart

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuickStartPreferencesTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteSharedPreferences(QuickStartPreferences.PREFERENCES_NAME)
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(QuickStartPreferences.PREFERENCES_NAME)
    }

    @Test
    fun `default selection uses the pomodoro preset`() {
        assertEquals(
            QuickStartSelection(durationMinutes = 25, skillId = null),
            QuickStartPreferences(context).read(),
        )
    }

    @Test
    fun `last duration and skill survive store recreation`() {
        QuickStartPreferences(context).save(
            QuickStartSelection(durationMinutes = 45, skillId = 7),
        )

        assertEquals(
            QuickStartSelection(durationMinutes = 45, skillId = 7),
            QuickStartPreferences(context).read(),
        )
    }

    @Test
    fun `invalid values are normalized before use`() {
        val store = QuickStartPreferences(context)

        store.save(QuickStartSelection(durationMinutes = 900, skillId = -3))
        val restored = store.read()

        assertEquals(QuickStartPreferences.MAX_DURATION_MINUTES, restored.durationMinutes)
        assertNull(restored.skillId)
    }
}
