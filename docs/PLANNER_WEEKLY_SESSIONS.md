# Weighted weekly study sessions

With saved availability windows, new plans also assign local clock times while
preserving these allocation limits. See [timed study sessions](PLANNER_TIMED_SESSIONS.md).
The date-only mode remains supported when no windows are configured.

## Use

Open Study → Plan → Schedule and create a weekly plan or choose Edit answers.
Set an optional weekly goal (0–56 hours), course effort and finish dates, daily
capacity, and session length. Subject settings supply priorities and optional
preferred weekly hours. Preview the actual first-week course totals and dated
sessions before saving. Cancel/back never changes the accepted plan.

The goal is a maximum for each Monday–Sunday week, not a promise to fill every
minute. Partial weeks use only available dates. A zero goal deliberately creates
an empty schedule. Clearing the goal restores deadline-only scheduling. Small
final blocks preserve exact remaining minutes, even below the chosen session size.
When work remains unscheduled, saving requires explicit acknowledgement.

## Allocation contract

For each week, reserve deadline-required work before discretionary priority
shares. Estimate future capacity separately for each course and each shared
deadline prefix, respecting the weekly target and course limits. Process those
reservations in deadline order. Distribute the remaining budget using the
highest-averages integer allocator described in [weekly targets](PLANNER_WEEKLY_TARGET.md).
Convert the resulting quotas into daily blocks with the deadline-aware builder.

Every generated block fits daily capacity, the overall weekly ceiling, the
course's frozen weekly ceiling, remaining effort and its inclusive deadline.
Block IDs are deterministic and unique. Shortfalls are the exact difference
between entered effort and scheduled minutes. This bounded greedy algorithm is
not a global optimization solver and does not claim an impossible schedule can
be repaired by saving it. Complex competing constraints may require adjusting
the goal, course limits, capacity or deadlines.

The daily time check deliberately reports raw daily capacity only. Its spare
time does not override weekly limits. Review actual scheduled totals and shortage
warnings too. No clock start/end times, calendar locking or network service is
introduced in this update.

## Accepted data and edits

Version 5 of the `date_only` envelope stores an integral `weeklyGoalMinutes`
and a required `weeklyCourseLimits` array of `{bossId, minutes}` records. An
empty array means no additional per-course ceiling, not advisory mode. Limits
are copied from the subject catalog at acceptance. Later catalog edits do not
silently move existing blocks or change their constraints.

Versions 1–4 remain readable with exact IDs, order, dates and progress baselines.
Version-4 weekly goals remain advisory, including schedules above their goals.
Reads never rewrite storage. Upgrade only by editing/accepting a preview or
explicitly confirming Rebuild remaining. Plans without goals still write v3.
Malformed policies, unknown IDs/fields, duplicate entries and cap violations
are rejected without replacing unreadable stored data.

Moves and No time today honor both destination-day and destination-week limits.
Moving inside the same week releases the source reservation first. Partial credit
still reserves the full moved slot. Failed multi-block postponements are atomic.
IDs and recorded credit do not change. Subject removal prunes only its own saved
limit, course and blocks. Confirmed rebuilds capture current subject limits and
recorded progress; an incomplete rebuild leaves the accepted plan untouched and
directs the user to the editor for a preview instead.

## Verification checklist

- Priority ratios, deadline reserves and competing infeasible late work.
- Partial weeks, zero target, exact fractional effort and a 100-hour course.
- Deterministic generated cases checking accounting and every capacity bound.
- Strict v5 round trips, malformed records and unchanged literal v4 storage.
- Move/postpone limits, unchanged study credit and subject removal.
- Actual-share UI, incomplete acknowledgement, restored goal and confirmed rebuild.
- Full regression, lint and both APK builds before handoff.

Manual device check: install over the existing app without clearing its data,
open the old plan, confirm it stays unchanged, edit a weekly goal and review the
preview. Save, reopen, move a session to a full week, and try No time today. Check
rotation and large font settings. Physical installation is not covered by JVM
or Compose host tests.

Local verification on 2026-10-21:
`testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1`
passed with 349 tests (zero failures, errors or skips), zero lint errors and 34
existing warnings. Debug and unsigned Release APKs built successfully. Native
layout checks cover 288 dp content width at 1.5 font scale. No device or emulator
was attached; the manual checklist above remains a device handoff requirement.
