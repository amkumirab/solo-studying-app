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
