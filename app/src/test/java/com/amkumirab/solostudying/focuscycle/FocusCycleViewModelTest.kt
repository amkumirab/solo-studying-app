package com.amkumirab.solostudying.focuscycle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.domain.session.SessionEndState
import com.amkumirab.solostudying.domain.session.SessionSummary
import com.amkumirab.solostudying.ui.viewmodel.FocusCycleViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FocusCycleViewModelTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteSharedPreferences(FocusCycleStore.PREFERENCES_NAME)
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(FocusCycleStore.PREFERENCES_NAME)
    }

    @Test
    fun `completed rounds move through break and restore from storage`() {
        val viewModel = FocusCycleViewModel(context)
        val plan = FocusCyclePlan(
            focusMinutes = 25,
            breakMinutes = 5,
            totalRounds = 2,
            skillId = 7,
            useFocusShield = true,
            useStrictFocus = true,
        )

        viewModel.start(plan)
        viewModel.onSessionFinished(summary(id = 10, durationSeconds = 1_500))

        assertEquals(FocusCyclePhase.READY_FOR_BREAK, viewModel.state?.phase)
        assertEquals(1, viewModel.state?.completedRounds)
        assertEquals(1_500L, viewModel.state?.focusedSeconds)
        assertEquals(5, viewModel.startBreak())
        viewModel.finishBreak()
        assertEquals(FocusCyclePhase.READY_FOR_FOCUS, viewModel.state?.phase)

        val restored = FocusCycleViewModel(context)
        assertEquals(plan, restored.state?.plan)
        assertEquals(2, restored.startNextRound()?.totalRounds)
        assertEquals(2, restored.state?.currentRound)
        assertEquals(FocusCyclePhase.FOCUS, restored.state?.phase)
    }

    @Test
    fun `same session is counted once and final round completes cycle`() {
        val viewModel = FocusCycleViewModel(context)
        viewModel.start(FocusCyclePlan(focusMinutes = 1, breakMinutes = 1, totalRounds = 2))

        val first = summary(id = 1, durationSeconds = 60)
        viewModel.onSessionFinished(first)
        viewModel.onSessionFinished(first)
        assertEquals(1, viewModel.state?.completedRounds)
        assertEquals(60L, viewModel.state?.focusedSeconds)

        viewModel.startBreak()
        viewModel.finishBreak()
        viewModel.startNextRound()
        viewModel.onSessionFinished(summary(id = 2, durationSeconds = 60))

        assertEquals(FocusCyclePhase.COMPLETE, viewModel.state?.phase)
        assertEquals(2, viewModel.state?.completedRounds)
        assertEquals(120L, viewModel.state?.focusedSeconds)
    }

    @Test
    fun `suspended focus cancels cycle`() {
        val viewModel = FocusCycleViewModel(context)
        viewModel.start(FocusCyclePlan.STANDARD)

        viewModel.onSessionFinished(summary(id = 4, endState = SessionEndState.Suspended))

        assertNull(viewModel.state)
        assertNull(FocusCycleStore(context).read())
    }

    @Test
    fun `ending a focus round early does not count it`() {
        val viewModel = FocusCycleViewModel(context)
        viewModel.start(FocusCyclePlan(focusMinutes = 25, breakMinutes = 5, totalRounds = 4))

        viewModel.onSessionFinished(summary(id = 5, durationSeconds = 12 * 60L))

        assertNull(viewModel.state)
        assertNull(FocusCycleStore(context).read())
    }

    private fun summary(
        id: Long,
        durationSeconds: Long = 30,
        endState: SessionEndState = SessionEndState.Completed,
    ) = SessionSummary(
        sessionId = id,
        subject = "General Focus",
        durationSeconds = durationSeconds,
        xpEarned = 0,
        goldEarned = 0,
        endState = endState,
        previousLevel = 1,
        currentLevel = 1,
        previousStreak = 0,
        currentStreak = 0,
    )
}
