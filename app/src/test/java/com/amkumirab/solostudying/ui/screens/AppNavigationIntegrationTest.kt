package com.amkumirab.solostudying.ui.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import com.amkumirab.solostudying.ui.viewmodel.SoloStudyingViewModel
import com.amkumirab.solostudying.ui.viewmodel.SoloStudyingViewModelFactory
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class AppNavigationIntegrationTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var database: SoloStudyingDatabase
    private lateinit var viewModel: SoloStudyingViewModel

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SoloStudyingDatabase::class.java).allowMainThreadQueries().build()
        runBlocking { database.soloStudyingDao().insertOrUpdateProfile(UserProfileEntity(hasCompletedOnboarding = true, hasCompletedTutorial = true)) }
        viewModel = SoloStudyingViewModelFactory(SoloStudyingRepository(database), context).create(SoloStudyingViewModel::class.java)
    }

    @After fun cleanup() {
        if (::viewModel.isInitialized) listOf(
            viewModel, viewModel.statusViewModel, viewModel.dungeonViewModel, viewModel.battleViewModel,
            viewModel.skillViewModel, viewModel.shopViewModel, viewModel.tutorialViewModel,
            viewModel.breakViewModel, viewModel.dailyQuestViewModel, viewModel.focusCycleViewModel,
            viewModel.focusProfileViewModel,
        ).forEach { it.viewModelScope.cancel() }
        if (::database.isInitialized) database.close()
    }

    @Test fun `home study weekly plan settings and profile remain independently reachable`() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme { MainAppScreen(viewModel) } }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("daily_tasks_toggle").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("bottom_nav_today").assertIsSelected()
        rule.onNodeWithTag("study_hub_sections").assertDoesNotExist()
        rule.onNodeWithTag("daily_tasks_toggle").performScrollTo().performClick()
        rule.onNodeWithTag("daily_quest_board").assertExists()
        rule.onNodeWithTag("bottom_nav_dungeons").performClick()
        rule.onNodeWithTag("study_hub_goals").assertIsSelected()
        rule.onNodeWithTag("gate_status_toggle").performScrollTo().performClick()
        rule.onNodeWithText("No active penalty. Keep studying on your scheduled days to maintain your streak.").assertExists()
        rule.onNodeWithTag("study_hub_plan").performScrollTo()
        rule.onNodeWithTag("study_hub_plan").performClick()
        rule.onNodeWithText("Weekly study plan").assertExists()
        rule.onNodeWithText("Planned courses today").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("bottom_nav_dungeons").assertIsSelected()
        rule.onNodeWithTag("study_hub_plan").assertIsSelected()
        rule.onNodeWithTag("bottom_nav_shop").performClick()
        rule.onNodeWithTag("create_reward_button").assertExists()
        rule.onNodeWithTag("bottom_nav_dungeons").performClick()
        rule.onNodeWithTag("study_hub_plan").assertIsSelected()
        rule.onNodeWithTag("open_app_settings").performClick()
        rule.onAllNodesWithText("Settings").assertCountEquals(1)
        rule.onNodeWithTag("back_to_profile").assertDoesNotExist()
        rule.onNodeWithTag("app_header_back").assertExists().performClick()
        rule.onNodeWithTag("profile_overview").assertIsSelected()
        rule.onNodeWithTag("bottom_nav_today").performClick()
        rule.onNodeWithTag("daily_quest_board").assertExists()
    }

    @Test fun `leaving session keeps timer active and return bar restores paused session`() {
        rule.setContent { SoloStudyingTheme { MainAppScreen(viewModel) } }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("daily_tasks_toggle").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { viewModel.battleViewModel.selectAndStartFreeStudy(25) }
        rule.waitUntil(10000) { viewModel.isBattleActive }
        rule.onNodeWithTag("bottom_nav_today").performClick()
        rule.onNodeWithTag("focus_navigation_warning_dialog").assertExists()
        rule.onNodeWithTag("keep_focusing_button").performClick()
        rule.onNodeWithTag("return_to_session").assertDoesNotExist()
        rule.onNodeWithTag("bottom_nav_today").performClick()
        rule.onNodeWithTag("confirm_leave_focus_screen_button").performClick()
        rule.onNodeWithText("Session in progress").assertExists()
        rule.runOnIdle {
            assertTrue(viewModel.isBattleActive)
            viewModel.battleViewModel.pauseBattle()
        }
        rule.onNodeWithText("Session paused").assertExists()
        val remaining = viewModel.battleTimeLeftSeconds
        rule.onNodeWithTag("return_to_session").performClick()
        rule.onNodeWithTag("return_to_session").assertDoesNotExist()
        rule.runOnIdle {
            assertTrue(viewModel.isBattleActive && viewModel.isBattlePaused)
            assertEquals(remaining, viewModel.battleTimeLeftSeconds)
        }
    }

    @Test fun `goal creation remains visible at large font size`() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                SoloStudyingTheme { MainAppScreen(viewModel) }
            }
        }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("daily_tasks_toggle").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("bottom_nav_dungeons").performClick()
        rule.onNodeWithTag("create_boss_button").assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test fun `Study restores each section scroll and expanded recovery details`() {
        runBlocking {
            repeat(12) { index ->
                database.soloStudyingDao().insertBoss(BossEntity(
                    name = "Course ${index + 1}", difficulty = "Medium", requiredMinutes = 600,
                    createdAt = index.toLong(),
                ))
            }
        }
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme { MainAppScreen(viewModel) } }
        rule.waitUntil(10000) { viewModel.bosses.value.size == 12 }
        rule.onNodeWithTag("bottom_nav_dungeons").performClick()
        rule.onNodeWithTag("gate_status_toggle").performScrollTo().performClick()
        rule.onNodeWithTag("study_hub_focus").performScrollTo().performClick()
        rule.onNodeWithTag("study_hub_goals").performScrollTo().performClick()
        rule.onNodeWithText("No active penalty. Keep studying on your scheduled days to maintain your streak.").assertExists()

        rule.onNodeWithText("COURSE 1").performScrollTo()
        fun offset() = rule.onNode(hasScrollAction() and hasAnyDescendant(hasText("COURSE 1")))
            .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        val goalsOffset = offset()
        assertTrue(goalsOffset > 0f)
        // Invoke section navigation directly so the test does not scroll back to the header first.
        rule.onNodeWithTag("study_hub_focus").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithTag("study_hub_plan").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithTag("study_hub_goals").performSemanticsAction(SemanticsActions.OnClick) { it() }
        assertEquals(goalsOffset, offset(), 0f)
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(goalsOffset, offset(), 0f)
        rule.onNodeWithText("No active penalty. Keep studying on your scheduled days to maintain your streak.").assertExists()
    }
}
