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
Keep the open-planner action above the overview and calendar week controls above
the summary so primary navigation remains reachable in short viewports. Calendar
day-budget labels use the same inclusive plan-date bounds as the weekly total;
outside dates are labelled explicitly instead of advertising unused capacity.
Do not change the separate allocation preview or add automatic regeneration.
Empty/loading/unreadable planner states keep the existing safe entry flow.

## Delivery

1. Pure overview calculations, edge-case tests and focused regression; commit.
2. Overview component, actual entry/calendar integration and interaction tests;
   commit after validation.
3. Native layout, accessibility and short-viewport regression tests; commit.
4. User documentation, accounting guidance and final verification records.
   Validate the full suite, lint and Debug/Release builds before publication.
   Publish these four commits together after the explicit push request; remote
   CI results are reported separately. This is not a store release.

## Checks

- [x] Exact totals, week boundaries, partial weeks, zero/absent/excess targets.
- [x] Breaks, manual/locked sessions, budget ceilings and immutable input.
- [x] Real entry/calendar reactivity and disclosures without persistence writes.
- [x] Accessible progress, restored disclosure, native narrow/large-font layout.
- [x] Full tests, lint, APK builds and scoped review.
- [ ] Physical upgrade, rotation, TalkBack, keyboard and reduced-motion smoke test.

This closes the overview slice, not full acceptance of the original university
planner specification. A complete requirements audit and physical testing remain.

## Phone smoke test

1. Install the test APK over the existing app without uninstalling or clearing
   data. Check the current goals, history, rewards and accepted planner records.
2. Open Study > Plan. Confirm the primary Open weekly plan action is above the
   overview, then compare goal, planned focus, budget and unallocated budget
   against the accepted sessions. Expanding details must not change the plan.
3. Open the calendar and move between weeks. Check overview and daily budgets
   agree. In a partial first/last week, dates outside the plan have zero budget;
   the full weekly target is explicitly retained. Reports must match focus totals.
4. Verify no target, zero target, above-target legacy plans, insufficient budget
   and weeks outside the plan stay readable. Positive goals have finite planned
   percentages; no state claims planned work is completed study.
5. Change a goal or availability through Edit answers, review and accept. Check
   summary values refresh only from accepted settings. Merely opening or
   cancelling an editor, overview or report must preserve saved records.
6. Use large fonts, rotate with details expanded and check portrait/landscape
   scrolling. With TalkBack, confirm labels, exact planned percentage and
   expanded/collapsed state. Test keyboard focus and activation. Set the system
   animation scale to zero and confirm progress remains accurate without motion.

Use the existing safe edit/rebuild flow for configuration changes; this overview
does not change scheduling, actual study credit, timers or rewards.

## Local verification

On 2026-11-01, 549 tests passed with zero failures, errors or skips, including
20 new overview cases. Coverage includes exact accounting, generated invariants,
entry/calendar/report agreement, accepted-plan reactivity, unchanged stored
records, restored disclosure, keyboard activation and exact progress semantics.
Native-rendered 320 dp / 1.5-font-scale, 800 dp tablet and a 360 dp-high large-font
viewport passed full-label and 48 dp-target checks and visual review.
lintDebug has zero errors and the same 38 pre-existing warnings, none in changed
files. Debug and unsigned Release builds succeeded. No build gate was lowered.

Integration verification caught an offscreen primary action; it now precedes the
overview. Virtualized list tests explicitly scroll to the target nodes before
checking launch, busy-state and invalid-move behavior. Existing assertions were
preserved; no tests were skipped or disabled. Daily labels now share the same
plan-date bounds as the weekly total, including partial and outside-plan dates.

No device or emulator was connected. Physical upgrade retention, rotation,
TalkBack, keyboard navigation across the full app and system reduced-motion
acceptance still require the phone smoke test above. Native PNGs under
`app/build/reports/planner-overview/` are local test evidence, not phone captures.

Test package: `solo-studying-planner-overview-20261101-debug.apk`, 20,549,952 bytes.
Application ID `com.amkumirab.solostudying`, version `1.0` / `1`, min SDK 24,
target SDK 36. Signature scheme v2 verified; its certificate matches the previous
weekly-report package. Install over that version without clearing data.

SHA-256:

`B5E87A8208C1B6B41C497DC13D67A2E83FA85CC8AC1FBEC8779C869AB765B700`
