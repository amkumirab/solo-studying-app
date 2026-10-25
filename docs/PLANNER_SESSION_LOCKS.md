# Session locks

## Contract

Keep a chosen timed session fixed when rebuilding or editing planner answers.
Locks protect planning, not access to the phone or the ability to study early.
Only future/today, unstudied sessions with an existing goal can be newly locked.
Unlocking does not change dates, times, IDs, order, baselines or study credit.
Active sessions disable planning mutations. Date-only plans remain unchanged.

Moves require unlocking first. No time today is atomic: an unfinished locked
session prevents applying the move; explain that the user must unlock it.
Explicit removal of a plan or subject remains possible through existing controls.

Rebuild and Edit answers retain every locked block's ID, subject, date, time and
duration. Reserve those slots and deduct their minutes from remaining effort,
daily/weekly capacity and course weekly limits before allocating other work.
Reject, without saving, a missing course, past or credited lock, changed windows,
reduced session length or capacity that makes a lock invalid. Explain conflicts;
never silently unlock, drop, move or count a lock twice. Unlock studied locks
before rebuilding so recorded progress can be rebased truthfully.

## Storage

Add `locked: Boolean = false` to blocks. Strict v7 timed records require a JSON
boolean on every block. Write v7 when any lock exists; otherwise keep v1–6 read
compatibility and the existing v3–6 writer. Reads never rewrite saved bytes.
Use existing snapshot conflict guards. No Room migration, dependencies,
permissions, cloud services or changes to timer/reward behavior.

Plans with [edited durations](PLANNER_SESSION_DURATIONS.md) use v8. Locked edited
durations remain fixed through rebuilding even when they exceed the preferred
generated length; the explicit override is bounded to 120 minutes.

## Implementation and verification

1. Domain locks, reserved-slot scheduling and strict storage: focused JUnit tests.
2. Weekly lock controls, safe rebuild/wizard integration and conflict messages:
   Compose interaction tests, cancellation/restoration and narrow-screen checks.
3. Scoped review, all regression tests, lint and Debug/Release APKs.

Keep local commits until publication is requested. Kotlin immutable models and
pure functions live in `domain/planner`; codecs in `planner`; Compose screens in
`ui/screens`. Tests mirror these under `app/src/test`. Reuse the existing theme
and patterns such as `block.copy(locked = true)`; no speculative abstractions.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1
```

Never weaken or skip tests. Ask before changing dependencies, Room or CI.
Real phone installation, upgrade retention, rotation and TalkBack remain manual
unless a device is available. Duration edits, reassignment, custom sessions and
automatic breaks are outside this slice.

## Manual test path

Install over the existing app without uninstalling or clearing data. Open Study
→ Plan → Schedule and lock an unstudied timed session. Its Move button becomes
unavailable, but Study course remains usable. Cancel a rebuild and check nothing
changes; confirm a compatible rebuild and verify the exact locked date/time.
Change Availability so the lock no longer fits, then rebuild: the accepted plan
must remain unchanged with an explanatory message. Try Edit answers too.
Unlock the session to apply the new windows. Restart the app and check locks
persist. No time today must explain unfinished locks rather than moving them.

## Verified locally, 2026-10-24

The full command passed: 396 tests, zero failures, errors or skipped tests;
lintDebug has zero errors and 34 existing warnings. Debug and unsigned Release
APKs built. Coverage includes 80 generated multi-lock schedules, strict v7 and
unchanged legacy reads, stale-save rejection, credit rebasing, maximum imported
IDs, full reserved days, wizard/rebuild conflicts and native narrow-screen text
at 1.5 font scale. No connected phone/emulator was available; the physical-device
checklist remains pending. No tests or assertions were weakened or disabled.
