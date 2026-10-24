# Timed study sessions

## Objective and contract

Assign real local start/end times inside saved availability windows after an
explicit preview/save or confirmed rebuild. Without saved windows, retain the
date-only planner. Never invent times on read or modify recorded study history.
The accepted plan freezes its windows independently of later availability edits.

Reuse the current deterministic daily/weekly allocation. Pack each day's work
from the first saved window forward. Split a generated block at a window boundary
without losing minutes; assign deterministic IDs only after packing. Keep current
session choices and exact minute precision, including 25-minute sessions and short
final blocks. Availability boundaries stay on the existing half-hour grid; session
boundaries may be minute-exact. No implicit breaks, reminders, UTC conversion or
claim about elapsed time across daylight-saving transitions.

Every timed block must fit one enabled window, end by 24:00, remain within its
course dates and all accepted daily/weekly limits, and never overlap another
block. Endpoints are exclusive, so adjacent sessions are valid. Preview and week
views show subject/color, local start/end, duration and priority. Today and
postponement previews also show times. Display chronological order without
reordering the persisted credit ledger.

Moves edit date and start time explicitly, preserve ID/duration and existing
credit, and reject studied blocks or invalid/overlapping slots. No time today
finds a complete future slot for each original block and applies atomically;
it does not split or renumber accepted blocks. Rebuild resets progress baselines
using existing recorded totals and explicitly captures current windows/limits.

## Storage and boundaries

An additive version-6 `timed` envelope includes frozen `studyWindows`, required
nullable weekly-goal/policy fields and required integral `startMinute` per block.
Reuse the validated availability codec. Read versions 1–5 losslessly with no
clock fields; preserve strict unknown-field rejection and snapshot conflict
protection. No new dependency, permission, Room migration or network service.
Session locks are added separately in [session locks](PLANNER_SESSION_LOCKS.md)
with strict v7 timed records. Custom sessions, duration/subject edits and
configurable gaps remain subsequent roadmap work.

## Implementation order and verification

1. Pure packing/validation, strict codec, safe date move/postponement: focused
   domain/storage tests and existing regression tests; local foundation commit.
2. Wizard/rebuild integration and readable mobile week/Today/preview times:
   Compose interaction and large-font layout checks; local display commit.
3. Accessible explicit-time move editor with validation, cancel/restoration and
   credit checks; full regression, lint, Debug/Release builds; local move commit.

Source: `domain/planner`, `planner` codecs and `ui/screens`; tests mirror these
packages in `app/src/test`. Follow existing immutable data classes, pure helpers,
Material theme and expected-snapshot persistence. Never weaken/skip tests or
rewrite legacy records on read. Use the checked-in wrapper with JDK 21:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1
```

Physical installation, rotation, TalkBack and an upgrade over real phone data
remain manual unless a device is connected. Keep all commits local until asked
to publish. No unrelated cleanup or changes to timer/reward behavior.

## Device test path

Install the debug APK over the existing app without uninstalling or clearing
data. In Study → Plan, save Availability windows, then Edit answers, review times
and Save plan. Check an old plan before saving: opening it must not add times.
Review the weekly list and Today, use Move to try both an overlapping time and
a free slot, and test Cancel. Confirm No time today shows new dates and times
before applying them. Study credit and rewards must remain unchanged by moves.

## Verified locally, 2026-10-21

The complete command above passed with 377 tests, zero failures, errors or skips,
zero lint errors and 34 existing warnings. Debug and unsigned Release APKs built.
Coverage includes 100 generated timed-allocation cases, a 20-course/year-long
capacity bound, strict storage compatibility, clock/date editing, cancellation,
saved drafts, credit/capacity guards and confirmed window updates. Native text
checks cover 288 dp content width and a narrow dialog at 1.5 font scale.

An initial combined run encountered an existing accessibility idle timeout.
Its unchanged suite and subsequent full regression runs passed in fresh workers.
No test was disabled or assertion weakened. No phone/emulator was connected;
the device checklist remains pending and is not claimed as automated coverage.
