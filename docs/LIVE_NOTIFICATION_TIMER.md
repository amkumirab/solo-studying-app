# Live focus notification

The active focus notification displays a large countdown in both its collapsed
and expanded layouts. Pause freezes the remaining duration; Resume restarts the
countdown from the saved duration. Existing Pause/Resume, Finish and open-session
actions remain available. Notification permission is still required.

The previous body included a formatted duration captured when the notification
was posted. Only the small header clock was live. The main content now uses a
native Chronometer, driven by SystemUI and anchored to elapsed realtime. There
is no per-second notification posting, new service, dependency or permission.
Completion scheduling, session accounting, storage and sound policy are unchanged.

DecoratedCustomViewStyle retains system controls and decorations. The compact
two-line layout uses adaptive notification text styles rather than fixed colors.
Paused time is a separate static text view so rebinding or reopening the shade
cannot accidentally start it. The native header countdown remains a fallback.

References: [RemoteViews clock API](https://developer.android.com/reference/android/widget/RemoteViews#setChronometer(int,long,java.lang.String,boolean)),
[custom notification guidance](https://developer.android.com/develop/ui/views/notifications/custom-notification).

## Verification

- Regression tests inspect the posted collapsed/expanded content, countdown
  direction and elapsed-realtime base on API 36 and minimum API 24. They also
  cover durations over an hour and paused-to-running RemoteViews reuse.
- On the dedicated API 36 AVD, the packaged E2E app's expanded clock changed
  from 23:17 to 23:04 without touching controls. Pause held at 22:54 across
  captures; Resume restarted it. The collapsed clock changed 22:33 to 22:18.
- On the dedicated API 37 AVD, the expanded clock changed 24:40 to 24:20.
  Pause held at 24:06 across captures; Resume restarted it, with 23:34 visible
  in the stable collapsed layout. Both tests used real app UI and notification
  permission approval, not seeded timer state. Screenshots are retained locally
  under app/build/reports/notification-inspect36e and notification-inspect37b.
- API 37 marks the framework notification-view inspection methods used by these
  Robolectric tests deprecated. Those test-only compiler advisories remain
  visible; no production deprecated API or suppression was added.

## Phone check

The normal debug update APK is
`app/build/outputs/apk/debug/solo-studying-live-timer-20261112-debug.apk`
(version 1.0, application ID `com.amkumirab.solostudying`).
SHA-256: `dbd2db43df319297e4308571ae53d54f7445cf65360a1e2ca2c15903b7c8a74f`.
It retains the existing debug signing certificate and is not a store release.

Install the update over the existing app without uninstalling or clearing data.
Start a focus timer, return Home and open the notification shade. Watch the large
number decrease for at least ten seconds without touching a control, in both
collapsed and expanded layouts. Pause, wait and reopen the shade: the number
must stay fixed. Resume and verify it decreases again. Lock/unlock the phone and
compare the remaining time with the in-app timer. OEM lock-screen presentation
and physical-device power restrictions still need this phone check.

## Build checks

JDK 21 and the repository Gradle wrapper passed all 605 JVM tests across 123
suites with zero failures, errors or skipped tests. Debug, E2E, unsigned Release
and instrumentation APKs built. Lint passed with zero errors and one retained
UseKtx advisory. The existing instrumentation suite and full planner acceptance
journey were not rerun for this rendering-only change; the changed notification
was exercised directly in the packaged E2E app on both AVDs.

Two full runs exposed an existing navigation-test readiness issue; a separate
class-only run passed. That test checked a StateFlow value without synchronizing
with the composed UI. It now waits for the home semantics node as well, retaining
the exact 12-goal check, original ten-second timeout and all navigation assertions.
The subsequent complete suite passed. No production navigation code changed.
Reference: [Compose test synchronization](https://developer.android.com/develop/ui/compose/testing/synchronization).
