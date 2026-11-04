# Contextual planner guidance

## Objective and boundaries

Add offline, read-only recommendations inside the existing weekly planner and
its proposed-plan comparison. Use real accepted/proposed values, not a chat bot,
invented statistics or automatic plan changes. Reuse the current Material theme,
navigation, domain accounting and explicit save/compare-and-swap paths.

Show at most three messages: the most urgent unfinished course, one highest-
priority course when it adds different information, and a weekly status. A near
deadline means today through the next 14 days, inclusive. Past deadlines are
labelled overdue, not described as upcoming exams. Finish-by dates are planning
dates, not necessarily exams. Course advice uses exact remaining seconds after
the accepted baseline and ledger allocation; round a displayed deficit up once.
Past unstudied slots do not cover future work. Completed/deleted goals never
receive positive allocation advice. Missing goals instead get a repair message.

Weekly messages describe planned time, not actual study or guaranteed spare
clock slots. Do not recommend adding study for a zero/unset goal or outside-plan
week. Keep selected-week amounts separate from all-date deadline coverage.
Highest priority is from the displayed plan, not unsaved subject settings.
The current week still shows expired-date repair advice when every plan date has
passed; it does not invent a weekly goal or allocation outside accepted dates.

Actions only navigate to existing Availability, Subjects, Edit answers or the
read-only rebuild review. Never write, start study, unlock, reschedule or apply a
proposal on an advice click. Disable and guard callbacks while busy/active.
Draft advice is clearly labelled and read-only, without editor navigation.
Unreadable settings retain existing recovery/error routes, not optimistic advice.

## Implementation and style

Pure typed advice lives in `domain/planner/PlannerGuidance.kt`; render data and
callbacks through a focused `ui/screens/PlannerGuidanceContent.kt` organism.
Tests use matching JUnit/Compose/Robolectric packages. Example style:

```kotlin
val advice = plannerGuidance(plan, bosses, today, weekStart)
```

No dependencies, network, permissions, schemas, timer/reward behavior or
scheduling-policy changes. Existing exact-minute durations and locks stay intact.
Do not weaken tests or suppress checks. Keep all public text English.

The UI is an organism reusing existing Material text, buttons, outlined cards and
theme tokens, not a new design system. Place it inside the selected calendar week
and proposed-plan review, not on the home screen. Show the first message by
default with a saveable disclosure for the rest. Stack text/actions with wrapping
labels, no decorative imagery, truncated content or color-only states. All
actions have at least 48 dp height. No new animation is needed.

Compose BOM 2024.09.00 and Kotlin 2.2.10 remain unchanged. Key derived calculations
by all their inputs and keep callback events separate from values:
[Compose state](https://developer.android.com/develop/ui/compose/state).
The disclosure stores only a Boolean, not plans or pending actions:
[saving UI state](https://developer.android.com/develop/ui/compose/state-saving).

## Verification and delivery

1. Test exact deadline gaps, progress baselines, missed/locked slots, zero targets,
   missing/completed courses, priorities, selected weeks and deterministic bounds.
2. Test real entry navigation, no writes/study starts, draft labelling, reactive
   inputs, restoration and guarded callbacks. Check keyboard and 48 dp actions.
3. Native-render narrow/large-font and tablet layouts; inspect generated images.
   Run the full suite, lint and both APK builds. Keep three focused commits local
   until publication is requested; deliver a named test APK without clearing data.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:GRADLE_USER_HOME = 'C:\Users\amira\.gradle'
.\gradlew.bat testDebugUnitTest --tests '*PlannerGuidance*' --no-daemon --max-workers=1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1
```

Physical upgrade, TalkBack, rotation and real-phone performance remain manual
unless tested on an attached device. This is not a store release.

## Phone smoke test

Install the delivered Debug APK over the existing app; do not uninstall or clear
data. Open Study > Plan > Open weekly plan > Schedule. Scroll below the weekly
overview to Planner guidance. Only the first message is initially expanded.

- For a near finish-by date with uncovered work, check the remaining amount,
  open Review remaining plan, then Cancel. Goal progress/history must not change.
- Expand more guidance. A budget shortage should lead to Availability; a weekly
  gap to Edit plan answers; a priority allocation to Subjects. Merely opening and
  leaving these editors must not change the accepted plan.
- Navigate weeks and check that weekly amounts change, while deadline messages
  explicitly use today's date. Compare a proposed plan and confirm its guidance
  is labelled Proposed and has no action that changes settings.
- Record study for a course or complete it; obsolete remaining-work advice should
  refresh. Start a real timer and check that advice cannot launch another action
  while study is active. Finish/cancel the real timer normally.
- Increase system font size, rotate, reopen, and use TalkBack to reach the heading,
  disclosure and actions. All text must wrap and the controls remain reachable.

## Verification record, 2026-11-04

- Full `testDebugUnitTest lintDebug assembleDebug assembleRelease` succeeded in
  6 minutes 12 seconds: 593 tests in 119 suites, zero failures, errors or skips.
  The feature adds 20 tests: ten domain, four UI, three real-entry and three native
  layout cases. No existing test, assertion, timeout or check was weakened.
- The first domain/UI red runs failed on missing APIs. A follow-up red test caught
  overdue guidance disappearing when all plan dates were before the current week.
  Current-week repair advice now remains visible without inventing outside-plan
  weekly metrics. That regression and the full suite pass.
- lintDebug: zero errors, the same 38 pre-existing warnings, none in changed
  feature files. Debug and unsigned Release built. No dependency, manifest,
  schema, timer/reward, CI or scheduling-policy changes.
- Native 320 dp/1.5-font-scale, 800 dp tablet and 360 dp-high viewport tests passed.
  Four generated PNGs in `app/build/reports/planner-guidance/` were visually
  inspected. Text wraps completely and actions have at least 48 dp height.
  The dark area below the short viewport is the host window, not app content.
- Scoped review checked accounting, determinism, bounded advice, accepted/draft
  separation, guarded no-write navigation and local-storage compatibility.
  This establishes automated evidence, not physical-phone certification.
- No phone/emulator was attached. Upgrade retention, rotation, TalkBack, real-phone
  performance and system reduced-motion acceptance remain manual checks.
  Three focused local commits are prepared; no push, store release or APK upload
  is part of this implementation request.

Test package: `app/build/outputs/apk/debug/solo-studying-planner-guidance-20261104-debug.apk`
(20,588,383 bytes, ignored build output). SHA-256:
`35a96170934137ceb916b89505159901c573ba4af261d1c4cd7dfd52c4cc4be8`.
Signature scheme v2 verified. Application ID `com.amkumirab.solostudying`, development
version 1.0/code 1, min SDK 24, target SDK 36. The Debug certificate matches the
previous comparison package:
`dc57434a4949a016ea3cddfc1e184412d530add37b926c48d73aaff25c62b824`.
