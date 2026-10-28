package com.amkumirab.solostudying.notification

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amkumirab.solostudying.MainActivity
import com.amkumirab.solostudying.ui.screens.TimerAlarmScreen
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme

class TimerAlarmActivity : ComponentActivity() {
    private var token by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TimerAlarmAlerts.restore(applicationContext)
        token = intent.data?.lastPathSegment
        if (TimerAlarmAlerts.events.value?.token != token || token == null) { finish(); return }
        showOverLockScreen()
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = dismiss()
        })
        setContent {
            val event by TimerAlarmAlerts.events.collectAsStateWithLifecycle()
            LaunchedEffect(event, token) { if (event == null || event?.token != token) finish() }
            event?.takeIf { it.token == token }?.let { current ->
                SoloStudyingTheme { TimerAlarmScreen(current.timer == CompletedTimer.BREAK, current.isTest,
                    onDismiss = ::dismiss, onOpenApp = {
                        dismiss()
                        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                    }) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        token = intent.data?.lastPathSegment
        TimerAlarmAlerts.restore(applicationContext)
        if (TimerAlarmAlerts.events.value?.token != token || token == null) finish()
    }

    private fun dismiss() {
        token?.let { TimerAlarmAlerts.dismiss(applicationContext, it) }
        finish()
    }

    @Suppress("DEPRECATION") // Screen-on window flags are required on Android 7–8.0.
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
