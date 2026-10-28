# Alarm-style timer completion

## Contract

An optional Profile setting gives user-started focus/break timers a clear completion
screen and a finite vibration pattern, stopped by Dismiss or a 30-second timeout.
Silent/vibrate modes never play audio. Existing app sound settings still apply in
normal mode. Do Not Disturb is never overridden. Default remains the short cue.

Use a dedicated silent high-importance channel, a private non-exported completion
activity and explicit immutable pending intents. No course names appear over the
lock screen. Never dismiss the secure keyguard or change system volume/policy.
On Android 14+, request full-screen access only through a user-selected Settings
action. Denial must retain the notification fallback and foreground completion UI.
The system decides whether a full-screen activity or heads-up card appears.

The active alert is persisted with an opaque ID and expiration. Duplicate timer
delivery cannot replay it. Old buttons/timeouts cannot cancel a newer alert.
Restoring a screen does not restart feedback, extend expiry or claim rewards.
Old completions do not launch intrusive screens. Preview does not modify study,
break, reward or timer completion records.

Alarm mode uses exact-and-idle scheduling for timer completions only when the
user has granted Alarms & reminders access. Never use USE_EXACT_ALARM or change
that grant automatically. Without access, use the existing inexact scheduling;
daily reminders always keep their existing behavior. The permission-grant
broadcast rechecks access and reschedules stored timers through existing paths.
Device power policies can still restrict delivery; do not promise exact timing
or wake-up on every phone. Physical lock-screen,
silent-mode, rotation and upgrade tests remain required before release.

## Phone smoke test

1. Install the new Debug APK over the current installation. Do not uninstall or
   clear data. Existing study records, goals, planner and sound settings stay put.
2. Open the header's Settings button and enable Timer completion alarm. Grant
   notifications, lock-screen alerts and precise timer access if Android asks.
   Review Notification settings if the timer channel is disabled or made quiet.
3. Put the phone in silent mode (not Do Not Disturb) and choose Test alert now.
   Expect a clearly labelled preview, no sound, and the distinctive pulse pattern.
   Dismiss must stop it immediately. Repeat without dismissing: stop within 30s.
4. Let a short real focus timer and a one-minute break expire, once in the app and
   once with the screen locked. Check the completion screen/banner, generic text,
   no duplicated alerts and unchanged study rewards. Open app must not bypass PIN.
5. Repeat with full-screen access and notifications denied, Do Not Disturb on,
   a rotated screen, and app sound disabled. Read each fallback explanation.
6. Disable the option: the original short completion cue must still work.

## Local verification

On 2026-10-27, `testDebugUnitTest lintDebug assembleDebug assembleRelease` passed:
492 tests, zero failures, errors or skipped tests. Debug and unsigned Release
builds succeeded. Lint has zero errors and 38 warnings: 36 existing advisories
plus two UseKtx suggestions for checked native SharedPreferences commit results.
Those Boolean results must not be discarded; no lint suppressions were added.

The delivered Debug package uses `com.amkumirab.solostudying`, version 1.0 (code 1),
min SDK 24 and target SDK 36, with verified APK signature scheme v2. The file is
`solo-studying-timer-alarm-20261027-debug.apk` and its SHA-256 is:

```text
61F8F417C6C7E5F2C537961CE2E238EC3679BA58234298C7021C47F42CFD3CD1
```

No phone/emulator was available. Real screen wake, motor output, device battery
policy, secure-keyguard behavior and upgrade retention still need the phone test
above. This is a local test package, not a store release.

## Platform references

- [Full-screen intent limits](https://source.android.com/docs/core/permissions/fsi-limits)
- [Urgent notifications](https://developer.android.com/develop/ui/views/notifications/build-notification#urgent-message)
- [Activity screen-on behavior](https://developer.android.com/reference/android/app/Activity#setTurnScreenOn(boolean))
- [Exact alarm access and fallback](https://developer.android.com/develop/background-work/services/alarms)
