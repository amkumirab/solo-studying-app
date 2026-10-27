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
- [ ] Weekly action, guided preview, cancellation and restored draft tests.
- [ ] Real entry/save and concurrent-edit protection tests.
- [ ] Small-screen/large-font checks, full tests, lint and debug/release builds.

Physical-device installation and TalkBack testing require a connected device;
desktop Compose tests are not a substitute for those checks.
