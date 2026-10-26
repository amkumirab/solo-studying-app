# Timer completion alerts

Focus and break timers share a bounded, non-repeating completion cue: two short
vibration pulses followed by a longer pulse. Normal ringer mode also plays the
existing `session_complete` asset using notification audio usage, the phone's
notification volume and the app's enabled/volume settings. Silent/vibrate modes
never play this chime. Vibration is independent of the app's sound toggle.

Do not change ringer mode, volume, notification policy or DND access. When DND
is not ALL, suppress both cues. Restore only the app-owned Focus Shield policy
through the existing completion path before attempting feedback. No DND bypass
flags, repeating alarm, exact-alarm permission or foreground service is added.

Persist only the most recent completion timestamp for each timer kind. Foreground
and alarm-receiver paths claim the same event before feedback, so reopening or
duplicate delivery does not ring again. Early finish, pause, skip and a premature
break broadcast do not claim completion. Notification cards are silent to avoid
an additional generic notification sound. Timer feedback does not depend on the
optional notification permission; the card itself still requires permission.

Add only the normal VIBRATE manifest permission. Use alarm vibration attributes
for user-started timers, including background delivery; select the supported API
on Android 7, Android 8–12, and Android 13+. A missing motor or playback failure
must not interrupt timer/reward completion. Release the short player on finish,
error or a five-second watchdog.

No schedule, reward formula, study-credit or database schema changes. Existing
inexact alarms and Android battery/force-stop restrictions still apply; this is
not a promise of exact-time delivery under every device power policy.

References: [Vibrator API](https://developer.android.com/reference/android/os/Vibrator),
[vibration usage](https://developer.android.com/reference/android/os/VibrationAttributes#USAGE_ALARM).

## Phone checks

Install over the current app. Let a short focus timer and a one-minute break
expire, once in the foreground and once with the screen locked. Normal mode
should give the chime and pulse pattern; silent/vibrate modes only the pattern.
Try app sound disabled and volume zero: vibration should remain. In DND, neither
cue should run. Reopen after an alert and verify no second cue. Pause/skip/end
early and verify no completion cue. Repeat with notification permission denied:
the cue remains available but no notification card should appear. Do not
uninstall or clear app data to test an upgrade. Actual vibration strength and
background delivery require a real device, not just Robolectric.

## Verified locally, 2026-10-26

`testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1`
passed through the checked-in Gradle wrapper with JDK 21. All 458 tests passed,
with zero failures, errors or skips. Debug and unsigned Release APKs built.
Coverage includes Android 7/8 compatibility, normal/silent/vibrate modes, app
mute/volume, DND, duplicate broadcasts, foreground expiry, denied notification
permission, skipped/early completion and player completion/error/watchdog cleanup.

lintDebug has zero errors and 36 warnings: 35 existing warnings and one UseKtx
style recommendation in the completion claim. The native editor commit result
is deliberately checked; the KTX edit helper does not return that Boolean.
The advisory remains visible, not suppressed. Tests and lint checks were not
disabled or weakened. A missing raw-media fixture in Robolectric was corrected
by supplying clip duration metadata, not by changing playback assertions.

Natural focus expiry now uses the common timer chime rather than the old
victory/level-up sound call; reward formulas and progression data are unchanged.
Manual conquest and other progression events retain their existing effects.
Review covered bounded resources, application-only context, DND restoration
order, persistent duplicate claims and silent notification cards. No connected
device was available; physical vibration, locked-screen timing and upgrade tests
remain manual. This commit is local until publication is requested.
