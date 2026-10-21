# Offline study planner

## Contract

The planner asks guided questions and builds a deterministic local schedule without network access,
accounts, subscriptions, new permissions, or new dependencies.

- Select up to 20 existing, unfinished course goals. Enter remaining focus hours,
  a **finish-by date** (inclusive, within the next year), and priority per course.
- Manage [goal-linked subjects](PLANNER_SUBJECTS.md) with planner-only names,
  priorities 1–5, named colors, optional deadlines and preferred weekly hours.
  Weekly preferences do not replace total remaining effort. Explicit editing or
  confirmed rebuilding applies scheduling defaults; metadata edits do not move
  accepted sessions automatically.
- Set Monday-to-Sunday focus capacity independently, from 0 to 480 minutes.
  Zero means a rest day; breaks are not included in this budget.
- Choose a preferred block length, 15 to 120 minutes. Smaller final blocks are
  allowed to fit the daily budget and remaining effort.
- Spread work across available capacity, preferring full focus blocks and protecting earlier deadlines first.
  Priority breaks deadline ties. Show unscheduled minutes per course explicitly.
- Preview before accepting. An incomplete plan may be saved only after its
  capacity warning is acknowledged. Never label it feasible.
- Before saving, use the [study time check](PLANNER_TIME_BUDGET.md) to see the
  minimum uniform daily budget needed across your study days. Compare exact
  15-minute daily changes, inspect each deadline and reset to your entered
  budget. A final surplus never hides an earlier deadline shortage.
- Browse seven-day windows. Launch a block through the existing bounded goal
  timer; opening a block never creates study credit or rewards.
- Credit only increases in recorded course study time since acceptance. Partial
  sessions count, completed goals need no further work, deleted goals are flagged.
- Credit follows original block IDs, not edited dates, so moving a block cannot
  move previously earned credit. Rebuild rounds fractional remaining minutes up.
- Move only untouched blocks to a future available day within the course deadline
  and daily capacity. Rebuild the remaining plan only after confirmation; existing
  study history, course targets, and rewards are never rewritten.
- Persist versioned, validated local plan and subject records. Corrupt or unsupported records
  report an error instead of being silently overwritten. Clear requires confirmation.
  Version-1 and version-2 plans remain readable without rewriting them on open;
  plans without a weekly goal use the version-3 date-only envelope with five priority levels.
  New weekly-limited plans use version 5 with frozen course limits; versions 1–5
  remain readable. Version-4 goals stay advisory until an explicit edit/rebuild.
  Legacy numeric priorities retain their exact values and 1–3 bounds.
  Migration preserves block IDs,
  ordering and progress baselines. Unknown fields or versions prevent a save
  from replacing the existing record. See the [storage decision](decisions/0001-planner-storage-versions.md)
  and [subject catalog decision](decisions/0002-planner-subject-catalog.md).

- Set an optional [weekly study target](PLANNER_WEEKLY_TARGET.md) to generate
  [weighted weekly sessions](PLANNER_WEEKLY_SESSIONS.md). Preview actual shares
  and unscheduled work before saving. Weekly and frozen course limits also apply
  to moves/postponement. Scheduled time is never reported as completed work.

## Implementation slices

Pure scheduling and progress rules, versioned local storage, guided editor and
weekly view, Study Hub integration, then regression and build verification.
Use existing Material theme tokens, accessible text-labelled controls and scrollable
layouts. Draft input survives configuration changes; accepted plans survive restarts.

## Verification

Automated tests cover deadline capacity, rest days, priorities, fractional progress,
infeasible plans, deterministic generation, moving blocks, malformed storage, input
validation, wizard navigation, warnings, and session launch callbacks.
Run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`.

Phone checklist: plan five courses with different deadlines; include rest days;
save and restart; finish and end a block early; inspect updated progress; move an
untouched block; confirm a rebuild; verify another active timer cannot be replaced.
Check keyboard, larger font, rotation, and airplane mode. This checklist is manual
and is not claimed as completed without an attached device.

### Local verification, 2026-10-05

`testDebugUnitTest lintDebug assembleDebug assembleRelease` completed successfully:
211 tests passed, including 30 new tests and 500 generated scheduling scenarios
inside the domain suite. Lint reported 0 errors and 34 pre-existing warnings.
The release APK is unsigned; use the debug APK for device testing.
Draft restoration also checks that an open date picker is dismissed safely.
Corruption recovery uses a fresh view model to model reopening the application;
an already-loaded view model deliberately retains its in-memory accepted plan.

### Storage foundation verification, 2026-10-16

`testDebugUnitTest lintDebug assembleDebug assembleRelease` completed successfully:
267 tests passed with no failures, errors or skips, including 16 storage
compatibility tests. Lint reported 0 errors and 34 existing warnings. Debug and
unsigned Release APKs built successfully.

Compatibility checks cover a literal version-1 fixture, non-sequential block IDs,
moved dates, fractional study credit, empty schedules, the 18,000-block limit,
unsupported schemas and an already-open editor encountering a newer record.

Manual upgrade check: install the new debug APK over the previous version without
uninstalling or clearing app data. Open an accepted plan, compare its sessions and
credited progress, explicitly save a change, then restart and compare again.
This physical-device check has not been performed.
