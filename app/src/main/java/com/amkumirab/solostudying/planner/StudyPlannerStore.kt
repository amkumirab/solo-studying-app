package com.amkumirab.solostudying.planner

import android.content.Context
import android.annotation.SuppressLint
import com.amkumirab.solostudying.domain.planner.StudyPlan
import com.amkumirab.solostudying.domain.planner.PlannerSubject
import com.amkumirab.solostudying.domain.planner.PlannerSnapshot
import com.amkumirab.solostudying.domain.planner.WeeklyAvailability

data class StoredStudyPlan(val plan: StudyPlan? = null, val error: String? = null)

data class StoredPlannerData(
    val plan: StudyPlan?, val subjects: List<PlannerSubject>, val planError: String?, val subjectsError: String?,
    val availability: WeeklyAvailability? = null, val availabilityError: String? = null,
) {
    val snapshot get() = PlannerSnapshot(plan, subjects, availability)
}

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
    internal fun write(plan: StudyPlan): Boolean = synchronized(mutationLock) {
        try {
            // Do not discard a newer schema or corrupt record behind an already-open editor.
            preferences.getString("plan", null)?.let(::decodeStudyPlan)
            return@synchronized preferences.edit().putString("plan", encodeStudyPlan(plan)).commit()
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("UseKtx")
    fun clear(): Boolean = synchronized(mutationLock) {
        preferences.edit().remove("plan").commit()
    }

    fun readData(): StoredPlannerData = synchronized(mutationLock) {
        val plan = read()
        var subjectsError: String? = null
        val subjects = try {
            val raw = preferences.getString("subjects", null)
            raw?.let(::decodePlannerSubjects).orEmpty()
        } catch (_: Exception) {
            subjectsError = "Your saved subjects could not be read. Reset subjects to recover; your goals and study history are safe."
            emptyList()
        }
        var availabilityError: String? = null
        val availability = try {
            preferences.getString("availability", null)?.let(::decodeWeeklyAvailability)
        } catch (_: Exception) {
            availabilityError = "Your saved availability could not be read. Reset availability to recover; your plan and study history are safe."
            null
        }
        StoredPlannerData(plan.plan, subjects, plan.error, subjectsError, availability, availabilityError)
    }

    /** Compare all planner records to reject stale editors; publish in one commit. */
    @SuppressLint("UseKtx")
    fun writeSnapshot(expected: PlannerSnapshot, updated: PlannerSnapshot): Boolean = synchronized(mutationLock) {
        try {
            val current = readData()
            if (current.planError != null || current.subjectsError != null || current.availabilityError != null || current.snapshot != expected) return@synchronized false
            val plan = updated.plan?.let(::encodeStudyPlan)
            val editor = preferences.edit()
            if (updated.subjects != expected.subjects) editor.putString("subjects", encodePlannerSubjects(updated.subjects))
            if (updated.plan != expected.plan) {
                if (plan == null) editor.remove("plan") else editor.putString("plan", plan)
            }
            if (updated.availability != expected.availability) {
                if (updated.availability == null) editor.remove("availability")
                else editor.putString("availability", encodeWeeklyAvailability(updated.availability))
            }
            return@synchronized editor.commit()
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("UseKtx")
    fun clearSubjects(): Boolean = synchronized(mutationLock) {
        preferences.edit().remove("subjects").commit()
    }

    @SuppressLint("UseKtx")
    fun clearAvailability(): Boolean = synchronized(mutationLock) {
        preferences.edit().remove("availability").commit()
    }

    companion object {
        const val PREFERENCES_NAME = "offline_study_planner"
        private val mutationLock = Any()
    }
}
