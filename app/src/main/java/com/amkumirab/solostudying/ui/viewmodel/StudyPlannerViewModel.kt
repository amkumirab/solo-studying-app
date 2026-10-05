package com.amkumirab.solostudying.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.domain.planner.StudyPlan
import com.amkumirab.solostudying.planner.StudyPlannerStore
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
)

class StudyPlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val store = StudyPlannerStore(application)
    private val mutableState = MutableStateFlow(StudyPlannerState())
    val state = mutableState.asStateFlow()
    init {
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) { store.read() }
            mutableState.value = StudyPlannerState(loaded = true, plan = stored.plan, error = stored.error, unreadableRecord = stored.error != null)
        }
    }
    fun save(plan: StudyPlan, onSaved: () -> Unit = {}) = update(plan, onSaved)
    fun clear(onCleared: () -> Unit = {}) = update(null, onCleared)
    private fun update(plan: StudyPlan?, onSuccess: () -> Unit) {
        if (!state.value.loaded || state.value.busy) return
        mutableState.value = state.value.copy(busy = true)
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching { if (plan == null) store.clear() else store.write(plan) }.getOrDefault(false) }
            if (saved) {
                mutableState.value = StudyPlannerState(loaded = true, plan = plan)
                onSuccess()
            } else mutableState.value = state.value.copy(busy = false, error = "Could not save your plan. Please try again.")
        }
    }
}
