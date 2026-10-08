# Weekly plans in Today

## Objective and contract

Show accepted weekly sessions inside the existing Today dashboard, with remaining
time, recorded progress and direct bounded timer launch. Keep weekly editing
available without adding another dashboard card. Course recommendations already
managed by the weekly plan are hidden to avoid duplicate starts.

"No time today" previews moving only today's unfinished, available course blocks
to later study days. Earlier deadlines are considered first. Existing future
blocks, block IDs, progress baselines, history and rewards remain unchanged.
Partly studied blocks keep their credit; their full slot reserves capacity.
Never exceed daily capacity or a deadline. If any block cannot move, show the
shortage and do not apply a partial change. Deleted courses are reported separately.
Cancel changes nothing. Saving requires confirmation and a still-current preview;
an active timer disables start and postponement but never prevents dismissing the
preview. Write failures retain the plan and keep the error visible for retry.

## Implementation and style

Use pure Kotlin domain functions in `domain/planner`, stateless Compose sections
in `ui/screens`, and the existing planner ViewModel/preferences for persistence.
Use Material theme tokens, English labels, 48 dp controls and expandable rows.
No new permission, dependency, database schema, online service or CI change.

## Verification

JUnit covers capacity, deadlines, partial credit, rest days, removed goals and
unchanged future work. Robolectric Compose tests cover live progress, bounded
start, expansion, active-session protection, cancel and confirm/error flows.
Run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`.
Phone checks: create a weekly plan, open Today, study a partial block, reopen,
preview postponement, cancel then confirm, restart and inspect the weekly plan.

## Build order

1. Test and implement the daily projection and immutable postponement preview.
2. Test and implement the compact daily section and preview confirmation.
3. Integrate the existing dashboard, run regression/build checks and document results.

## Local verification, 2026-10-08

`testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon` passed.
223 tests passed, including 12 new domain and Compose tests. Lint reported
0 errors and 34 pre-existing warnings. Both APK variants built; release is unsigned.
The integration test verifies confirmation, stored dates and reopening the weekly view.

An earlier full run encountered intermittent Espresso `AppNotIdleException` in
unchanged accessibility controls. The failing control passed in isolation, then
the complete suite passed with a single-use Gradle process. No tests or timeout
checks were disabled. The underlying intermittent test-idling issue is not claimed
as permanently fixed. No physical device was attached; phone checks remain manual.
