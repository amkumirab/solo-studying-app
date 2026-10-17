package com.amkumirab.solostudying.planner

import com.amkumirab.solostudying.domain.planner.PlannerSubject
import com.amkumirab.solostudying.domain.planner.SubjectColor
import com.amkumirab.solostudying.domain.planner.validatePlannerSubjects
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

fun encodePlannerSubjects(subjects: List<PlannerSubject>): String {
    validatePlannerSubjects(subjects)
    return JSONObject().put("version", 1).put("subjects", JSONArray().apply {
        subjects.forEach { subject -> put(JSONObject().apply {
            put("bossId", subject.bossId)
            put("name", subject.name)
            put("priority", subject.priority)
            put("color", subject.color.name)
            put("weeklyMinutes", subject.preferredWeeklyMinutes ?: JSONObject.NULL)
            put("deadline", subject.deadline?.toString() ?: JSONObject.NULL)
        }) }
    }).toString().also { require(it.length <= 32768) }
}

fun decodePlannerSubjects(text: String): List<PlannerSubject> {
    require(text.length <= 32768)
    val tokens = JSONTokener(text)
    val root = tokens.nextValue()
    require(root is JSONObject && tokens.nextClean() == '\u0000')
    require(root.keys().asSequence().toSet() == setOf("version", "subjects"))
    require(root.get("version") == 1)
    val values = root.getJSONArray("subjects")
    require(values.length() <= 20)
    return List(values.length()) { index ->
        val subject = values.getJSONObject(index)
        require(subject.keys().asSequence().toSet() == setOf("bossId", "name", "priority", "color", "weeklyMinutes", "deadline"))
        val name = subject.get("name")
        val color = subject.get("color")
        val deadline = subject.get("deadline")
        require(name is String && color is String)
        require(deadline == JSONObject.NULL || deadline is String && deadline.length == 10)
        PlannerSubject(
            subject.subjectInt("bossId"), name, subject.subjectInt("priority"), SubjectColor.valueOf(color),
            if (subject.isNull("weeklyMinutes")) null else subject.subjectInt("weeklyMinutes"),
            if (deadline == JSONObject.NULL) null else LocalDate.parse(deadline as String),
        )
    }.also(::validatePlannerSubjects)
}

private fun JSONObject.subjectInt(key: String): Int {
    val value = get(key)
    require(value is Int)
    return value
}
