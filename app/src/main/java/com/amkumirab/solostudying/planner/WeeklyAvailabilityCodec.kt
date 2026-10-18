package com.amkumirab.solostudying.planner

import com.amkumirab.solostudying.domain.planner.*
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

fun encodeWeeklyAvailability(availability: WeeklyAvailability): String =
    JSONObject().put("version", 1).put("days", JSONArray().apply {
        availability.days.forEach { day -> put(JSONObject().put("enabled", day.enabled)
            .put("windows", JSONArray().apply {
                day.windows.forEach { window -> put(JSONObject().put("start", window.startMinute).put("end", window.endMinute)) }
            })) }
    }).toString().also { require(it.length <= 16384) }

fun decodeWeeklyAvailability(text: String): WeeklyAvailability {
    require(text.length <= 16384)
    val tokens = JSONTokener(text)
    val root = tokens.nextValue()
    require(root is JSONObject && tokens.nextClean() == '\u0000')
    require(root.keys().asSequence().toSet() == setOf("version", "days") && root.get("version") == 1)
    val days = root.getJSONArray("days")
    require(days.length() == 7)
    return WeeklyAvailability(List(7) { index ->
        val day = days.getJSONObject(index)
        require(day.keys().asSequence().toSet() == setOf("enabled", "windows"))
        val enabled = day.get("enabled")
        require(enabled is Boolean)
        val windows = day.getJSONArray("windows")
        require(windows.length() <= 16)
        StudyDayAvailability(enabled, List(windows.length()) { windowIndex ->
            val window = windows.getJSONObject(windowIndex)
            require(window.keys().asSequence().toSet() == setOf("start", "end"))
            val start = window.get("start")
            val end = window.get("end")
            require(start is Int && end is Int)
            StudyTimeWindow(start, end)
        })
    })
}
