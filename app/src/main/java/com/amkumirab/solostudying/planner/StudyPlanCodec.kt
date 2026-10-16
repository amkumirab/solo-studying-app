package com.amkumirab.solostudying.planner

import com.amkumirab.solostudying.domain.planner.PlannerConfig
import com.amkumirab.solostudying.domain.planner.PlannerCourse
import com.amkumirab.solostudying.domain.planner.StudyBlock
import com.amkumirab.solostudying.domain.planner.StudyPlan
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

private const val CURRENT_VERSION = 2
private const val MAX_RECORD_CHARACTERS = 2_000_000
private val planFields = setOf("start", "sessionMinutes", "capacity", "courses", "blocks")
private val courseFields = setOf("bossId", "remainingMinutes", "finishBy", "priority", "baselineSeconds")
private val blockFields = setOf("id", "bossId", "date", "minutes")

/** Encodes the current date-only schema. No scheduling or credit recalculation occurs. */
fun encodeStudyPlan(plan: StudyPlan): String {
    val body = JSONObject().apply {
        put("start", plan.config.start.toString())
        put("sessionMinutes", plan.config.sessionMinutes)
        put("capacity", JSONArray(plan.config.weekdayMinutes))
        put("courses", JSONArray().apply {
            plan.config.courses.forEach { course ->
                put(JSONObject().apply {
                    put("bossId", course.bossId)
                    put("remainingMinutes", course.remainingMinutes)
                    put("finishBy", course.finishBy.toString())
                    put("priority", course.priority)
                    put("baselineSeconds", course.baselineSeconds)
                })
            }
        })
        put("blocks", JSONArray().apply {
            plan.blocks.forEach { block ->
                put(JSONObject().apply {
                    put("id", block.id)
                    put("bossId", block.bossId)
                    put("date", block.date.toString())
                    put("minutes", block.minutes)
                })
            }
        })
    }
    return JSONObject().apply {
        put("version", CURRENT_VERSION)
        put("kind", "date_only")
        put("plan", body)
    }.toString().also { require(it.length <= MAX_RECORD_CHARACTERS) }
}

/** Reads versions 1 and 2 without writing storage; invalid or unknown schemas throw. */
fun decodeStudyPlan(text: String): StudyPlan {
    require(text.length <= MAX_RECORD_CHARACTERS)
    val tokens = JSONTokener(text)
    val json = tokens.nextValue()
    require(json is JSONObject && tokens.nextClean() == '\u0000')
    val body = when (json.strictLong("version")) {
        1L -> json.also { it.requireFields(planFields + "version") }
        CURRENT_VERSION.toLong() -> {
            json.requireFields(setOf("version", "kind", "plan"))
            require(json.get("kind") == "date_only")
            json.getJSONObject("plan").also { it.requireFields(planFields) }
        }
        else -> throw IllegalArgumentException("Unsupported study plan version")
    }
    return decodePlanBody(body)
}

private fun decodePlanBody(json: JSONObject): StudyPlan {
    val courses = json.getJSONArray("courses")
    val capacity = json.getJSONArray("capacity")
    val blocks = json.getJSONArray("blocks")
    require(courses.length() in 1..20 && capacity.length() == 7 && blocks.length() <= 18000)
    val config = PlannerConfig(
        start = json.strictDate("start"),
        courses = List(courses.length()) { index ->
            val course = courses.getJSONObject(index)
            course.requireFields(courseFields)
            PlannerCourse(
                course.strictInt("bossId"), course.strictInt("remainingMinutes"), course.strictDate("finishBy"),
                course.strictInt("priority"), course.strictLong("baselineSeconds"),
            )
        },
        weekdayMinutes = List(7) { index -> strictNumber(capacity.get(index)).also { require(it in 0..480) }.toInt() },
        sessionMinutes = json.strictInt("sessionMinutes"),
    )
    return StudyPlan(config, List(blocks.length()) { index ->
        val block = blocks.getJSONObject(index)
        block.requireFields(blockFields)
        StudyBlock(block.strictInt("id"), block.strictInt("bossId"), block.strictDate("date"), block.strictInt("minutes"))
    })
}

private fun JSONObject.requireFields(expected: Set<String>) {
    require(keys().asSequence().toSet() == expected)
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
