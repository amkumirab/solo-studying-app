# Offline study planner

## Contract

The planner asks guided questions and builds a deterministic local schedule without network access,
accounts, subscriptions, new permissions, or new dependencies.

- Select up to 20 existing, unfinished course goals. Enter remaining focus hours,
  a **finish-by date** (inclusive, within the next year), and priority per course.
- Set Monday-to-Sunday focus capacity independently, from 0 to 480 minutes.
  Zero means a rest day; breaks are not included in this budget.
- Choose a preferred block length, 15 to 120 minutes. Smaller final blocks are
  allowed to fit the daily budget and remaining effort.
- Spread work across available capacity, preferring full focus blocks and protecting earlier deadlines first.
  Priority breaks deadline ties. Show unscheduled minutes per course explicitly.
- Preview before accepting. An incomplete plan may be saved only after its
  capacity warning is acknowledged. Never label it feasible.
- Browse seven-day windows. Launch a block through the existing bounded goal
  timer; opening a block never creates study credit or rewards.
- Credit only increases in recorded course study time since acceptance. Partial
  sessions count, completed goals need no further work, deleted goals are flagged.
- Credit follows original block IDs, not edited dates, so moving a block cannot
  move previously earned credit. Rebuild rounds fractional remaining minutes up.
- Move only untouched blocks to a future available day within the course deadline
  and daily capacity. Rebuild the remaining plan only after confirmation; existing
  study history, course targets, and rewards are never rewritten.
- Persist one versioned, validated local record. Corrupt or unsupported records
  report an error instead of being silently overwritten. Clear requires confirmation.

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
