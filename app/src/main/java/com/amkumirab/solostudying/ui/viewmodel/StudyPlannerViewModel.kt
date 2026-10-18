package com.amkumirab.solostudying.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.domain.planner.StudyPlan
import com.amkumirab.solostudying.domain.planner.PlannerSubject
import com.amkumirab.solostudying.domain.planner.PlannerSnapshot
import com.amkumirab.solostudying.domain.planner.WeeklyAvailability
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.planner.StudyPlannerStore
import com.amkumirab.solostudying.planner.StoredPlannerData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StudyPlannerState(
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val plan: StudyPlan? = null,
    val error: String? = null,
    val unreadableRecord: Boolean = false,
    val subjects: List<PlannerSubject> = emptyList(),
    val subjectError: String? = null,
    val unreadableSubjects: Boolean = false,
    val editorGeneration: Int = 0,
    val availability: WeeklyAvailability? = null,
    val availabilityError: String? = null,
    val unreadableAvailability: Boolean = false,
) {
    val snapshot get() = PlannerSnapshot(plan, subjects, availability)
}

private enum class PlannerRecord { Plan, Subjects, Availability }

class StudyPlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val store = StudyPlannerStore(application)
    private val mutableState = MutableStateFlow(StudyPlannerState())
    val state = mutableState.asStateFlow()
    init {
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) { store.readData() }
            mutableState.value = stored.toUiState()
        }
    }
    fun save(plan: StudyPlan, expected: PlannerSnapshot, onSaved: () -> Unit = {}) =
        update(expected.copy(plan = plan), PlannerRecord.Plan, onSaved, expected)
    fun saveAvailability(availability: WeeklyAvailability, expected: PlannerSnapshot, onSaved: () -> Unit = {}) =
        update(expected.copy(availability = availability), PlannerRecord.Availability, onSaved, expected)
    fun saveSubject(subject: PlannerSubject, goals: List<BossEntity>, onSaved: () -> Unit) {
        if (!state.value.loaded || state.value.busy) return
        if (goals.none { it.id == subject.bossId && !it.isCompleted }) {
            mutableState.value = state.value.copy(subjectError = "The linked goal is unavailable or complete.")
            return
        }
        val updated = runCatching { state.value.snapshot.copy(subjects =
            state.value.subjects.filterNot { it.bossId == subject.bossId } + subject) }.getOrNull()
        if (updated == null) mutableState.value = state.value.copy(subjectError = "Subject settings are invalid or duplicated.")
        else update(updated, PlannerRecord.Subjects, onSaved)
    }
    fun removeSubject(bossId: Int, onRemoved: () -> Unit) = update(state.value.snapshot.withoutSubject(bossId), PlannerRecord.Subjects, onRemoved)
    fun clear(onCleared: () -> Unit = {}) = recover(PlannerRecord.Plan, onCleared)
    fun clearSubjects(onCleared: () -> Unit) = recover(PlannerRecord.Subjects, onCleared)
    fun clearAvailability(onCleared: () -> Unit) = recover(PlannerRecord.Availability, onCleared)

    private fun update(updated: PlannerSnapshot, record: PlannerRecord, onSuccess: () -> Unit,
                       expected: PlannerSnapshot = state.value.snapshot) {
        if (!state.value.loaded || state.value.busy) return
        val generation = state.value.editorGeneration
        mutableState.value = state.value.copy(busy = true)
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { store.writeSnapshot(expected, updated) }
            if (saved) {
                mutableState.value = StudyPlannerState(loaded = true, plan = updated.plan, subjects = updated.subjects,
                    availability = updated.availability, editorGeneration = generation)
                onSuccess()
            } else {
                val latest = withContext(Dispatchers.IO) { store.readData() }.toUiState().copy(editorGeneration = generation + 1)
                val message = "Could not save your changes. Saved settings may have changed; reopen the editor and try again."
                mutableState.value = when (record) {
                    PlannerRecord.Plan -> latest.copy(error = latest.error ?: message)
                    PlannerRecord.Subjects -> latest.copy(subjectError = latest.subjectError ?: message)
                    PlannerRecord.Availability -> latest.copy(availabilityError = latest.availabilityError ?: message)
                }
            }
        }
    }

    private fun recover(record: PlannerRecord, onSuccess: () -> Unit) {
        if (!state.value.loaded || state.value.busy) return
        mutableState.value = state.value.copy(busy = true)
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching { when (record) {
                PlannerRecord.Plan -> store.clear()
                PlannerRecord.Subjects -> store.clearSubjects()
                PlannerRecord.Availability -> store.clearAvailability()
            } }.getOrDefault(false) }
            val latest = withContext(Dispatchers.IO) { store.readData() }.toUiState().copy(editorGeneration = state.value.editorGeneration + 1)
            mutableState.value = if (saved) latest else when (record) {
                PlannerRecord.Plan -> latest.copy(error = "Could not remove your plan. Try again.")
                PlannerRecord.Subjects -> latest.copy(subjectError = "Could not reset subjects. Try again.")
                PlannerRecord.Availability -> latest.copy(availabilityError = "Could not reset availability. Try again.")
            }
            if (saved) onSuccess()
        }
    }
}

private fun StoredPlannerData.toUiState() = StudyPlannerState(
    loaded = true, plan = plan, subjects = subjects, error = planError, unreadableRecord = planError != null,
    subjectError = subjectsError, unreadableSubjects = subjectsError != null,
    availability = availability, availabilityError = availabilityError, unreadableAvailability = availabilityError != null,
)
