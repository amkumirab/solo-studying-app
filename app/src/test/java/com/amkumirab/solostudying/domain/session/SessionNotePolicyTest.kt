package com.amkumirab.solostudying.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionNotePolicyTest {

    @Test
    fun `notes are trimmed and blank notes are removed`() {
        assertEquals("Continue with chapter four", SessionNotePolicy.normalize("  Continue with chapter four  "))
        assertNull(SessionNotePolicy.normalize("   \n  "))
    }

    @Test
    fun `notes cannot exceed the storage limit`() {
        val normalized = SessionNotePolicy.normalize("x".repeat(SessionNotePolicy.MaxLength + 50))
        assertEquals(SessionNotePolicy.MaxLength, normalized?.length)
    }
}
