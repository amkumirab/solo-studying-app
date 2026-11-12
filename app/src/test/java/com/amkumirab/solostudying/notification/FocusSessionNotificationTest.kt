package com.amkumirab.solostudying.notification

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.R
import com.amkumirab.solostudying.focus.FocusSessionSnapshot
import com.amkumirab.solostudying.focus.FocusSessionStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusSessionNotificationTest {

    private lateinit var context: Context
    private lateinit var store: FocusSessionStore
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        store = FocusSessionStore(context)
        store.clear()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        NotificationHelper.createNotificationChannel(context)
        FocusSessionNotifier.createChannel(context)
    }

    @After
    fun tearDown() {
        FocusSessionNotifier.cancelCompletionAlarm(context)
        notificationManager.cancelAll()
        store.clear()
    }

    @Test
    fun `running notification is ongoing and exposes pause and finish controls`() {
        FocusSessionNotifier.show(context, runningSnapshot())

        val notification = shadowOf(notificationManager)
            .getNotification(FocusSessionNotifier.ACTIVE_NOTIFICATION_ID)

        assertNotNull(notification)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals("Electromagnetics", notification.extras.getString(Notification.EXTRA_TITLE))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals(listOf("Pause", "Finish"), notification.actions.map { it.title.toString() })
    }

    @Test
    fun `running notification provides a live countdown in the main content`() {
        FocusSessionNotifier.show(context, runningSnapshot())

        val notification = shadowOf(notificationManager)
            .getNotification(FocusSessionNotifier.ACTIVE_NOTIFICATION_ID)

        val builder = Notification.Builder.recoverBuilder(context, notification)
        for (content in listOf(builder.createContentView(), builder.createBigContentView())) {
            val view = requireNotNull(content).apply(context, FrameLayout(context))
            val clock = view.findViewById<Chronometer>(R.id.focus_notification_countdown)
            assertNotNull("Main content must contain a live countdown", clock)
            assertTrue(clock.isCountDown)
            assertEquals(SystemClock.elapsedRealtime() + 300_000L, clock.base)
            assertEquals(View.VISIBLE, clock.visibility)
        }
        assertEquals("Focus in progress", notification.extras.getString(Notification.EXTRA_TEXT))
    }

    @Test
    @Config(sdk = [24])
    fun `main countdown also works on the minimum supported Android version`() {
        `running notification provides a live countdown in the main content`()
    }

    @Test
    fun `pausing replaces the live clock with a frozen duration and resume rebases it`() {
        FocusSessionNotifier.show(context, runningSnapshot().copy(isPaused = true))
        val paused = shadowOf(notificationManager).getNotification(FocusSessionNotifier.ACTIVE_NOTIFICATION_ID)
        val view = requireNotNull(Notification.Builder.recoverBuilder(context, paused).createContentView())
            .apply(context, FrameLayout(context))

        assertEquals(View.GONE, view.findViewById<Chronometer>(R.id.focus_notification_countdown).visibility)
        val frozen = view.findViewById<TextView>(R.id.focus_notification_paused_time)
        assertEquals(View.VISIBLE, frozen.visibility)
        assertEquals("05:00", frozen.text.toString())

        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMinutes(5))
        assertEquals("05:00", frozen.text.toString())
        FocusSessionNotifier.show(context, runningSnapshot())
        val resumed = shadowOf(notificationManager).getNotification(FocusSessionNotifier.ACTIVE_NOTIFICATION_ID)
        requireNotNull(Notification.Builder.recoverBuilder(context, resumed).createContentView())
            .reapply(context, view)
        assertEquals(View.GONE, view.findViewById<TextView>(R.id.focus_notification_paused_time).visibility)
        val clock = view.findViewById<Chronometer>(R.id.focus_notification_countdown)
        assertEquals(View.VISIBLE, clock.visibility)
        assertEquals(SystemClock.elapsedRealtime() + 300_000L, clock.base)
    }

    @Test
    fun `live countdown supports sessions longer than one hour`() {
        FocusSessionNotifier.show(context, runningSnapshot(timeLeftSeconds = 7_384L))
        val notification = shadowOf(notificationManager).getNotification(FocusSessionNotifier.ACTIVE_NOTIFICATION_ID)
        val view = requireNotNull(Notification.Builder.recoverBuilder(context, notification).createContentView())
            .apply(context, FrameLayout(context))
        assertEquals(SystemClock.elapsedRealtime() + 7_384_000L,
            view.findViewById<Chronometer>(R.id.focus_notification_countdown).base)
    }

    @Test
    fun `paused notification replaces countdown with remaining time and resume control`() {
        FocusSessionNotifier.show(context, runningSnapshot().copy(isPaused = true))

        val notification = shadowOf(notificationManager)
            .getNotification(FocusSessionNotifier.ACTIVE_NOTIFICATION_ID)

        assertNotNull(notification)
        assertFalse(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals(
            "Paused · 05:00 remaining",
            notification.extras.getString(Notification.EXTRA_TEXT),
        )
        assertEquals(listOf("Resume", "Finish"), notification.actions.map { it.title.toString() })
    }

    @Test
    fun `pause action updates persisted timer state`() {
        store.write(runningSnapshot(lastTickTimeMillis = System.currentTimeMillis()))

        FocusSessionActionReceiver().onReceive(
            context,
            Intent(context, FocusSessionActionReceiver::class.java).apply {
                action = FocusSessionActionReceiver.ACTION_PAUSE
            },
        )

        val paused = store.read() ?: error("Session was removed")
        assertTrue(paused.isPaused)
        assertTrue(paused.timeLeftSeconds in 299L..300L)
        assertNotNull(
            shadowOf(notificationManager).getNotification(FocusSessionNotifier.ACTIVE_NOTIFICATION_ID),
        )
    }

    @Test
    fun `elapsed timer replaces active notification with completion alert`() {
        store.write(
            runningSnapshot(
                timeLeftSeconds = 5L,
                lastTickTimeMillis = System.currentTimeMillis() - 10_000L,
            ),
        )

        FocusSessionActionReceiver().onReceive(
            context,
            Intent(context, FocusSessionActionReceiver::class.java).apply {
                action = FocusSessionActionReceiver.ACTION_TIMER_ELAPSED
            },
        )

        assertEquals(0L, store.read()?.timeLeftSeconds)
        assertEquals(
            null,
            shadowOf(notificationManager).getNotification(FocusSessionNotifier.ACTIVE_NOTIFICATION_ID),
        )
        assertNotNull(
            shadowOf(notificationManager).getNotification(FocusSessionNotifier.COMPLETED_NOTIFICATION_ID),
        )
    }

    @Test
    fun `duration formatter supports sessions longer than one hour`() {
        assertEquals("00:00", formatDuration(0L))
        assertEquals("24:59", formatDuration(1_499L))
        assertEquals("2:03:04", formatDuration(7_384L))
    }

    @Test
    fun `focus notifications are skipped when permission is denied`() {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        FocusSessionNotifier.show(context, runningSnapshot())
        FocusSessionNotifier.showCompleted(context, "Electromagnetics")

        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }

    private fun runningSnapshot(
        timeLeftSeconds: Long = 300L,
        lastTickTimeMillis: Long = 1_000_000L,
    ) = FocusSessionSnapshot(
        isActive = true,
        isFreeStudy = false,
        isPaused = false,
        timeLeftSeconds = timeLeftSeconds,
        timeSpentSeconds = 0L,
        initialBossTimeSpentSeconds = 0L,
        lastTickTimeMillis = lastTickTimeMillis,
        bossId = 7,
        skillId = null,
        bossTitle = "Electromagnetics",
    )
}
