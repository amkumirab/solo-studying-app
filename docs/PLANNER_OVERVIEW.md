# Weekly planner overview

## Contract

Consolidate the accepted plan's weekly goal, planned focus, available study
budget, unallocated budget, remaining target and percentage planned. Use a
compact overview in Study > Plan for the current week, and replace the older
goal-only summary in the calendar with the same overview for its selected week.
Keep the existing report, calendar, editor and persistence paths intact.

The overview is read-only and uses accepted plan values, not current unsaved
subject/availability settings. Reading, expanding details or changing calendar
weeks must not save, rebuild, credit study or change rewards. No dependency,
permission, database or storage-format change is required.

## Accounting

- Monday-Sunday weeks; only dates between plan start and its final course
  deadline contribute study budget. Identify partial weeks. The configured
  weekly goal is not prorated for a partial week, matching the scheduler.
- Scheduled focus is the exact sum of accepted blocks dated in the week. Never
  count breaks or recorded study credit as newly planned work or completion.
- Available study budget is the sum of accepted daily focus-budget ceilings.
  Unallocated budget is that sum minus scheduled focus, never negative. These
  are not clock-slot capacity estimates or promises another session will fit.
- Show reserved minimum breaks separately. Breaks use clock windows but do not
  reduce focus-budget ceilings: subtracting them from both would double-count
  constraints. Explain that windows, breaks, deadlines and saved course limits
  can prevent filling otherwise unused budget. Reuse the existing break helper.
- Remaining target and over-goal time are separate non-negative amounts.
  A goal above study budget has a definite budget shortfall; other unmet targets
  must not be described as a proven lack of available time.
- Missing goal, zero goal and outside-plan weeks are neutral explicit states,
  not fabricated completion or division by zero. An inactive week has no
  applicable weekly goal. Old date-only plans above their advisory goal remain
  readable and unchanged. For positive goals, progress is capped at 100% with
  excess scheduled time shown separately.

## Interface

Reuse the current Material theme, typography, spacing and controls. No new
navigation destination or oversized icons. Display the date range and four
compact labelled metrics, followed by planned-goal progress and exact feedback.
Keep detailed accounting guidance behind a labelled, stateful disclosure.
Progress describes scheduling, never completed study. Animate value changes
using Compose animation, respecting the platform motion-duration scale; expose
the exact target value in accessibility semantics rather than intermediate
animation frames. Preserve full labels and 48 dp controls with large fonts.

The old goal-summary implementation is replaced, not shown beside the new one.
Do not change the separate allocation preview or add automatic regeneration.
Empty/loading/unreadable planner states keep the existing safe entry flow.

## Delivery

1. Pure overview calculations, edge-case tests and focused regression; commit.
2. Overview component, actual entry/calendar integration and interaction tests;
   commit after validation.
3. Native layout QA, persisted-state and reactive-flow verification, docs and
   full tests/lint/Debug/Release builds; commit locally and provide a test APK.
   Publish only after a separate push request.

## Checks

- [ ] Exact totals, week boundaries, partial weeks, zero/absent/excess targets.
- [ ] Breaks, manual/locked sessions, budget ceilings and immutable input.
- [ ] Real entry/calendar reactivity and disclosures without persistence writes.
- [ ] Accessible progress, restored disclosure, native narrow/large-font layout.
- [ ] Full tests, lint, APK builds and scoped review.
- [ ] Physical upgrade, rotation, TalkBack, keyboard and reduced-motion smoke test.

This closes the overview slice, not full acceptance of the original university
planner specification. A complete requirements audit and physical testing remain.
