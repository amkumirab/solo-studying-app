package com.amkumirab.solostudying.planner

import android.content.Context
import android.annotation.SuppressLint
import com.amkumirab.solostudying.domain.planner.StudyPlan

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

    companion object {
        const val PREFERENCES_NAME = "offline_study_planner"
        private val mutationLock = Any()
    }
}
