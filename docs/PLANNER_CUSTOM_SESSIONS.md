# Custom study sessions

## Contract

Add a timed session for a course already in the accepted plan, and remove an
unstudied, unlocked session after explicit confirmation. Both proposals are
immutable and save through the existing expected-snapshot path. No timer,
reward, goal history, Room schema, dependency or permission changes.

An added session selects a course, local date/start and 1–120 whole minutes.
It consumes only that course's unscheduled work; it never borrows or moves
other sessions. Reject missing/completed courses, dates before today/start or
after the deadline, overlaps, invalid windows, daily/weekly/course limits,
exhausted IDs and the 18,000-block limit. If all work is allocated, explain that
the user can shorten/remove an unstudied session first. Adding must not absorb
already-recorded but unallocated credit; ask for an explicit remaining rebuild.

Removing applies to timed sessions dated today or later, whether generated or
manual. Reject locked, missing-course or partly/fully studied sessions. Return
the exact minutes to unscheduled course work without marking study complete.
Surviving IDs, list order, slots, config/baselines and credited seconds stay exact.

New blocks append after the greatest surviving ID, preserving credit order.
Strict v9 timed records add a required boolean `manual` on every block alongside
the v8 fields. v1–8 reads remain unchanged; write v9 only while manual blocks
exist. A manual block may be longer than the generated preferred length, up to
120 minutes. Show its origin independently of duration edits or locks. Explicit
rebuilding replaces unlocked manual sessions, like other unlocked sessions;
locking preserves the exact manual slot and marker.

## UI and verification

Reuse existing Material controls/theme and pure domain validation. Add a weekly
Add session entry and a per-session Remove action. Preview the selected slot,
remaining unscheduled minutes and unchanged history before confirmation. Keep
drafts across recreation; disable actions while busy/active and reject stale
previews if the plan, date or recorded course progress changes. Cancel writes
nothing. Date-only plans explain that timed windows must first be accepted.

Source: `domain/planner`, `planner/StudyPlanCodec.kt`, `ui/screens`; tests mirror
those packages in `app/src/test` (JUnit, Robolectric and Compose). Follow existing
immutable `copy` proposals rather than mutating collections. Two local commits:
domain/storage and then UI/integration. Do not publish until requested.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1
```

Test accounting, strict v9/legacy bytes, caps/windows, boundary times, locks,
credit, removed/completed courses, restored drafts, cancellation, concurrent
saves and large-font narrow-screen layout. Never weaken existing tests or
suppress new warnings. Real-phone upgrade, rotation and TalkBack are manual
unless a device is connected. Reassignment and configurable breaks are later.

## Device test path

Install the new debug APK over the existing app without uninstalling or clearing
data. Open Study → Plan → Schedule. If Add session is absent, save Availability
windows and explicitly accept a timed plan via Edit answers first.

1. If all course work is allocated, shorten an unstudied, unlocked session using
   Edit duration to release some minutes. Existing study credit must stay exact.
2. Choose Add session, a course, date, free local start and released minutes.
   Check its end time and remaining unscheduled work. Cancel must save nothing.
3. Add and confirm a valid slot. Reopen the planner: the slot and Manual session
   label must persist. Try an overlapping slot and verify saving is disabled.
4. Remove the new slot, first cancelling and then confirming. Only the confirmed
   removal returns its minutes to unscheduled work. The course remains present.
5. Lock a session: Remove session must be disabled. Unlock it to remove it.
   Study a session: its recorded credit must prevent removal.
6. Rotate while editing, reopen the app, and check TalkBack labels and larger
   system text. Rebuild only when desired: unlocked manual sessions are replaced,
   while compatible locked manual sessions retain their exact slots.

## Verified locally, 2026-10-26

The complete command above succeeded with 442 tests, zero failures, errors or
skips. Debug and unsigned Release APKs built. lintDebug reports zero errors and
35 existing warnings, none in files changed for this feature. SDK tool/schema
and older API deprecation warnings remain separate from this planner slice.

Coverage includes 120 generated add/remove accounting cases, the full 18,000
block ledger, midnight end times, immutable locks/credit, strict v9 markers,
legacy bytes, real saves, concurrent conflicts, restored inputs, course/date
selection, cancellation and native narrow-screen checks at 1.5 font scale.
A regression test caught a Remove action appearing in date-only plans; it now
passes with the action limited to timed plans.

An initial combined run was stopped after existing AccessibilitySemanticsTest
AppNotIdle failures. Its unchanged 24-test suite passed in a fresh worker, then
the full 442-test run passed. No test, assertion or timeout was weakened, disabled
or skipped to obtain that final result. The intermittent test-runner issue is
not claimed to be fixed by this feature.

Review covered accounting, legacy storage, stale proposals, bounded inputs and
layout. No Android device/emulator was attached; phone upgrade, real rotation,
keyboard and TalkBack checks remain manual. No publication was performed.
