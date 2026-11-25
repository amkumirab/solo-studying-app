package com.amkumirab.solostudying

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.widget.TodayWidgetProvider
import com.amkumirab.solostudying.widget.WeeklyStreakWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
  fun `launcher icons use separate color and themed artwork`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals(R.mipmap.ic_launcher, context.applicationInfo.icon)
    for (icon in listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)) {
      val drawable = context.getDrawable(icon) as AdaptiveIconDrawable
      assertTrue(drawable.foreground is BitmapDrawable)
      assertTrue(drawable.monochrome is VectorDrawable)
    }
  }

  @Test
  fun `the launcher activity resolves the branded native splash`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val activity = context.packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0)
    assertEquals(R.style.Theme_SoloStudying_Launcher, activity.theme)
    val theme = context.resources.newTheme().apply { applyStyle(activity.theme, true) }
    val splash = theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowSplashScreenBackground))
    try {
      assertEquals(context.getColor(R.color.brand_navy), splash.getColor(0, 0))
    } finally {
      splash.recycle()
    }
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

  @Test
  fun `today widget is registered with provider metadata`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val receiver = context.packageManager.getReceiverInfo(
      ComponentName(context, TodayWidgetProvider::class.java),
      PackageManager.GET_META_DATA,
    )

    assertFalse(receiver.exported)
    assertEquals(
      R.xml.today_widget_info,
      receiver.metaData.getInt("android.appwidget.provider"),
    )
  }

  @Test
  fun `weekly streak widget is registered with provider metadata`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val receiver = context.packageManager.getReceiverInfo(
      ComponentName(context, WeeklyStreakWidgetProvider::class.java),
      PackageManager.GET_META_DATA,
    )

    assertFalse(receiver.exported)
    assertEquals(
      R.xml.weekly_streak_widget_info,
      receiver.metaData.getInt("android.appwidget.provider"),
    )
  }

  @Test
  fun `weekly streak companion is a compact transparent asset`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val mascotBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.streak_companion)
    val pngBytes = context.resources.openRawResource(R.drawable.streak_companion).use { it.readBytes() }

    assertEquals(512, mascotBitmap.width)
    assertEquals(512, mascotBitmap.height)
    assertEquals(6, pngBytes[25].toInt())
  }
}
