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

The report opens at the calendar's displayed week. Navigating the report does
not move the underlying calendar; returning restores its previous week and scroll
position. Close planner exits the dialog; Android Back or Back to schedule returns
from the report first. Editing controls stay outside the report. The credit
explanation expands on request, and section chips wrap without splitting words
when system fonts are enlarged. Zero-minute bars have no colored fill.

## Delivery

1. Pure report aggregation and accounting tests; verify and commit.
2. Accessible chart components with interaction tests; verify and commit.
3. Weekly navigation integration, restoration, native layout checks, docs, full
   tests, lint and Debug/Release builds. Commit locally; push only when requested.

## Checks

- [x] Exact weekly/day/course/priority sums, boundaries, ties and empty data.
- [x] Break exclusion, ID-ordered credit, seconds, baselines and missing goals.
- [x] Read-only interactions, selection and live recomputation.
- [x] Week navigation, saved-state restoration and unchanged persisted records.
- [x] Small-screen/large-font renders, full tests, lint and APKs.
- [ ] Physical install, rotation, TalkBack and keyboard checks (requires device).

## Phone smoke test

1. Install the Debug APK over the existing app without uninstalling or clearing
   data. Check goals, history, rewards, accepted plans and availability remain.
2. Open Study > Plan > Open weekly plan. Select a week, then Weekly report.
   Check daily bars sum to Planned focus. With planned breaks, check their
   separate total and confirm they are not included in focus time.
3. Select days, subjects and priority groups. Check session clocks, exact
   minutes, course labels/colors and accepted priorities. Zero-minute subjects
   remain listed; a week without sessions stays usable.
4. Open About session credit. Verify the distinction from activity dates and
   completed-goal credit is clear. Compare actual study records in Profile >
   History, not by assuming report credit occurred on a scheduled date.
5. Navigate report weeks, rotate, then return using both Back to schedule and
   Android Back. The original calendar week and position should return. Reading
   a report must not change any saved plan, goal progress, reward or history.
6. Enable large system fonts and TalkBack. Check full labels, selected states,
   exact time values, scroll access, and the pinned return button. If using a
   keyboard, verify focus and activation across tabs, bars and week controls.

No storage version, network integration, permission or dependency was added.
This feature is not a store release; remaining roadmap and physical acceptance
checks still apply.

## Local verification

On 2026-10-30, the complete local suite passed: 529 tests, zero failures, errors
or skips. lintDebug reported zero errors and the same 38 existing warnings, none
in the changed files. Debug and unsigned Release builds succeeded. No tests,
assertions or quality checks were disabled.

Native-rendered 320 dp / 1.5-font-scale and 800 dp layouts passed full-label and
touch-target checks and visual review. The large-font check also guards against
short tab labels splitting inside words. Generated images are local evidence in
`app/build/reports/planner-report/`, not screenshots from a physical phone.
The real planner entry test checks saved plan, subject and availability records
remain byte-identical through report navigation and state restoration.

Scoped review confirmed the report receives read-only inputs, shares the
existing credit rules and does not touch timers, rewards, migrations, permissions
or dependencies. No phone or emulator was connected; upgrade retention, system
Back, rotation, TalkBack and keyboard use still require the smoke test above.

Test artifact: `solo-studying-weekly-report-20261030-debug.apk`, 20,541,272 bytes.
Application ID `com.amkumirab.solostudying`, version `1.0` / `1`, min SDK 24,
target SDK 36. APK signature scheme v2 verified; the debug certificate matches
the previous planner-breaks test package. Install over it without clearing data.

SHA-256:

`DB44CA848D4610B8282EEDA2D203DB1FB9B3845BF90178A596D76EA581408688`
