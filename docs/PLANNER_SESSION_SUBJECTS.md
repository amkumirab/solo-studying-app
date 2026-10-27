# Change a session's subject

## Contract

An unstudied, unlocked timed session dated today or later can be reassigned to
another unfinished course in the accepted plan. Its date, clock time and duration
stay fixed. Preview both courses' unscheduled minutes and save only after explicit
confirmation. Cancel does not write anything.

The destination needs enough unscheduled work, a deadline covering the session,
and room within its saved weekly limit. Reassignment never borrows time, changes
workload, moves another session or edits recorded study history and rewards.
Date-only plans, missing courses, studied sessions and active-session edits are
not supported. Rebuild the remaining plan if recorded but unscheduled credit
would otherwise be assigned to the replacement session.

## Credit and storage

Credit follows ascending block IDs, not display order. Remove the zero-credit
source and append a manual replacement with an ID above every existing ID. Keep
all surviving blocks and the configuration (including baselines) exactly intact.
Verify that every surviving credit allocation is unchanged and the replacement
has zero credit. Refuse ID exhaustion; replacement does not increase block count.
The existing version 9 manual-session format stores this without a migration.
Snapshot compare-and-save rejects a preview if another writer changed storage.

## Delivery checklist

- [x] Pure proposal, accounting, credit-order and boundary regression tests.
- [x] Version 9 round-trip, legacy read preservation and stale-save tests.
- [x] Weekly action, guided preview, cancellation and restored draft tests.
- [x] Real entry/save and concurrent-edit protection tests.
- [x] Small-screen/large-font checks, full tests, lint and debug/release builds.

Verified on 2026-10-27: 474 tests passed with zero failures, errors or skipped
tests. Debug and unsigned Release builds succeeded. Lint has zero errors and the
same 36 existing warnings, none in the files changed for this feature.

Physical-device installation and TalkBack testing require a connected device;
desktop Compose tests are not a substitute for those checks.

## Phone smoke test

1. Install the Debug APK over the current installation without uninstalling it.
2. Open Study > Plan > Open weekly plan. Use a timed plan with at least two
   unfinished courses. The destination needs unscheduled work; if all its work is
   scheduled, explicitly shorten or remove one of its unstudied slots first.
3. Choose Change subject on an unlocked, unstudied session. Check that its date,
   clock range and duration stay fixed and both unscheduled totals are correct.
4. Cancel and verify that nothing changed. Repeat, confirm, then reopen the app:
   the selected course, priority and color should appear on the same slot.
5. Start Study course and verify the new course and unchanged duration. Check
   locked/studied sessions cannot change subject and use TalkBack to review the
   selector, preview, rejection messages and confirmation.
