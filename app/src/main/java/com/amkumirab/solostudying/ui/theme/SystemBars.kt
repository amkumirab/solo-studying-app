package com.amkumirab.solostudying.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb

internal fun ComponentActivity.enableSoloStudyingEdgeToEdge() {
    // The app is always dark, even when Android uses a light system theme.
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(DarkFantasySurface.toArgb()),
    )
}
