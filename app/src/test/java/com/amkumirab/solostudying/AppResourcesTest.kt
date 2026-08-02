package com.amkumirab.solostudying

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppResourcesTest {

  @Test
  fun `application name is available from resources`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Solo Studying", appName)
  }

  @Test
  fun `interface sound effects are packaged`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sounds = listOf(
      R.raw.battle_start,
      R.raw.boss_defeated,
      R.raw.gold_spend,
      R.raw.level_up,
      R.raw.session_complete,
      R.raw.shop_purchase,
      R.raw.skill_unlock,
      R.raw.study_pause,
      R.raw.study_resume,
      R.raw.system_click,
      R.raw.system_warning,
    )

    assertEquals(11, sounds.distinct().size)
    sounds.forEach { resourceId ->
      context.resources.openRawResource(resourceId).use { stream ->
        assertTrue("Sound resource is empty: $resourceId", stream.read() >= 0)
      }
    }
  }
}
