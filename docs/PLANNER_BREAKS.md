# Breaks between planned sessions

## Contract

Timed plans may opt into a minimum gap of 0–30 whole minutes between consecutive
sessions on the same day. Zero keeps existing behavior. A natural gap between
availability windows counts toward the break; no break is required before the
first session or after the last. Breaks are not study blocks, earned credit,
weekly study hours, rewards or automatically started break timers.

Choose the gap in the session-length step of Edit answers. Preview and explicitly
save the resulting plan. Opening an old plan never moves its sessions. Existing
locked slots must stay exact; incompatible locks reject the preview without a
write. Recorded study history is never rewritten. The ordinary remaining-plan
rebuild carries the accepted gap forward.

Generation, moving, postponing, adding and resizing sessions must respect the
same spacing rule. Work that does not fit remains explicitly unscheduled. Keep
the current date-only flow; it cannot enable clock-based breaks without windows.

## Storage and delivery

Positive gaps use strict plan format v10, including all existing session markers.
Versions 1–9 read with a zero gap and remain byte-identical until explicit save.
Malformed or future records must not be overwritten. No Room migration,
dependencies, permissions, timer or reward changes are needed.

1. Domain/codec tests, spacing-aware generation and mutation guards.
2. Wizard controls, preview/week break labels and interaction tests.
3. Native layout checks, documentation and full verification before publication.

## Verification

- [x] Exact gap, natural-window gaps, shortfalls and midnight boundaries.
- [x] Determinism, weekly limits, deadline safety and locked-slot retention.
- [x] Add/move/resize/postpone guards and unchanged credit/history.
- [x] Strict v10 round-trip, legacy bytes and stale-save rejection.
- [x] Draft restoration, explicit acceptance and small-screen accessibility.
- [x] Full tests, lint and Debug/Release builds.
- [ ] Physical upgrade and TalkBack smoke test (requires a phone).

Capacity estimates assume preferred-length sessions. Course boundaries, locks,
weekly limits and fragmented windows can reduce actual scheduled study time;
the generated session list and explicit shortfalls are authoritative. Unplaced
fragments are retried on later valid days of the same week, and only placed
minutes reduce outstanding course work.

## Phone smoke test

1. Install the Debug APK over the current installation without uninstalling or
   clearing data. Check existing goals, study history, rewards and old plans.
2. Open Study > Plan > Open weekly plan > Edit answers. With saved Availability
   windows, continue to the session-length step. Set Break length to 10 minutes.
3. Preview a two-hour window with 60-minute sessions and 120 minutes of work due
   that day. Expect 60 minutes of study, a 10-minute break, then 50 minutes of
   study, with 10 minutes of work still unscheduled. Saving requires acknowledging
   the incomplete plan. Going back or cancelling must not change the old plan.
4. Save, close and reopen. Verify the gap, clock intervals, study totals and
   shortfall persist. There must be no trailing break after the final session.
5. Try adding, moving or growing a session into a reserved break. It must reject
   the change. A sufficiently spaced slot must remain usable. Lock a session and
   rebuild; its time must stay exact, or a conflict must leave the plan unchanged.
6. Edit answers back to 0 and preview before saving. Date-only plans should offer
   guidance to save windows first, without changing their existing timers.
7. Rotate with a break draft open. Check the value survives. With TalkBack, check
   the field label, validation, break interval and preview/save actions. Check
   large system fonts, keyboard entry and portrait/landscape layouts.

Positive-gap plans need the new reader. An older app cannot open a v10 plan;
its existing unsupported-record guard must preserve it rather than reset it.

## Local verification

Verified on 2026-10-29: 514 tests passed with zero failures, errors or skipped
tests. Debug and unsigned Release builds succeeded. lintDebug reported zero
errors and the same 38 pre-existing warnings. No test checks were weakened.

The editor and weekly gap labels passed native-rendered 320 dp / 1.5-font-scale
checks and visual review. Generated PNGs live under
`app/build/reports/planner-breaks/`; they are not tracked screenshots of a phone.
No phone or emulator was connected, so physical upgrade retention, rotation,
TalkBack and keyboard behavior still need the smoke test above.

Test package: `solo-studying-planner-breaks-20261029-debug.apk`, 20,509,789 bytes.
Application ID: `com.amkumirab.solostudying`; version name/code: `1.0` / `1`;
min SDK 24, target SDK 36. Debug signature scheme v2 verified with the existing
debug certificate. This is a local test artifact, not a store release.

SHA-256:

`FED9036EB5649222B99A58B7B2A779A29C819C49E8490C41D1E22C7B13E530B1`
