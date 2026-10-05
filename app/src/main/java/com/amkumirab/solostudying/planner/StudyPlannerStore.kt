package com.amkumirab.solostudying.planner

import android.content.Context
import android.annotation.SuppressLint
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

data class StoredStudyPlan(val plan: StudyPlan? = null, val error: String? = null)

class StudyPlannerStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    fun read(): StoredStudyPlan = try {
        val text = preferences.getString("plan", null)
        if (text == null) StoredStudyPlan() else StoredStudyPlan(decodeStudyPlan(text))
    } catch (_: Exception) {
        StoredStudyPlan(error = "Your saved plan could not be read. Clear it to create a new one; study history is safe.")
    }
    // Explicit commit preserves the success result; KTX edit returns Unit.
    @SuppressLint("UseKtx")
    fun write(plan: StudyPlan): Boolean = preferences.edit().putString("plan", encodeStudyPlan(plan)).commit()
    @SuppressLint("UseKtx")
    fun clear(): Boolean = preferences.edit().remove("plan").commit()

    companion object { const val PREFERENCES_NAME = "offline_study_planner" }
}

fun encodeStudyPlan(plan: StudyPlan): String = JSONObject().apply {
    put("version", 1)
    put("start", plan.config.start.toString())
    put("sessionMinutes", plan.config.sessionMinutes)
    put("capacity", JSONArray(plan.config.weekdayMinutes))
    put("courses", JSONArray().apply {
        plan.config.courses.forEach { course -> put(JSONObject().apply {
            put("bossId", course.bossId)
            put("remainingMinutes", course.remainingMinutes)
            put("finishBy", course.finishBy.toString())
            put("priority", course.priority)
            put("baselineSeconds", course.baselineSeconds)
        }) }
    })
    put("blocks", JSONArray().apply {
        plan.blocks.forEach { block -> put(JSONObject().apply {
            put("id", block.id)
            put("bossId", block.bossId)
            put("date", block.date.toString())
            put("minutes", block.minutes)
        }) }
    })
}.toString()

fun decodeStudyPlan(text: String): StudyPlan {
    require(text.length <= 2_000_000)
    val json = JSONObject(text)
    require(json.strictLong("version") == 1L)
    val courses = json.getJSONArray("courses")
    val capacity = json.getJSONArray("capacity")
    val blocks = json.getJSONArray("blocks")
    require(courses.length() in 1..20 && capacity.length() == 7 && blocks.length() <= 18000)
    val config = PlannerConfig(
        start = json.strictDate("start"),
        courses = List(courses.length()) { index -> courses.getJSONObject(index).let {
            PlannerCourse(it.strictInt("bossId"), it.strictInt("remainingMinutes"), it.strictDate("finishBy"),
                it.strictInt("priority"), it.strictLong("baselineSeconds"))
        } },
        weekdayMinutes = List(7) { index -> strictNumber(capacity.get(index)).also { require(it in 0..480) }.toInt() },
        sessionMinutes = json.strictInt("sessionMinutes"),
    )
    return StudyPlan(config, List(blocks.length()) { index -> blocks.getJSONObject(index).let {
        StudyBlock(it.strictInt("id"), it.strictInt("bossId"), it.strictDate("date"), it.strictInt("minutes"))
    } })
}

private fun strictNumber(value: Any): Long {
    require(value is Int || value is Long)
    return (value as Number).toLong()
}
private fun JSONObject.strictLong(key: String) = strictNumber(get(key))
private fun JSONObject.strictInt(key: String) = strictLong(key).also { require(it in Int.MIN_VALUE..Int.MAX_VALUE) }.toInt()
private fun JSONObject.strictDate(key: String): LocalDate {
    val value = get(key)
    require(value is String && value.length == 10)
    return LocalDate.parse(value)
}
