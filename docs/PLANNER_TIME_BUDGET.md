# Planner time budget

## Objective and contract

Help students check whether remaining course effort fits their study days before
saving an offline exam plan. Course deadlines include that day's study time.
The daily budget is shared by all courses, not repeated for each course.

- Show total remaining effort, available study days and available time.
- Recommend the minimum **uniform** daily budget that meets every deadline:
  for each deadline, divide cumulative effort due by the study days up to that
  date, round up to a whole minute, and take the largest result.
- If a deadline has no available study days, show that it cannot be met with
  these weekdays. Do not divide by zero or suggest a misleading daily average.
- Compare capacity against cumulative effort at every deadline. A positive final
  balance does not hide a shortage at an earlier deadline.
- Adjust each non-rest weekday by the same signed multiple of 15 minutes.
  Preserve rest days; reject changes outside 1 to 480 minutes on study days.
  Show the actual accumulated change through the final deadline.
- Keep adjustments as a draft. Rebuild the preview and save the displayed
  scenario only through the existing Save plan action and shortage acknowledgement.
- Offer the required uniform daily budget when it fits the existing daily limit.
  Reset the comparison when the input answers change; restore the draft on rotation.

## Implementation

Kotlin with the existing Compose Material 3 theme; no new dependencies, network,
permissions, database migrations, planner storage formats or study-credit changes.
Pure calculations live in `domain/planner/PlannerTimeBudget.kt`. The preview
section lives in `ui/screens/PlannerTimeBudgetCard.kt`, with its state owned by
`StudyPlannerWizard`. Use existing theme tokens, wrapping text, labelled buttons
at least 48 dp tall and a polite status announcement. Never change saved data
merely by exploring a scenario.

Style example: `val balanceMinutes = availableMinutes - requiredMinutes`.
Minute values are integers; scheduling remains on the existing background dispatcher.

## Verification

JUnit tests cover early and shared deadlines, rest days, one-day and zero-day
windows, uneven weekday budgets, large inputs, rounding, exact 15-minute changes
and daily boundaries. Compose/Robolectric tests cover adjustment, saving the
displayed plan, restoring state, shortage acknowledgement and busy controls.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest --tests '*PlannerTimeBudgetTest' --no-daemon
.\gradlew.bat testDebugUnitTest --tests '*StudyPlannerUiTest' --no-daemon
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon
```

Always preserve existing tests and recorded study. Ask first before changing
dependencies, storage or CI. Never commit secrets, build artifacts or skip tests.
Device visual review is separate from automated Compose testing.

## Manual check

Open Study > Plan, create or edit a weekly plan, enter course effort and finish
dates, choose weekday budgets and continue to Preview plan. Explore the daily
adjustment, compare each deadline and save only when satisfied. Reopen the plan
to check that the selected budgets were saved. Cancelling must preserve the old plan.

## Local verification, 2026-10-14

The complete test/build command succeeded: 251 tests passed with no failures,
errors or skips. Debug and unsigned Release APKs built successfully. Lint reports
0 errors and 34 existing warnings. The domain tests include 200 generated cases
checking that the uniform recommendation fits and one minute less misses a deadline.
UI tests cover scenario restoration, safe saving, reset, shortage acknowledgement,
editing answers, busy controls and daily limits. Native text-layout assertions
verify complete, non-ellipsized text within painted bounds at 1.5 font scale on a
288 dp card inside a 320 dp screen.

Independent review found no required changes. The budget controls use full-width
buttons for readable labels at larger font sizes. No Android device was attached;
physical-device visual review remains pending.
