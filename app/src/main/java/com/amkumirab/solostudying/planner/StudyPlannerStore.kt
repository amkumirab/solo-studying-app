package com.amkumirab.solostudying.planner

import android.content.Context
import android.annotation.SuppressLint
import com.amkumirab.solostudying.domain.planner.StudyPlan
import com.amkumirab.solostudying.domain.planner.PlannerSubject
import com.amkumirab.solostudying.domain.planner.PlannerSnapshot

data class StoredStudyPlan(val plan: StudyPlan? = null, val error: String? = null)

data class StoredPlannerData(
    val plan: StudyPlan?, val subjects: List<PlannerSubject>, val planError: String?, val subjectsError: String?,
) {
    val snapshot get() = PlannerSnapshot(plan, subjects)
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
    fun write(plan: StudyPlan): Boolean = synchronized(mutationLock) {
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
        try {
            val raw = preferences.getString("subjects", null)
            StoredPlannerData(plan.plan, raw?.let(::decodePlannerSubjects).orEmpty(), plan.error, null)
        } catch (_: Exception) {
            StoredPlannerData(plan.plan, emptyList(), plan.error,
                "Your saved subjects could not be read. Reset subjects to recover; your goals and study history are safe.")
        }
    }

    /** Compare both records to reject stale editors; publish changes in one commit. */
    @SuppressLint("UseKtx")
    fun writeSnapshot(expected: PlannerSnapshot, updated: PlannerSnapshot): Boolean = synchronized(mutationLock) {
        try {
            val current = readData()
            if (current.planError != null || current.subjectsError != null || current.snapshot != expected) return@synchronized false
            val subjects = encodePlannerSubjects(updated.subjects)
            val plan = updated.plan?.let(::encodeStudyPlan)
            val editor = preferences.edit().putString("subjects", subjects)
            if (updated.plan != expected.plan) {
                if (plan == null) editor.remove("plan") else editor.putString("plan", plan)
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

    companion object {
        const val PREFERENCES_NAME = "offline_study_planner"
        private val mutationLock = Any()
    }
}
