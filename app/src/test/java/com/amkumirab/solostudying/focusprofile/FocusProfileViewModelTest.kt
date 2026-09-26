package com.amkumirab.solostudying.focusprofile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.ui.viewmodel.FocusProfileViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FocusProfileViewModelTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteSharedPreferences(FocusProfileStore.PREFERENCES_NAME)
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(FocusProfileStore.PREFERENCES_NAME)
    }

    @Test
    fun `profiles can be created edited used and deleted`() {
        var now = 1_000L
        var nextId = 0
        val viewModel = FocusProfileViewModel(
            context = context,
            clock = { now },
            idProvider = { "profile-${++nextId}" },
        )

        viewModel.save(null, "Revision", FocusProfileType.SINGLE, 20, 5, 4, null, false, false)
        now = 2_000L
        viewModel.save(null, "Deep Work", FocusProfileType.CYCLE, 45, 10, 3, 8, true, true)

        assertEquals(listOf("Deep Work", "Revision"), viewModel.profiles.map { it.name })
        val revision = viewModel.profiles.last()
        viewModel.save(revision.id, "Exam Revision", FocusProfileType.SINGLE, 30, 5, 4, 4, true, false)
        val edited = viewModel.profiles.first { it.id == revision.id }
        assertEquals(1_000L, edited.createdAtMillis)
        assertEquals("Exam Revision", edited.name)

        now = 3_000L
        viewModel.markUsed(edited)
        assertEquals(edited.id, viewModel.profiles.first().id)
        assertEquals(3_000L, viewModel.profiles.first().lastUsedAtMillis)

        viewModel.delete(viewModel.profiles.first())
        assertFalse(viewModel.profiles.any { it.id == edited.id })
        assertEquals(viewModel.profiles, FocusProfileStore(context).read())
    }
}
