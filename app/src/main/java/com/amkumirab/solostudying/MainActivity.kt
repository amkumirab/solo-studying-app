package com.amkumirab.solostudying

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.notification.NotificationHelper
import com.amkumirab.solostudying.notification.FocusSessionNotifier
import com.amkumirab.solostudying.focus.FocusSessionStore
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.sound.RpgSoundManager
import com.amkumirab.solostudying.ui.screens.MainAppScreen
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import com.amkumirab.solostudying.ui.viewmodel.SoloStudyingViewModel
import com.amkumirab.solostudying.ui.viewmodel.SoloStudyingViewModelFactory

class MainActivity : ComponentActivity() {
  private lateinit var viewModel: SoloStudyingViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    RpgSoundManager.initialize(applicationContext)

    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        NOTIFICATION_PERMISSION_REQUEST,
      )
    }

    NotificationHelper.createNotificationChannel(applicationContext)
    FocusSessionNotifier.createChannel(applicationContext)
    NotificationHelper.scheduleDailyAlarms(applicationContext)

    prepareFocusAction(intent)

    val database = SoloStudyingDatabase.getDatabase(applicationContext)
    val repository = SoloStudyingRepository(database)
    viewModel = ViewModelProvider(
      this,
      SoloStudyingViewModelFactory(repository, applicationContext)
    )[SoloStudyingViewModel::class.java]
    if (intent?.action == ACTION_OPEN_FOCUS_SESSION) {
      viewModel.requestFocusScreen()
    }

    setContent {
      SoloStudyingTheme {
        MainAppScreen(viewModel = viewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (prepareFocusAction(intent) && ::viewModel.isInitialized) {
      viewModel.processPendingFocusAction()
    }
    if (intent.action == ACTION_OPEN_FOCUS_SESSION && ::viewModel.isInitialized) {
      viewModel.requestFocusScreen()
    }
  }

  private fun prepareFocusAction(intent: Intent?): Boolean {
    if (intent?.action != ACTION_FINISH_FOCUS_SESSION) return false
    intent.action = ACTION_OPEN_FOCUS_SESSION
    return FocusSessionStore(applicationContext).requestFinish()
  }

  companion object {
    const val ACTION_OPEN_FOCUS_SESSION = "com.amkumirab.solostudying.ACTION_OPEN_FOCUS_SESSION"
    const val ACTION_FINISH_FOCUS_SESSION = "com.amkumirab.solostudying.ACTION_FINISH_FOCUS_SESSION"
    const val NOTIFICATION_PERMISSION_REQUEST = 101
  }
}
