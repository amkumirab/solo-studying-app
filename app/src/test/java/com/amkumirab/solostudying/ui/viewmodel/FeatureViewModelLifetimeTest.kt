package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.focus.FocusSessionSnapshot
import com.amkumirab.solostudying.focus.FocusSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FeatureViewModelLifetimeTest {
    @Test
    fun `owner teardown cancels feature scopes without clearing saved focus`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, SoloStudyingDatabase::class.java).build()
        val sessions = FocusSessionStore(context)
        val paused = FocusSessionSnapshot(
            isActive = true, isFreeStudy = true, isPaused = true,
            timeLeftSeconds = 1_260, timeSpentSeconds = 240,
            initialBossTimeSpentSeconds = 0, lastTickTimeMillis = 1_000,
            bossId = null, skillId = null,
        )
        sessions.write(paused)
        val owner = ViewModelStore()
        Dispatchers.setMain(StandardTestDispatcher())
        try {
            val factory = SoloStudyingViewModelFactory(SoloStudyingRepository(database), context)
            val app = ViewModelProvider.create(owner, factory)[SoloStudyingViewModel::class]
            val features = listOf(
                app.statusViewModel, app.dungeonViewModel, app.battleViewModel, app.skillViewModel,
                app.shopViewModel, app.tutorialViewModel, app.breakViewModel, app.dailyQuestViewModel,
                app.focusCycleViewModel, app.focusProfileViewModel,
            )
            try {
                val retained = ViewModelProvider.create(owner, factory)[SoloStudyingViewModel::class]
                assertSame(app, retained)
                assertTrue(features.all { it.viewModelScope.isActive })
                owner.clear()
                assertTrue(
                    "A feature scope outlived the Activity owner",
                    features.none { it.viewModelScope.isActive },
                )
                assertEquals(paused, sessions.read())
            } finally {
                features.forEach { it.viewModelScope.cancel() }
            }
        } finally {
            owner.clear()
            sessions.clear()
            database.close()
            Dispatchers.resetMain()
        }
    }
}
