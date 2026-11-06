package com.amkumirab.solostudying.ui.screens

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.planner.StudyPlannerStore
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerQaStorageTest {
    @Test fun fixtureStorageNeverWritesTheNormalPlannerNamespace() {
        val production = ApplicationProvider.getApplicationContext<Application>()
        val normalPreferences = production.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, 0)
        val original = normalPreferences.all.toMap()
        val qa = PlannerQaApplication(production)
        assertSame(qa, qa.applicationContext)
        val date = LocalDate.of(2026, 11, 2)
        val plan = buildStudyPlan(PlannerConfig(date,
            listOf(PlannerCourse(1, 60, date.plusDays(6), 3, 0)), List(7) { 60 }, 30))
        try {
            qa.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME)
            assertTrue(StudyPlannerStore(qa).write(plan))
            assertEquals(plan, StudyPlannerStore(qa).readData().plan)
            assertEquals(original, normalPreferences.all)
        } finally {
            qa.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME)
        }
        assertEquals(original, normalPreferences.all)
    }
}
