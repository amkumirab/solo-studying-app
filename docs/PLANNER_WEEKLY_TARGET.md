# Weekly study target

New or explicitly rebuilt plans now apply weekly targets to real sessions; see
[weighted weekly sessions](PLANNER_WEEKLY_SESSIONS.md). The calculation and
version-4 behavior documented below describe the earlier advisory mode, retained
for existing plans. Opening an old plan never changes its sessions or schema.

In Study → Plan, open the weekly planner and create a plan or choose Edit answers.
The availability step includes an optional weekly goal from 0 to 56 hours.
Clear the field to remove it. The goal survives saving, reopening and confirmed
rebuilds. Leaving it blank preserves the existing deadline-planning flow.

The preview shows a recommended split for the first calendar week, Monday through
Sunday. A plan starting midweek excludes earlier days. The recommendation uses
the entered scenario's daily capacity, priority 1–5, inclusive finish dates,
remaining course effort and optional preferred weekly hours. Preferred hours
are a ceiling for this recommendation; they do not replace total remaining work.
In newly accepted version-5 plans, these ceilings apply to actual sessions too.

## Calculation

Each course has weight `priority * (14 - daysUntilFinish.coerceIn(0, 7))`.
Courses finishing outside this week have the same baseline urgency. Distribute
whole minutes with the highest-averages method: each next minute goes to the
eligible course with the highest `weight / (allocatedMinutes + 1)` ratio.
Compare integer cross-products and use the smallest goal ID to break ties.

A course is eligible only below its remaining-effort and preferred-hour ceilings.
Every allocation must also fit each cumulative deadline capacity through Sunday.
This prevents several courses due Monday from spending Tuesday's capacity.
Stop at the weekly goal, available capacity or the point where no course can
accept more time. Show the exact unallocated goal and free capacity. No minutes
are silently discarded by rounding. Calculations are local and deterministic;
the maximum is 3,360 minutes across 20 courses.

## Recommended versus scheduled time

In legacy version-4 plans, this provides advice, not weighted timetable generation.
The accepted date-only schedule still uses the deadline-first algorithm. The preview
labels recommendations separately, and the weekly view reports actual scheduled
minutes, free capacity and the shortfall or overage against the weekly goal.
Scheduled time is never reported as completed work. Setting a lower goal does
not silently delete sessions. Version-5 plans apply quotas only after explicit
edit/acceptance or confirmed rebuild. Assigned clock times remain later work.

## Persistence

Advisory plans with a weekly goal use a strict version-4 `date_only` envelope.
Its plan body adds one integral `weeklyGoalMinutes` field, from 0 through 3,360. Plans
without a goal continue to use version 3. Readers accept versions 1–5; older
versions never acquire an invented goal. Reads do not write. IDs, dates, block
order, numeric priorities and study-credit baselines are preserved.

Unknown schemas, extra or missing fields, coercible strings, fractional JSON
numbers and invalid limits are rejected. Existing store conflict protection and
explicit recovery still apply. No dependencies, permissions or Room changes.

## Verification

Automated coverage includes weighted shares, deadline prefixes, preferred limits,
capacity shortfalls, exact totals, repeatability, zero goals, partial weeks,
legacy bytes, v4 round trips, malformed schemas, restored inputs and the saved
goal. Device installation and rotation are manual checks when no device is
connected.

### Local verification, 2026-10-19

`testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1`
completed successfully: 331 tests, zero failures, errors or skipped tests. Lint
reported zero errors and 34 existing warnings. Debug and unsigned Release APKs
built successfully. The new native layout check verifies 288 dp content width at
1.5 font scale. No phone or emulator was connected, so physical-device checks
remain pending.
