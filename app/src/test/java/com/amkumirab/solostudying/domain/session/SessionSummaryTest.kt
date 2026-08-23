package com.amkumirab.solostudying.domain.session

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSummaryTest {

    @Test
    fun `progress is clamped while gained time keeps the recorded difference`() {
        val progress = ProgressSummary(
            name = "Final Exam",
            beforeSeconds = 50L,
            afterSeconds = 130L,
            targetSeconds = 100L,
        )

        assertEquals(0.5f, progress.progressBefore)
        assertEquals(1f, progress.progressAfter)
        assertEquals(80L, progress.gainedSeconds)
    }

    @Test
    fun `invalid target produces safe empty progress`() {
        val progress = ProgressSummary(
            name = "Open study",
            beforeSeconds = 20L,
            afterSeconds = 10L,
            targetSeconds = 0L,
        )

        assertEquals(0f, progress.progressBefore)
        assertEquals(0f, progress.progressAfter)
        assertEquals(0L, progress.gainedSeconds)
    }
}
