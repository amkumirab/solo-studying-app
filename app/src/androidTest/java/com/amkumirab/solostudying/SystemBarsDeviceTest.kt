package com.amkumirab.solostudying

import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemBarsDeviceTest {
    @Test fun mainActivityKeepsLightSystemIconsAfterRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            fun assertLightIcons() = scenario.onActivity { activity ->
                val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                assertFalse("Dark app needs light status-bar icons", controller.isAppearanceLightStatusBars)
                assertFalse("Dark app needs light navigation-bar icons", controller.isAppearanceLightNavigationBars)
            }
            assertLightIcons()
            scenario.recreate()
            assertLightIcons()
        }
    }
}
