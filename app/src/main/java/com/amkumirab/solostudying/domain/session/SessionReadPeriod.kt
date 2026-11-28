package com.amkumirab.solostudying.domain.session

import java.time.LocalDate
import java.time.ZoneId

internal data class SessionReadPeriod(val today: LocalDate, val zoneId: ZoneId) {
    private val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())

    val todayMillis = today.startMillis()
    val tomorrowMillis = today.plusDays(1).startMillis()
    val weekMillis = weekStart.startMillis()
    val nextWeekMillis = weekStart.plusWeeks(1).startMillis()

    private fun LocalDate.startMillis() = atStartOfDay(zoneId).toInstant().toEpochMilli()

    companion object {
        fun current(): SessionReadPeriod {
            val zone = ZoneId.systemDefault()
            return SessionReadPeriod(LocalDate.now(zone), zone)
        }
    }
}
