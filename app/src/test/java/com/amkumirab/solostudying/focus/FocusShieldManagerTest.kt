package com.amkumirab.solostudying.focus

import android.app.NotificationManager
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

@RunWith(RobolectricTestRunner::class)
class FocusShieldManagerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteSharedPreferences(FocusShieldManager.PREFERENCES_NAME)
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(FocusShieldManager.PREFERENCES_NAME)
    }

    @Test
    fun `enabled shield activates during focus and restores previous filter on pause`() {
        val controller = FakeController()
        val manager = FocusShieldManager(context, controller)

        manager.setEnabled(enabled = true, sessionActive = true, sessionPaused = false)

        assertTrue(manager.isEnabled())
        assertTrue(manager.isActive())
        assertTrue(manager.ownsCurrentFilter())
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, controller.filter)

        manager.reconcile(sessionActive = true, sessionPaused = true)

        assertFalse(manager.isActive())
        assertFalse(manager.ownsCurrentFilter())
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, controller.filter)
    }

    @Test
    fun `existing do not disturb mode is never overwritten or restored`() {
        val controller = FakeController(filter = NotificationManager.INTERRUPTION_FILTER_NONE)
        val manager = FocusShieldManager(context, controller)

        manager.setEnabled(enabled = true, sessionActive = true, sessionPaused = false)
        manager.reconcile(sessionActive = false, sessionPaused = false)

        assertEquals(NotificationManager.INTERRUPTION_FILTER_NONE, controller.filter)
        assertFalse(manager.ownsCurrentFilter())
        assertEquals(emptyList<Int>(), controller.appliedFilters)
    }

    @Test
    fun `permission can be granted after feature is enabled`() {
        val controller = FakeController(hasAccess = false)
        val manager = FocusShieldManager(context, controller)

        manager.setEnabled(enabled = true, sessionActive = true, sessionPaused = false)
        assertFalse(manager.isActive())

        controller.hasAccess = true
        manager.reconcile(sessionActive = true, sessionPaused = false)

        assertTrue(manager.isActive())
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, controller.filter)
    }

    @Test
    fun `failed activation does not claim ownership of system setting`() {
        val controller = FakeController(allowChanges = false)
        val manager = FocusShieldManager(context, controller)

        manager.setEnabled(enabled = true, sessionActive = true, sessionPaused = false)

        assertFalse(manager.isActive())
        assertFalse(manager.ownsCurrentFilter())
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, controller.filter)
    }

    @Test
    fun `manual system change during focus is preserved`() {
        val controller = FakeController()
        val manager = FocusShieldManager(context, controller)
        manager.setEnabled(enabled = true, sessionActive = true, sessionPaused = false)

        controller.filter = NotificationManager.INTERRUPTION_FILTER_NONE
        manager.reconcile(sessionActive = false, sessionPaused = false)

        assertEquals(NotificationManager.INTERRUPTION_FILTER_NONE, controller.filter)
        assertFalse(manager.ownsCurrentFilter())
    }

    private class FakeController(
        var hasAccess: Boolean = true,
        var filter: Int = NotificationManager.INTERRUPTION_FILTER_ALL,
        var allowChanges: Boolean = true,
    ) : InterruptionFilterController {
        val appliedFilters = mutableListOf<Int>()

        override fun hasPolicyAccess(): Boolean = hasAccess

        override fun currentFilter(): Int = filter

        override fun setFilter(filter: Int): Boolean {
            if (!allowChanges) return false
            appliedFilters += filter
            this.filter = filter
            return true
        }
    }
}
