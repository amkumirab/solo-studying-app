package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.breaks.BreakSessionSnapshot
import com.amkumirab.solostudying.breaks.BreakSessionStore
import com.amkumirab.solostudying.breaks.remainingSeconds
import com.amkumirab.solostudying.notification.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BreakViewModel(
    context: Context,
    private val store: BreakSessionStore = BreakSessionStore(context),
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val applicationContext = context.applicationContext
    private var timerJob: Job? = null

    var activeBreak by mutableStateOf<BreakSessionSnapshot?>(null)
        private set

    var breakTimeLeftSeconds by mutableLongStateOf(0L)
        private set

    var showBreakComplete by mutableStateOf(false)
        private set

    var breakSuggestionsEnabled by mutableStateOf(store.areSuggestionsEnabled())
        private set

    init {
        restoreBreak()
    }

    fun startBreak(minutes: Int) {
        require(minutes in ALLOWED_BREAK_MINUTES) { "Unsupported break duration: $minutes" }
        val durationSeconds = minutes * 60L
        val session = BreakSessionSnapshot(
            durationSeconds = durationSeconds,
            endTimeMillis = clock() + durationSeconds * 1_000L,
        )
        timerJob?.cancel()
        showBreakComplete = false
        activeBreak = session
        breakTimeLeftSeconds = durationSeconds
        store.saveSession(session)
        NotificationHelper.scheduleBreakAlarm(applicationContext, session.endTimeMillis)
        startTimer()
    }

    fun syncBreakTime() {
        val session = activeBreak ?: return
        val remaining = session.remainingSeconds(clock())
        breakTimeLeftSeconds = remaining
        if (remaining <= 0L) {
            finishBreak()
        }
    }

    fun skipBreak() {
        timerJob?.cancel()
        NotificationHelper.cancelBreakAlarm(applicationContext)
        store.clearSession()
        activeBreak = null
        breakTimeLeftSeconds = 0L
        showBreakComplete = false
    }

    fun dismissBreakComplete() {
        NotificationHelper.cancelBreakAlarm(applicationContext)
        showBreakComplete = false
    }

    fun updateBreakSuggestions(enabled: Boolean) {
        breakSuggestionsEnabled = enabled
        store.setSuggestionsEnabled(enabled)
    }

    private fun restoreBreak() {
        val saved = store.readSession() ?: return
        val remaining = saved.remainingSeconds(clock())
        if (remaining <= 0L) {
            NotificationHelper.cancelBreakAlarm(applicationContext)
            store.clearSession()
            showBreakComplete = true
            return
        }
        activeBreak = saved
        breakTimeLeftSeconds = remaining
        NotificationHelper.scheduleBreakAlarm(applicationContext, saved.endTimeMillis)
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (activeBreak != null && breakTimeLeftSeconds > 0L) {
                delay(1_000L)
                syncBreakTime()
            }
        }
    }

    private fun finishBreak() {
        timerJob?.cancel()
        store.clearSession()
        activeBreak = null
        breakTimeLeftSeconds = 0L
        showBreakComplete = true
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

    companion object {
        val ALLOWED_BREAK_MINUTES = setOf(5, 10, 15)
    }
}
