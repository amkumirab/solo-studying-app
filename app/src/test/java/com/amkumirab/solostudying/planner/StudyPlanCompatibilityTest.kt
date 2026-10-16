package com.amkumirab.solostudying.planner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StudyPlanCompatibilityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences get() = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val legacy get() = checkNotNull(javaClass.getResource("/planner/study-plan-v1.json")).readText()
    private val start = LocalDate.of(2026, 10, 5)
    private val expected = StudyPlan(
        PlannerConfig(start, listOf(
            PlannerCourse(7, 80, start.plusDays(4), 3, 120),
            PlannerCourse(12, 70, start.plusDays(6), 1, 5_000_000_000),
        ), listOf(60, 0, 45, 30, 60, 0, 30), 25),
        listOf(
            StudyBlock(19, 7, start, 25),
            StudyBlock(4, 7, start.plusDays(2), 25),
            StudyBlock(31, 12, start.plusDays(2), 20),
            StudyBlock(50, 12, start.plusDays(6), 15),
        ),
    )

    @Before fun reset() { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    @Test fun `legacy fixture preserves all domain data without rebuilding blocks`() {
        assertEquals(expected, decodeStudyPlan(legacy))
        assertEquals(mapOf(7 to 30, 12 to 35), decodeStudyPlan(legacy).shortfalls)
    }

    @Test fun `opening legacy storage twice never rewrites it`() {
        preferences.edit().putString("plan", legacy).commit()
        repeat(2) {
            assertEquals(expected, StudyPlannerStore(context).read().plan)
            assertEquals(legacy, preferences.getString("plan", null))
        }
    }

    @Test fun `explicit save upgrades legacy storage to version two`() {
        preferences.edit().putString("plan", legacy).commit()
        val store = StudyPlannerStore(context)
        assertTrue(store.write(checkNotNull(store.read().plan)))
        val saved = JSONObject(preferences.getString("plan", null)!!)
        assertEquals(2, saved.getInt("version"))
        assertEquals("date_only", saved.getString("kind"))
        assertEquals(expected, StudyPlannerStore(context).read().plan)
    }

    @Test fun `new saves use a deterministic version two envelope`() {
        val saved = encodeStudyPlan(expected)
        val json = JSONObject(saved)
        assertEquals(setOf("version", "kind", "plan"), json.keys().asSequence().toSet())
        assertEquals(2, json.getInt("version"))
        assertEquals("date_only", json.getString("kind"))
        assertEquals(expected, decodeStudyPlan(saved))
        assertEquals(saved, encodeStudyPlan(decodeStudyPlan(saved)))
    }

    @Test fun `literal version two envelope is readable`() {
        assertEquals(expected, decodeStudyPlan(versionTwo().toString()))
    }

    @Test fun `migration preserves fractional study credit and original id order`() {
        val bosses = listOf(
            BossEntity(id = 7, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 6000,
                timeSpentSeconds = 120 + 25 * 60 + 15),
            BossEntity(id = 12, name = "Physics", difficulty = "Medium", requiredMinutes = 6000,
                timeSpentSeconds = 5_000_000_075),
        )
        val migrated = decodeStudyPlan(encodeStudyPlan(decodeStudyPlan(legacy)))
        val progress = blockProgress(migrated, bosses)
        assertEquals(listOf(4, 19, 31, 50), progress.map { it.block.id })
        assertEquals(listOf(1500L, 15L, 75L, 0L), progress.map { it.creditedSeconds })
        assertEquals(blockProgress(expected, bosses), progress)
        assertEquals(remainingPlannerCourses(expected, bosses), remainingPlannerCourses(migrated, bosses))
        assertEquals(listOf(55, 69), remainingPlannerCourses(migrated, bosses).map { it.remainingMinutes })
    }

    @Test fun `empty schedules retain their explicit shortfalls`() {
        val empty = expected.copy(blocks = emptyList())
        assertEquals(empty, decodeStudyPlan(encodeStudyPlan(empty)))
        assertEquals(mapOf(7 to 80, 12 to 70), decodeStudyPlan(encodeStudyPlan(empty)).shortfalls)
    }

    @Test fun `largest supported block list remains readable within the record size limit`() {
        val large = StudyPlan(
            PlannerConfig(start, listOf(PlannerCourse(1, 18000, start.plusDays(37), 2, 0)), List(7) { 480 }, 15),
            List(18000) { index -> StudyBlock(index + 1, 1, start.plusDays(index / 480L), 1) },
        )
        val encoded = encodeStudyPlan(large)
        assertTrue(encoded.length <= 2_000_000)
        assertEquals(large, decodeStudyPlan(encoded))
        val oversizedList = JSONObject(encoded).apply {
            getJSONObject("plan").getJSONArray("blocks").put(JSONObject()
                .put("id", 18001).put("bossId", 1).put("date", start.toString()).put("minutes", 1))
        }
        assertRejectedAndPreserved(oversizedList.toString())
    }

    @Test fun `both formats enforce the existing scheduling invariants`() {
        val mutations: List<(JSONObject) -> Unit> = listOf(
            { it.getJSONArray("capacity").put(0, 481) },
            { it.getJSONArray("capacity").put(0, "60") },
            { it.getJSONArray("courses").getJSONObject(0).put("priority", 5) },
            { it.getJSONArray("courses").getJSONObject(0).put("baselineSeconds", -1) },
            { it.getJSONArray("courses").getJSONObject(0).put("remainingMinutes", 60001) },
            { it.getJSONArray("blocks").getJSONObject(0).put("id", 4) },
            { it.getJSONArray("blocks").getJSONObject(0).put("minutes", 26) },
            { it.getJSONArray("blocks").getJSONObject(0).put("date", start.plusDays(1).toString()) },
            { it.getJSONArray("blocks").getJSONObject(0).put("bossId", 999) },
        )
        mutations.forEach { mutate ->
            assertRejectedAndPreserved(JSONObject(legacy).apply(mutate).toString())
            assertRejectedAndPreserved(versionTwo().apply { mutate(getJSONObject("plan")) }.toString())
        }
    }

    @Test fun `future and malformed records cannot be overwritten by a save`() {
        listOf("not-json", "{\"version\":99}", "x".repeat(2_000_001),
            versionTwo().put("kind", "timed").toString(),
            versionTwo().put("futureMetadata", true).toString(),
        ).forEach { original ->
            preferences.edit().putString("plan", original).commit()
            val store = StudyPlannerStore(context)
            assertNotNull(store.read().error)
            assertFalse(store.write(expected))
            assertEquals(original, preferences.getString("plan", null))
        }
    }

    @Test fun `an already open editor cannot replace a record that became unsupported`() {
        val editorStore = StudyPlannerStore(context)
        assertTrue(editorStore.write(expected))
        val editing = checkNotNull(editorStore.read().plan)
        val future = "{\"version\":99,\"sessions\":[]}"
        preferences.edit().putString("plan", future).commit()
        assertFalse(editorStore.write(editing))
        assertEquals(future, preferences.getString("plan", null))
    }

    @Test fun `unknown fields at each schema level cannot be silently discarded`() {
        val records = listOf(
            JSONObject(legacy).put("weeklyTarget", 600),
            versionTwo().apply { getJSONObject("plan").put("weeklyTarget", 600) },
            versionTwo().apply { getJSONObject("plan").getJSONArray("courses").getJSONObject(0).put("color", "cyan") },
            versionTwo().apply { getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("locked", true) },
        )
        records.forEach { assertRejectedAndPreserved(it.toString()) }
    }

    @Test fun `schema versions require integers and supported document kinds`() {
        listOf<Any>("2", true, JSONObject.NULL, 0, -1, Long.MAX_VALUE).forEach {
            assertRejectedAndPreserved(versionTwo().put("version", it).toString())
        }
        assertRejectedAndPreserved(versionTwo().toString().replace("\"version\":2", "\"version\":2.0"))
        listOf<Any>("timed", "DATE_ONLY", 2, JSONObject.NULL).forEach {
            assertRejectedAndPreserved(versionTwo().put("kind", it).toString())
        }
    }

    @Test fun `missing fields mistyped numbers invalid dates and trailing content are rejected`() {
        listOf(
            versionTwo().apply { remove("kind") }.toString(),
            versionTwo().apply { getJSONObject("plan").remove("capacity") }.toString(),
            versionTwo().put("plan", "not-an-object").toString(),
            versionTwo().apply { getJSONObject("plan").getJSONArray("courses").getJSONObject(0).put("baselineSeconds", "120") }.toString(),
            versionTwo().apply { getJSONObject("plan").put("sessionMinutes", 25.5) }.toString(),
            versionTwo().apply { getJSONObject("plan").put("start", "2026-02-30") }.toString(),
            versionTwo().apply { getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("id", Long.MAX_VALUE) }.toString(),
            versionTwo().toString() + "{}",
        ).forEach(::assertRejectedAndPreserved)
    }

    @Test fun `explicit clear permits recovery without touching unrelated preferences`() {
        preferences.edit().putString("plan", "{\"version\":99}").putString("unrelated", "keep").commit()
        val store = StudyPlannerStore(context)
        assertFalse(store.write(expected))
        assertTrue(store.clear())
        assertTrue(store.write(expected))
        assertEquals(expected, StudyPlannerStore(context).read().plan)
        assertEquals("keep", preferences.getString("unrelated", null))
    }

    @Test fun `wrong preference value type is protected until explicitly cleared`() {
        preferences.edit().putInt("plan", 42).commit()
        val store = StudyPlannerStore(context)
        assertNotNull(store.read().error)
        assertFalse(store.write(expected))
        assertEquals(42, preferences.getInt("plan", 0))
        assertTrue(store.clear())
        assertTrue(store.write(expected))
    }

    private fun versionTwo(): JSONObject = JSONObject().apply {
        put("version", 2)
        put("kind", "date_only")
        put("plan", JSONObject(legacy).apply { remove("version") })
    }

    private fun assertRejectedAndPreserved(raw: String) {
        preferences.edit().putString("plan", raw).commit()
        val store = StudyPlannerStore(context)
        assertNull(store.read().plan)
        assertNotNull(store.read().error)
        assertFalse(store.write(expected))
        assertEquals(raw, preferences.getString("plan", null))
    }
}
