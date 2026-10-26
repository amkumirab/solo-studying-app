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
