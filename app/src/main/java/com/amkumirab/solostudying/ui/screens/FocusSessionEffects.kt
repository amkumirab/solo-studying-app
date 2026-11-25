package com.amkumirab.solostudying.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amkumirab.solostudying.focus.StrictFocusPinning
import com.amkumirab.solostudying.ui.viewmodel.SoloStudyingViewModel
import com.amkumirab.solostudying.widget.TodayWidgetProvider
import com.amkumirab.solostudying.widget.WeeklyStreakWidgetProvider

@Composable
internal fun FocusSessionLifecycleEffect(viewModel: SoloStudyingViewModel) {
    val view = LocalView.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val shouldKeepScreenOn = viewModel.isBattleActive && !viewModel.isBattlePaused

    DisposableEffect(view, shouldKeepScreenOn) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = shouldKeepScreenOn
        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateStrictFocusPinningState(StrictFocusPinning.isPinned(context))
                viewModel.syncFocusSessionTime()
                viewModel.refreshFocusShield()
                viewModel.syncBreakTime()
                viewModel.refreshDailyQuests()
                TodayWidgetProvider.requestUpdate(context)
                WeeklyStreakWidgetProvider.requestUpdate(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
internal fun StrictFocusPinningEffect(
    activity: Activity?,
    viewModel: SoloStudyingViewModel,
) {
    val shouldPin = viewModel.strictFocusRequested &&
        viewModel.isBattleActive &&
        !viewModel.isBattlePaused

    LaunchedEffect(activity, shouldPin) {
        val hostActivity = activity ?: return@LaunchedEffect
        if (shouldPin) {
            if (StrictFocusPinning.start(hostActivity)) {
                viewModel.markStrictFocusStartAttempted()
                if (StrictFocusPinning.isPinned(hostActivity)) {
                    viewModel.updateStrictFocusPinningState(true)
                }
            }
        } else if (viewModel.strictFocusOwnsPinning) {
            val stopped = StrictFocusPinning.stop(hostActivity)
            val remainsPinned = StrictFocusPinning.isPinned(hostActivity)
            if (stopped && !remainsPinned) {
                viewModel.markStrictFocusReleased()
            } else {
                viewModel.updateStrictFocusPinningState(remainsPinned)
            }
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
