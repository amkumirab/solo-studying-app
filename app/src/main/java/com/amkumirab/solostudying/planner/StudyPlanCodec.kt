package com.amkumirab.solostudying.planner

import com.amkumirab.solostudying.domain.planner.PlannerConfig
import com.amkumirab.solostudying.domain.planner.PlannerCourse
import com.amkumirab.solostudying.domain.planner.StudyBlock
import com.amkumirab.solostudying.domain.planner.StudyPlan
import com.amkumirab.solostudying.domain.planner.WeeklySchedulingPolicy
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

private const val MAX_RECORD_CHARACTERS = 2_000_000
private val planFields = setOf("start", "sessionMinutes", "capacity", "courses", "blocks")
private val courseFields = setOf("bossId", "remainingMinutes", "finishBy", "priority", "baselineSeconds")
private val blockFields = setOf("id", "bossId", "date", "minutes")

/** Encodes accepted data without scheduling or credit recalculation. */
fun encodeStudyPlan(plan: StudyPlan): String {
    val hasLocks = plan.blocks.any { it.locked }
    val hasDurationEdits = plan.blocks.any { it.durationEdited }
    val hasManualSessions = plan.blocks.any { it.manual }
    val body = JSONObject().apply {
        put("start", plan.config.start.toString())
        put("sessionMinutes", plan.config.sessionMinutes)
        plan.config.weeklyGoalMinutes?.let { put("weeklyGoalMinutes", it) }
        plan.config.weeklyScheduling?.let { policy ->
            put("weeklyCourseLimits", JSONArray().apply {
                policy.courseMinuteLimits.toSortedMap().forEach { (id, minutes) ->
                    put(JSONObject().put("bossId", id).put("minutes", minutes))
                }
            })
        }
        plan.config.studyWindows?.let { windows ->
            put("studyWindows", JSONObject(encodeWeeklyAvailability(windows)))
            if (plan.config.weeklyGoalMinutes == null) put("weeklyGoalMinutes", JSONObject.NULL)
            if (plan.config.weeklyScheduling == null) put("weeklyCourseLimits", JSONObject.NULL)
        }
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
                    block.startMinute?.let { put("startMinute", it) }
                    if (hasLocks || hasDurationEdits || hasManualSessions) put("locked", block.locked)
                    if (hasDurationEdits || hasManualSessions) put("durationEdited", block.durationEdited)
                    if (hasManualSessions) put("manual", block.manual)
                })
            }
        })
    }
    return JSONObject().apply {
        put("version", when {
            hasManualSessions -> 9
            hasDurationEdits -> 8
            hasLocks -> 7
            plan.config.studyWindows != null -> 6
            plan.config.weeklyScheduling != null -> 5
            plan.config.weeklyGoalMinutes != null -> 4
            else -> 3
        })
        put("kind", if (plan.config.studyWindows == null) "date_only" else "timed")
        put("plan", body)
    }.toString().also { require(it.length <= MAX_RECORD_CHARACTERS) }
}

/** Reads versions 1–9 without writing storage; legacy priority bounds remain enforced. */
fun decodeStudyPlan(text: String): StudyPlan {
    require(text.length <= MAX_RECORD_CHARACTERS)
    val tokens = JSONTokener(text)
    val json = tokens.nextValue()
    require(json is JSONObject && tokens.nextClean() == '\u0000')
    val version = json.strictLong("version")
    val body = when (version) {
        1L -> json.also { it.requireFields(planFields + "version") }
        2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L -> {
            json.requireFields(setOf("version", "kind", "plan"))
            require(json.get("kind") == if (version >= 6L) "timed" else "date_only")
            val fields = when (version) {
                6L, 7L, 8L, 9L -> planFields + setOf("weeklyGoalMinutes", "weeklyCourseLimits", "studyWindows")
                5L -> planFields + setOf("weeklyGoalMinutes", "weeklyCourseLimits")
                4L -> planFields + "weeklyGoalMinutes"
                else -> planFields
            }
            json.getJSONObject("plan").also { it.requireFields(fields) }
        }
        else -> throw IllegalArgumentException("Unsupported study plan version")
    }
    return decodePlanBody(body, if (version < 3) 3 else 5, version)
}

private fun decodePlanBody(json: JSONObject, maxPriority: Int, version: Long): StudyPlan {
    val courses = json.getJSONArray("courses")
    val capacity = json.getJSONArray("capacity")
    val blocks = json.getJSONArray("blocks")
    require(courses.length() in 1..20 && capacity.length() == 7 && blocks.length() <= 18000)
    val config = PlannerConfig(
        start = json.strictDate("start"),
        courses = List(courses.length()) { index ->
            val course = courses.getJSONObject(index)
            course.requireFields(courseFields)
            require(course.strictInt("priority") in 1..maxPriority)
            PlannerCourse(
                course.strictInt("bossId"), course.strictInt("remainingMinutes"), course.strictDate("finishBy"),
                course.strictInt("priority"), course.strictLong("baselineSeconds"),
            )
        },
        weekdayMinutes = List(7) { index -> strictNumber(capacity.get(index)).also { require(it in 0..480) }.toInt() },
        sessionMinutes = json.strictInt("sessionMinutes"),
        weeklyGoalMinutes = if (version < 4L || version >= 6L && json.isNull("weeklyGoalMinutes")) null else json.strictInt("weeklyGoalMinutes"),
        weeklyScheduling = if (version == 5L || version >= 6L && !json.isNull("weeklyCourseLimits")) decodeWeeklyPolicy(json.getJSONArray("weeklyCourseLimits")) else null,
        studyWindows = if (version >= 6L) decodeWeeklyAvailability(json.getJSONObject("studyWindows").toString()) else null,
    )
    return StudyPlan(config, List(blocks.length()) { index ->
        val block = blocks.getJSONObject(index)
        block.requireFields(when { version == 9L -> blockFields + setOf("startMinute", "locked", "durationEdited", "manual")
            version == 8L -> blockFields + setOf("startMinute", "locked", "durationEdited")
            version == 7L -> blockFields + setOf("startMinute", "locked")
            version == 6L -> blockFields + "startMinute"
            else -> blockFields })
        StudyBlock(block.strictInt("id"), block.strictInt("bossId"), block.strictDate("date"), block.strictInt("minutes"),
            if (version >= 6L) block.strictInt("startMinute") else null,
            if (version >= 7L) block.strictBoolean("locked") else false,
            if (version >= 8L) block.strictBoolean("durationEdited") else false,
            if (version == 9L) block.strictBoolean("manual") else false)
    })
}

private fun decodeWeeklyPolicy(values: JSONArray): WeeklySchedulingPolicy {
    require(values.length() <= 20)
    val limits = List(values.length()) { index ->
        val entry = values.getJSONObject(index)
        entry.requireFields(setOf("bossId", "minutes"))
        entry.strictInt("bossId") to entry.strictInt("minutes")
    }
    require(limits.map { it.first }.distinct().size == limits.size)
    return WeeklySchedulingPolicy(limits.toMap())
}

private fun JSONObject.requireFields(expected: Set<String>) {
    require(keys().asSequence().toSet() == expected)
}

private fun strictNumber(value: Any): Long {
    require(value is Int || value is Long)
    return (value as Number).toLong()
}

private fun JSONObject.strictLong(key: String) = strictNumber(get(key))
private fun JSONObject.strictBoolean(key: String): Boolean = get(key).also { require(it is Boolean) } as Boolean
private fun JSONObject.strictInt(key: String) = strictLong(key).also { require(it in Int.MIN_VALUE..Int.MAX_VALUE) }.toInt()
private fun JSONObject.strictDate(key: String): LocalDate {
    val value = get(key)
    require(value is String && value.length == 10)
    return LocalDate.parse(value)
}
