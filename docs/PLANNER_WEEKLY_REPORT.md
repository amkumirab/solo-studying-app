# Weekly planner report

## Contract

Read-only charts describe the displayed Monday–Sunday week of an accepted plan:
seven daily planned-minute totals, planned minutes per course and the priority
split (Low 1–2, Normal 3, High 4–5). Use accepted course priorities, not unsaved
catalog changes. Retain all accepted courses, including zero-minute and missing
goals. Derive data from existing blocks; never regenerate, save or mutate a plan.

Planned focus minutes exclude breaks. Show minimum reserved break time separately
when applicable. All charts use exact minutes and a common scale within a chart;
labels never imply that planned sessions are completed study. Empty weeks and
zero totals must remain readable without division by zero.

The existing credit ledger can report **Credit assigned to these sessions**.
It cannot prove when the user studied: IDs, not the scheduled date, determine
credit order, and a completed goal can fully credit its planned sessions. Reuse
`blockProgress` across the entire plan before selecting the displayed week.
Never label this value as actual study performed during that week. Preserve
seconds and make missing goals and the distinction from the activity log clear.

## Interface

Add a report action to the weekly plan without changing the default schedule.
Keep the report in its own screen/dialog with week navigation and an obvious
return action so charts do not crowd the session controls. Reuse the current
theme and Material components; no new chart dependency.

Days, Subjects and Priorities views share the same derived report. Selecting a
day shows its accepted sessions; selecting a course or priority shows its exact
planned totals. Use text values alongside bars, adequate touch targets, meaningful
selection states and scrollable layouts. Recompute on plan/progress changes;
current subject labels/colors are display metadata only. No write on inspection.

## Delivery

1. Pure report aggregation and accounting tests; verify and commit.
2. Accessible chart components with interaction tests; verify and commit.
3. Weekly navigation integration, restoration, native layout checks, docs, full
   tests, lint and Debug/Release builds. Commit locally; push only when requested.

## Checks

- [ ] Exact weekly/day/course/priority sums, boundaries, ties and empty data.
- [ ] Break exclusion, ID-ordered credit, seconds, baselines and missing goals.
- [ ] Read-only interactions, selection and live recomputation.
- [ ] Week navigation, saved-state restoration and unchanged persisted records.
- [ ] Small-screen/large-font renders, full tests, lint and APKs.
- [ ] Physical install, rotation, TalkBack and keyboard checks (requires device).
