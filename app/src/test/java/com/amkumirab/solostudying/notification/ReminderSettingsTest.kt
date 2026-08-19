package com.amkumirab.solostudying.notification

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReminderSettingsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(ReminderSettingsStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        ShadowAlarmManager.reset()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(ReminderSettingsStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        ShadowAlarmManager.reset()
    }

    @Test
    fun `default reminder schedule matches the original app behavior`() {
        val settings = ReminderSettingsStore(context).read()

        assertEquals(ReminderSchedule(true, 9, 0), settings.morning)
        assertEquals(ReminderSchedule(true, 18, 0), settings.beforeStudy)
        assertEquals(ReminderSchedule(true, 21, 0), settings.evening)
    }

    @Test
    fun `saved reminder settings survive store recreation`() {
        val expected = ReminderSettings(
            morning = ReminderSchedule(false, 8, 15),
            beforeStudy = ReminderSchedule(true, 16, 45),
            evening = ReminderSchedule(true, 22, 30),
        )
        val store = ReminderSettingsStore(context)

        store.save(expected)

        assertEquals(expected, store.settings.value)
        assertEquals(expected, ReminderSettingsStore(context).read())
    }

    @Test
    fun `next reminder uses today when the selected time is still ahead`() {
        val timeZone = TimeZone.getTimeZone("UTC")
        val now = utcMillis(2026, Calendar.AUGUST, 19, 8, 30)

        val trigger = calculateNextReminderTimeMillis(
            schedule = ReminderSchedule(true, 9, 0),
            nowMillis = now,
            timeZone = timeZone,
        )

        assertEquals(utcMillis(2026, Calendar.AUGUST, 19, 9, 0), trigger)
    }

    @Test
    fun `next reminder moves to tomorrow when the selected time has passed`() {
        val timeZone = TimeZone.getTimeZone("UTC")
        val now = utcMillis(2026, Calendar.AUGUST, 19, 21, 0)

        val trigger = calculateNextReminderTimeMillis(
            schedule = ReminderSchedule(true, 21, 0),
            nowMillis = now,
            timeZone = timeZone,
        )

        assertEquals(utcMillis(2026, Calendar.AUGUST, 20, 21, 0), trigger)
    }

    @Test
    fun `scheduler creates enabled alarms and cancels disabled alarms`() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        val partiallyEnabled = ReminderSettings(
            morning = ReminderSchedule(true, 8, 0),
            beforeStudy = ReminderSchedule(false, 17, 30),
            evening = ReminderSchedule(true, 22, 0),
        )

        NotificationHelper.scheduleDailyAlarms(context, partiallyEnabled)

        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
        assertTrue(shadowAlarmManager.scheduledAlarms.all { it.getIntervalMs() == 0L })

        NotificationHelper.scheduleDailyAlarms(
            context,
            ReminderSettings(
                morning = partiallyEnabled.morning.copy(enabled = false),
                beforeStudy = partiallyEnabled.beforeStudy,
                evening = partiallyEnabled.evening.copy(enabled = false),
            ),
        )

        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun `boot receiver restores alarms from saved settings`() {
        val saved = ReminderSettings(
            morning = ReminderSchedule(false, 8, 0),
            beforeStudy = ReminderSchedule(true, 17, 0),
            evening = ReminderSchedule(false, 22, 0),
        )
        ReminderSettingsStore(context).save(saved)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        ReminderRescheduleReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        val alarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, alarms.size)
        assertFalse(alarms.single().getTriggerAtMs() <= System.currentTimeMillis())
    }

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month, day, hour, minute, 0)
    }.timeInMillis
}
