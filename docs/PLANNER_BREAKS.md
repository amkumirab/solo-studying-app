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
2. Wizard controls, preview/week break labels, UI tests and full verification.

## Verification

- [x] Exact gap, natural-window gaps, shortfalls and midnight boundaries.
- [x] Determinism, weekly limits, deadline safety and locked-slot retention.
- [x] Add/move/resize/postpone guards and unchanged credit/history.
- [x] Strict v10 round-trip, legacy bytes and stale-save rejection.
- [ ] Draft restoration, explicit acceptance and small-screen accessibility.
- [ ] Full tests, lint and Debug/Release builds.
- [ ] Physical upgrade and TalkBack smoke test (requires a phone).

Capacity estimates assume preferred-length sessions. Course boundaries, locks,
weekly limits and fragmented windows can reduce actual scheduled study time;
the generated session list and explicit shortfalls are authoritative. Unplaced
fragments are retried on later valid days of the same week, and only placed
minutes reduce outstanding course work.
