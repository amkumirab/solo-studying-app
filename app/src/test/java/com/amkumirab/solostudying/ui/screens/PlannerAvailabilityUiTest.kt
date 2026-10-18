package com.amkumirab.solostudying.ui.screens

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.planner.StudyPlannerStore
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import com.amkumirab.solostudying.ui.viewmodel.StudyPlannerViewModel
import java.time.LocalDate
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlannerAvailabilityUiTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val today = LocalDate.of(2026, 10, 18)
    private val goal = BossEntity(id = 1, name = "Physics", difficulty = "Medium", requiredMinutes = 6000)
    private val store get() = StudyPlannerStore(app)
    private val week get() = defaultWeeklyAvailability(List(7) { 60 })
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    private fun open(): StudyPlannerViewModel {
        val model = StudyPlannerViewModel(app)
        rule.setContent { SoloStudyingTheme {
            StudyPlannerEntry(listOf(goal), today, List(7) { 60 }, false, { _, _ -> },
                plannerViewModel = model, showDailySessions = false)
        } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText("Availability").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Availability", useUnmergedTree = true).performScrollTo().performClick()
        return model
    }

    @Test fun `multiple windows and rest days save from the real planner entry`() {
        open()
        rule.onNodeWithTag("edit_availability").performClick()
        rule.onNodeWithTag("edit_availability_day_0").performScrollTo().performClick()
        rule.onNodeWithTag("window_start_0").performTextReplacement("09:00")
        rule.onNodeWithTag("window_end_0").performTextReplacement("10:00")
        rule.onNodeWithTag("add_availability_window").performScrollTo().performClick()
        rule.onNodeWithTag("window_start_1").performScrollTo().performTextReplacement("17:00")
        rule.onNodeWithTag("window_end_1").performScrollTo().performTextReplacement("18:00")
        rule.onNodeWithTag("save_availability_day").performScrollTo().performClick()
        rule.onNodeWithTag("availability_enabled_6").performScrollTo().performClick()
        rule.onNodeWithTag("availability_total").performScrollTo().assertTextEquals("Weekly capacity: 7 h")
        rule.onNodeWithTag("save_availability").performScrollTo().performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.readData().availability != null }
        val saved = store.readData().availability!!
        assertEquals(listOf(StudyTimeWindow(540, 600), StudyTimeWindow(1020, 1080)), saved.days[0].windows)
        assertFalse(saved.days[6].enabled)
        assertEquals(420, saved.totalMinutes)
        val reopened = StudyPlannerViewModel(app)
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); reopened.state.value.loaded }
        assertEquals(saved, reopened.state.value.availability)
    }

    @Test fun `overlap blocks saving and raw day drafts survive restoration`() {
        var saved: WeeklyAvailability? = null
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme {
            PlannerAvailabilityScreen(PlannerSnapshot(availability = week), List(7) { 60 }, false, null, false,
                onSave = { value, _, done -> saved = value; done() }, onReset = {})
        } }
        rule.onNodeWithTag("edit_availability").performClick()
        rule.onNodeWithTag("edit_availability_day_0").performScrollTo().performClick()
        rule.onNodeWithTag("add_availability_window").performScrollTo().performClick()
        rule.onNodeWithTag("window_start_1").performScrollTo().performTextReplacement("17:30")
        rule.onNodeWithTag("save_availability_day").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Study windows must not overlap.").performScrollTo().assertExists()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("window_start_1").performScrollTo().assertTextContains("17:30")
        rule.onNodeWithTag("save_availability_day").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("window_start_1").performScrollTo().performTextReplacement("18:00")
        rule.onNodeWithTag("save_availability_day").performScrollTo().performClick()
        rule.onNodeWithTag("save_availability").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(480, saved!!.totalMinutes) }
    }

    @Test fun `slider uses half hour increments and cancel leaves settings unchanged`() {
        var saves = 0
        rule.setContent { SoloStudyingTheme {
            PlannerAvailabilityScreen(PlannerSnapshot(availability = week), List(7) { 60 }, false, null, false,
                onSave = { _, _, _ -> saves++ }, onReset = {})
        } }
        rule.onNodeWithTag("edit_availability").performClick()
        rule.onNodeWithTag("edit_availability_day_0").performScrollTo().performClick()
        rule.onNodeWithTag("window_duration_0").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(90f) }
        rule.onNodeWithTag("window_end_0").performScrollTo().assertTextContains("18:30")
        rule.onNodeWithTag("availability_day_total").performScrollTo().assertTextEquals("Daily capacity: 1.5 h")
        rule.onNodeWithTag("availability_total").performScrollTo().assertTextEquals("Weekly capacity: 7.5 h")
        rule.onNodeWithTag("save_availability_day").performScrollTo().performClick()
        rule.onNodeWithTag("cancel_availability").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(0, saves) }
        rule.onNodeWithTag("edit_availability").performClick()
        rule.onNodeWithTag("availability_total").assertTextEquals("Weekly capacity: 7 h")
    }

    @Test fun `all rest week saves but busy controls cannot change it`() {
        var busy by mutableStateOf(false)
        var saved: WeeklyAvailability? = null
        rule.setContent { SoloStudyingTheme {
            PlannerAvailabilityScreen(PlannerSnapshot(availability = week), List(7) { 60 }, busy, null, false,
                onSave = { value, _, done -> saved = value; done() }, onReset = {})
        } }
        rule.onNodeWithTag("edit_availability").performClick()
        rule.runOnIdle { busy = true }
        rule.onNodeWithTag("availability_enabled_0").assertIsNotEnabled()
        rule.onNodeWithTag("save_availability").performScrollTo().assertIsNotEnabled()
        rule.runOnIdle { busy = false }
        (0..6).forEach { rule.onNodeWithTag("availability_enabled_$it").performScrollTo().performClick() }
        rule.onNodeWithTag("save_availability").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(0, saved!!.totalMinutes) }
    }

    @Test fun `restored draft cannot silently rebase onto changed settings`() {
        var snapshot by mutableStateOf(PlannerSnapshot(availability = week))
        var saves = 0
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme {
            PlannerAvailabilityScreen(snapshot, List(7) { 60 }, false, null, false,
                onSave = { _, _, _ -> saves++ }, onReset = {})
        } }
        rule.onNodeWithTag("edit_availability").performClick()
        rule.runOnIdle { snapshot = snapshot.copy(availability = defaultWeeklyAvailability(List(7) { 120 })) }
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("save_availability").performScrollTo().performClick()
        rule.onNodeWithText("Saved settings changed while this draft was closed. Cancel and reopen the editor before saving.")
            .performScrollTo().assertExists()
        rule.runOnIdle { assertEquals(0, saves) }
    }

    @Test fun `real entry restores a draft while a fresh view model loads saved data`() {
        assertTrue(store.writeSnapshot(PlannerSnapshot(), PlannerSnapshot(availability = week)))
        var model: StudyPlannerViewModel? = null
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme {
            val current = remember { StudyPlannerViewModel(app) }
            model = current
            StudyPlannerEntry(listOf(goal), today, List(7) { 60 }, false, { _, _ -> },
                plannerViewModel = current, showDailySessions = false)
        } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model?.state?.value?.loaded == true }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.onNodeWithText("Availability", useUnmergedTree = true).performScrollTo().performClick()
        rule.onNodeWithTag("edit_availability").performClick()
        rule.onNodeWithTag("availability_enabled_6").performScrollTo().performClick()
        val previous = model
        restoration.emulateSavedInstanceStateRestore()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model !== previous && model?.state?.value?.loaded == true }
        rule.onNodeWithTag("save_availability").performScrollTo().performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.readData().availability?.days?.get(6)?.enabled == false }
        assertEquals(360, store.readData().availability!!.totalMinutes)
    }

    @Test fun `removing the final window disables the day and midnight end is valid`() {
        var saved: WeeklyAvailability? = null
        rule.setContent { SoloStudyingTheme {
            WeeklyAvailabilityEditor(week, false, false, null, { saved = it }, {})
        } }
        rule.onNodeWithTag("edit_availability_day_0").performScrollTo().performClick()
        rule.onNodeWithTag("remove_availability_window_0").performScrollTo().performClick()
        rule.onNodeWithTag("availability_day_total").performScrollTo().assertTextEquals("Daily capacity: 0 h")
        rule.onNodeWithTag("add_availability_window").performScrollTo().performClick()
        rule.onNodeWithTag("window_start_0").performScrollTo().performTextReplacement("24:00")
        rule.onNodeWithTag("save_availability_day").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("window_start_0").performScrollTo().performTextReplacement("23:30")
        rule.onNodeWithTag("window_end_0").performScrollTo().performTextReplacement("24:00")
        rule.onNodeWithTag("save_availability_day").performScrollTo().performClick()
        rule.onNodeWithTag("save_availability").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(StudyDayAvailability(true, listOf(StudyTimeWindow(1410, 1440))), saved!!.days[0]) }
    }

    @Test fun `confirmed rebuild uses saved capacity but cancel leaves accepted sessions unchanged`() {
        val original = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 6000, today.plusDays(60), 3, 0)), List(7) { 480 }, 25))
        assertTrue(store.writeSnapshot(PlannerSnapshot(), PlannerSnapshot(plan = original, availability = defaultWeeklyAvailability(List(7) { 120 }))))
        open()
        rule.onNodeWithText("Schedule", useUnmergedTree = true).performScrollTo().performClick()
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(original, store.readData().plan)
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Confirm").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.readData().plan?.config?.weekdayMinutes == List(7) { 120 } }
        assertEquals(6000, store.readData().plan!!.config.courses.single().remainingMinutes)
    }

    @Test fun `all rest rebuild preserves the accepted plan`() {
        val original = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 6000, today.plusDays(60), 3, 0)), List(7) { 480 }, 25))
        val rest = week.copy(days = week.days.map { it.copy(enabled = false) })
        assertTrue(store.writeSnapshot(PlannerSnapshot(), PlannerSnapshot(plan = original, availability = rest)))
        open()
        rule.onNodeWithText("Schedule", useUnmergedTree = true).performScrollTo().performClick()
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Confirm").performClick()
        rule.onNodeWithText("Enable at least one study day in Availability before rebuilding. Your current plan is unchanged.").assertExists()
        assertEquals(original, store.readData().plan)
    }

    @Test fun `unreadable availability reset keeps the accepted plan and subjects`() {
        val plan = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 60, today.plusDays(6), 3, 0)), List(7) { 60 }, 25))
        val before = PlannerSnapshot(plan, listOf(PlannerSubject(1, "Physics")))
        assertTrue(store.writeSnapshot(store.readData().snapshot, before))
        val prefs = app.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("availability", "future-record").commit()
        open()
        rule.onNodeWithText("Reset saved availability").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals("future-record", prefs.getString("availability", null))
        rule.onNodeWithText("Reset saved availability").performClick()
        rule.onNodeWithText("Reset availability").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.readData().availabilityError == null }
        assertEquals(before, store.readData().snapshot)
    }

    @Test fun `retrying a stale availability save cannot overwrite new windows`() {
        val model = StudyPlannerViewModel(app)
        rule.setContent { }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        val before = model.state.value.snapshot
        assertTrue(store.writeSnapshot(before, before.copy(availability = week)))
        var saves = 0
        repeat(2) {
            rule.runOnIdle { model.saveAvailability(week.copy(days = week.days.map { it.copy(enabled = false) }), before) { saves++ } }
            rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); !model.state.value.busy }
        }
        assertEquals(0, saves)
        assertEquals(week, store.readData().availability)
    }
}
