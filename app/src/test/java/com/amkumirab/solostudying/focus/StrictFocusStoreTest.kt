package com.amkumirab.solostudying.focus

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StrictFocusStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteSharedPreferences(StrictFocusStore.PREFERENCES_NAME)
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(StrictFocusStore.PREFERENCES_NAME)
    }

    @Test
    fun `strict focus request persists until session releases it`() {
        val store = StrictFocusStore(context)
        assertFalse(store.isRequested())

        store.setRequested(true)
        assertTrue(StrictFocusStore(context).isRequested())
        store.setOwnsPinning(true)
        assertTrue(StrictFocusStore(context).ownsPinning())

        store.setRequested(false)
        assertFalse(StrictFocusStore(context).isRequested())
        assertTrue(StrictFocusStore(context).ownsPinning())
        store.setOwnsPinning(false)
        assertFalse(StrictFocusStore(context).ownsPinning())
    }
}
