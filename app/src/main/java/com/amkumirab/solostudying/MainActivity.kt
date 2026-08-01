package com.amkumirab.solostudying

import android.Manifest
import android.content.pm.PackageManager
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
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.ui.screens.MainAppScreen
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import com.amkumirab.solostudying.ui.viewmodel.SoloStudyingViewModel
import com.amkumirab.solostudying.ui.viewmodel.SoloStudyingViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

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
    NotificationHelper.scheduleDailyAlarms(applicationContext)

    val database = SoloStudyingDatabase.getDatabase(applicationContext)
    val repository = SoloStudyingRepository(database.soloStudyingDao())
    val viewModel = ViewModelProvider(
      this,
      SoloStudyingViewModelFactory(repository, applicationContext)
    )[SoloStudyingViewModel::class.java]

    setContent {
      SoloStudyingTheme {
        MainAppScreen(viewModel = viewModel)
      }
    }
  }

  private companion object {
    const val NOTIFICATION_PERMISSION_REQUEST = 101
  }
}
