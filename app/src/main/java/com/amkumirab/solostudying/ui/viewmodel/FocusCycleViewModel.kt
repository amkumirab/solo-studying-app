package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.amkumirab.solostudying.domain.session.SessionEndState
import com.amkumirab.solostudying.domain.session.SessionSummary
import com.amkumirab.solostudying.focuscycle.FocusCyclePhase
import com.amkumirab.solostudying.focuscycle.FocusCyclePlan
import com.amkumirab.solostudying.focuscycle.FocusCycleState
import com.amkumirab.solostudying.focuscycle.FocusCycleStore

class FocusCycleViewModel(
    context: Context,
    private val store: FocusCycleStore = FocusCycleStore(context),
) : ViewModel() {

    var state by mutableStateOf(store.read())
        private set

    fun start(plan: FocusCyclePlan) {
        update(FocusCycleState(plan = plan))
    }

    fun onSessionFinished(summary: SessionSummary) {
        val current = state ?: return
        if (current.phase != FocusCyclePhase.FOCUS || current.lastSessionId == summary.sessionId) return
        val completedFullRound = summary.endState == SessionEndState.Completed &&
            summary.durationSeconds >= current.plan.focusMinutes * 60L
        if (!completedFullRound) {
            cancel()
            return
        }

        val completed = current.completedRounds + 1
        update(
            current.copy(
                completedRounds = completed,
                focusedSeconds = current.focusedSeconds + summary.durationSeconds,
                phase = if (completed >= current.plan.totalRounds) {
                    FocusCyclePhase.COMPLETE
                } else {
                    FocusCyclePhase.READY_FOR_BREAK
                },
                lastSessionId = summary.sessionId,
            ),
        )
    }

    fun startBreak(): Int? {
        val current = state?.takeIf { it.phase == FocusCyclePhase.READY_FOR_BREAK } ?: return null
        update(current.copy(phase = FocusCyclePhase.BREAK))
        return current.plan.breakMinutes
    }

    fun finishBreak() {
        val current = state?.takeIf { it.phase == FocusCyclePhase.BREAK } ?: return
        update(current.copy(phase = FocusCyclePhase.READY_FOR_FOCUS))
    }

    fun startNextRound(): FocusCyclePlan? {
        val current = state?.takeIf { it.phase == FocusCyclePhase.READY_FOR_FOCUS } ?: return null
        update(
            current.copy(
                currentRound = current.nextRound,
                phase = FocusCyclePhase.FOCUS,
            ),
        )
        return current.plan
    }

    fun cancel() {
        state = null
        store.clear()
    }

    fun dismissCompleted() = cancel()

    private fun update(value: FocusCycleState) {
        state = value
        store.write(value)
    }
}
