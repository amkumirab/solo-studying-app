package com.amkumirab.solostudying

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationContextTest {
  @Test
  fun applicationIdMatchesReleaseNamespace() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("com.amkumirab.solostudying", appContext.packageName)
  }
}
