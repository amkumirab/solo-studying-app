# Session duration editing

## Contract

Edit the duration of an existing unstudied, unlocked timed session dated today
or later. Keep its subject, ID, date and start time. Accept integer durations
from 1 to 120 minutes independently of the preferred generated block length.
No new/custom sessions, subject reassignment, timer changes or automatic moves.

Increasing uses unscheduled time for the same course first. If necessary, take
minutes from that course's later-day, unstudied, unlocked sessions, furthest date
first (then latest start and greatest ID). Reserve same-week transfers first if
needed to respect a full target week's overall/course limit. Keep donor start times and IDs; shorten
their ends or remove a fully consumed donor. Never take recorded credit, locked
time, same-day sessions or another course's time. Show every changed/removed
session before explicit confirmation. If there is insufficient eligible time,
or the new slot violates windows, overlaps, daily/weekly limits or deadlines,
reject the whole proposal without saving or modifying the source.

Decreasing returns the difference to unscheduled course work. It never counts
as completed study. Show new end time, current-week totals and exact unscheduled
minutes for the course. Preserve course effort, baselines and credited seconds
for every surviving block; never renumber. Cancelling changes nothing.
Active/saving states disable edits. Revalidate when plan, recorded progress or
local date changes; retain existing snapshot conflict guards on save.

## Persistence and integration

Add an explicit `durationEdited: Boolean = false` block marker. Edited blocks may
exceed the preferred session length but never 120 minutes. Date-only records
cannot contain this marker or locks. Strict v8 timed records require `locked`
and `durationEdited` booleans for every block. Retain exact v1–7 reads and v3–7
writing for plans without duration edits; never rewrite bytes just by reading.
Unknown/missing/coerced fields fail closed. No Room migration, permissions,
dependencies, external services or CI changes.

Move, No time today and lock/rebuild must preserve edited duration markers.
Confirmed rebuild preserves locked edited durations; unlocked sessions may be
regenerated using the preferred length, as the existing rebuild contract says.

## Implementation and checks

Use pure Kotlin functions in `domain/planner`, codecs in `planner`, and an
accessible Compose dialog in `ui/screens`, reusing the existing Material theme.
Mirror tests in `app/src/test`. Follow immutable copies, for example
`block.copy(minutes = minutes, durationEdited = true)`.

1. Domain and strict codec tests first; implement atomic duration proposals.
2. Weekly duration action, complete confirmation preview, restored draft and
   unavailable states; verify Compose interactions and native narrow-screen text.
3. Scoped review, full regression, lint and Debug/Release builds. Keep commits
   local until publication is requested. Never weaken or skip tests.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1
```

Ask before changing dependencies, Room or CI. Physical upgrade, rotation,
TalkBack and installation remain manual unless a real device is connected.

## Manual test path

Install the debug APK over the existing app without uninstalling or clearing
data. Open Study → Plan → Schedule and select Edit duration on an unstudied,
unlocked timed session. Increase by 15 minutes where there is room before the
next session and the saved window end. Review the new end, week totals and every
shortened/removed donor, then cancel: nothing should change. Repeat and apply,
then reopen the planner to verify persistence. Decrease by 15 minutes and check
that unscheduled work increases without study credit or rewards. Try an overlap,
invalid number and locked session; none may be saved. Unlocked edits may reset
on a confirmed rebuild, while locked edited durations must remain fixed.

## Verified locally, 2026-10-25

The full command passed: 416 tests, zero failures, errors or skipped tests.
Debug and unsigned Release APKs built. Lint reports zero errors and 35 warnings,
all on unchanged files (including SDK/dependency freshness checks); no warnings
were suppressed. Coverage includes 150 generated accounting cases, credit/lock
invariants, full donor removal, same-week caps, 24:00, strict v8 fields, legacy
bytes, saved drafts, cancelled previews and the real snapshot-guarded save path.
Native layout tests verify complete text and 48 dp actions at 1.5 font scale on
a narrow screen. No connected phone/emulator was available; device checks remain
pending. No existing tests were disabled or weakened.
