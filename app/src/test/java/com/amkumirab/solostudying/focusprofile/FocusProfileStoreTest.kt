package com.amkumirab.solostudying.focusprofile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FocusProfileStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteSharedPreferences(FocusProfileStore.PREFERENCES_NAME)
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(FocusProfileStore.PREFERENCES_NAME)
    }

    @Test
    fun `profile settings survive storage round trip`() {
        val expected = FocusProfile(
            id = "deep-work",
            name = "COMSOL Deep Work",
            type = FocusProfileType.CYCLE,
            focusMinutes = 45,
            breakMinutes = 10,
            rounds = 3,
            skillId = 12,
            useFocusShield = true,
            useStrictFocus = true,
            createdAtMillis = 1_000L,
            lastUsedAtMillis = 2_000L,
        )

        FocusProfileStore(context).write(listOf(expected))

        assertEquals(listOf(expected), FocusProfileStore(context).read())
    }

    @Test
    fun `invalid entries are ignored without losing valid profiles`() {
        val valid = FocusProfile(
            id = "review",
            name = "Quick Review",
            type = FocusProfileType.SINGLE,
            focusMinutes = 15,
            createdAtMillis = 4_000L,
        )
        val store = FocusProfileStore(context)
        store.write(listOf(valid))
        val validJson = context.getSharedPreferences(FocusProfileStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString("profiles", "[]")
            .orEmpty()
            .removeSuffix("]")
        context.getSharedPreferences(FocusProfileStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("profiles", "$validJson,{\"id\":\"broken\"}]")
            .commit()

        assertEquals(listOf(valid), store.read())

        context.getSharedPreferences(FocusProfileStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("profiles", "not-json")
            .commit()
        assertTrue(store.read().isEmpty())
    }
}
