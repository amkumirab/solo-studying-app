# University planner acceptance audit

Status: **not all original requirements are complete**. This audit compares the
sixteen sections of the university-planner brief with the Android implementation.
Passing JVM tests does not certify a physical installation or store readiness.

## Requirement map

Paths below are relative to `app/src/main/java/com/amkumirab/solostudying/`.
Test classes live under the matching `app/src/test/java` package.

| Brief section | Implementation and evidence | Assessment |
| --- | --- | --- |
| 1. Existing-app integration | `ui/screens/StudyPlannerEntry.kt`, `ui/viewmodel/StudyPlannerViewModel.kt`; `AppOrganizationTest`, `PlannerWorkflowIntegrationTest` | Integrated into Study; local store and existing theme, no network or added dependency. |
| 2. Dashboard | `domain/planner/PlannerWeekOverview.kt`, `ui/screens/PlannerWeekOverviewContent.kt`; `PlannerWeekOverviewTest`, `PlannerOverviewEntryTest` | Accepted-week goal, focus, budget, unallocated budget, remaining target and animated percentage. Draft changes do not silently change the accepted overview. Budget is not guaranteed free clock capacity. |
| 3. Subjects | `domain/planner/PlannerSubject.kt`, `ui/screens/PlannerSubjectsScreen.kt`; `PlannerSubjectsUiTest` | CRUD, priorities 1–5, four named colors, optional deadline/weekly limit. Subjects require existing goals; they are not standalone courses. Confirmed deletion removes that course's automatic, manual and locked planned blocks, not recorded study. |
| 4. Availability | `domain/planner/WeeklyAvailability.kt`, `ui/screens/PlannerAvailabilityScreen.kt`; `PlannerAvailabilityUiTest` | Seven days, multiple windows, half-hour sliders, disabled days, overlap validation. Saved windows and accepted plan windows intentionally remain separate until acceptance/rebuild. |
| 5. Scheduling | `domain/planner/WeightedWeeklyPlan.kt`, `WeeklyStudyAllocation.kt`, `SessionLocks.kt`, `PlannedStudyBreaks.kt`; `WeightedWeeklyPlanTest`, `TimedStudyPlanTest`, `PlannerBreaksTest` | Deterministic, weighted, deadline-aware, bounded scheduling with locks, caps and optional breaks. Exact-minute sessions differ from the brief's strict half-hour increments. A bounded greedy builder is not a global optimizer; it does not guarantee minimal same-subject repetition. |
| 6. Calendar/editing | `ui/screens/StudyPlannerWeek.kt`, timed move/duration/subject/custom-session dialogs; `TimedStudyMoveUiTest`, `StudyDurationEntryTest`, `SessionSubjectEntryTest`, `CustomSessionEntryTest`, `SessionLocksUiTest` | Seven scrollable days with times, duration, priority and named colors. Accessible edit dialogs replace drag-and-drop. All listed manual actions exist; only locked slots are guaranteed to survive explicit rebuilds. |
| 7. Analytics | `domain/planner/WeeklyPlannerReport.kt`, `ui/screens/PlannerReportCharts.kt`; `WeeklyPlannerReportTest`, `PlannerReportIntegrationTest` | Interactive day, subject and priority bars. Planned totals, target percentage and unused budget remain distinct from credit. Credit follows the ID ledger, not actual study dates; actual activity dates belong in Profile history. |
| 8. Reactivity | `StudyPlannerEntry.kt`, `StudyPlannerWizard.kt`, `StudyPlannerViewModel.kt`; `PlannerWorkflowIntegrationTest`, `WeeklyStudyGoalUiTest` | Preview inputs update the preview; accepted mutations update report/overview. Editing subject/availability settings does **not** immediately regenerate accepted sessions. This deliberately differs from the original live-regeneration requirement. |
| 9. UX/accessibility | Existing Material theme; `PlannerOverviewLayoutTest`, `PlannerReportLayoutTest`, `SessionSubjectLayoutTest` | Narrow/large-font and Android-tablet host tests exist. Full-app keyboard, TalkBack, rotation and system reduced-motion acceptance still require a device. Host theme is dark-only; no desktop/web application or light mode is claimed. |
| 10. Persistence | `planner/StudyPlannerStore.kt`, strict versioned codecs; `StudyPlannerStoreTest`, `PlannerSubjectsTest`, `PlannerWorkflowIntegrationTest` | Local private preferences; atomic snapshot compare-and-swap, legacy reads, explicit recovery, preserved IDs/baselines. Cold model reload is tested, not cross-device synchronization or physical process-death acceptance. |
| 11. Smart feedback | `PlannerWeekOverviewContent.kt`, `WeeklyStudyGoalComponents.kt`, `StudyPlannerWizard.kt`; overview/weekly-allocation tests | Actual shortage, excess, empty/zero target, break/window/cap and lock-conflict explanations exist. No dedicated contextual highest-priority/upcoming-exam recommendation feature yet. |
| 12. Example data | Empty-state flow in `StudyPlannerWizard.kt`; `StudyPlannerUiTest` | **Missing:** initial generated example schedule, four example subjects and 14-hour/16-hour defaults. Existing flow requires user goals; do not inject sample goals into existing accounts. |
| 13. Edge cases | Domain validators, strict codecs and guarded editors; `StudyPlannerTest`, `WeeklyAvailabilityTest`, lock/duration/custom-session tests | Empty/duplicate/invalid input, zero goal, rest days, removed goals, overlap, midnight end and stale saves have automated coverage. Dates are local wall-clock planning values, not time-zone synchronization. Physical restoration remains pending. |
| 14. Quality | Pure domain tests, real-store Compose integration and layout tests; Gradle verification below | Existing tests plus cross-feature workflows verify accounting and preservation. Native host tests are not phone end-to-end tests; no zero-bug or global-optimality claim. |
| 15. Maintainability | Separate `domain/planner`, `planner`, `ui/viewmodel`, `ui/screens` modules | No new dependency, schema, permission, runtime refactor or scheduling-policy change in this verification slice. Historical slice docs record earlier versions; this map describes the current accepted behavior. |
| 16. Definition of done | This audit and the acceptance checklist below | **Open:** product differences above, missing sample/recommendation features and device-only checks prevent a claim that the full original brief is complete. |

## Cross-feature regression contract

`PlannerWorkflowIntegrationTest` uses the real planner entry, view model, codec,
preferences and scheduling/report functions. Only existing goal records are
fixtures; scheduling, persistence, calculations and UI callbacks are not mocked.

- Start with empty planner preferences. Create two linked subjects with different
  priorities and colors, configure availability/rest, create and accept a timed
  weekly-target plan with breaks, then compare overview/report values and reload.
- Begin with partial recorded study, a manual slot and a future locked slot.
  Change subject priority and saved windows through the UI. A conflicting rebuild
  must not write any preferences. Cancel a compatible rebuild before confirming;
  confirm only then, retaining the exact lock and rebasing remaining work against
  recorded credit without double-counting it.
- Compare rendered day/course/priority amounts with the accepted report, assert
  every report axis sums to the same planned total, and verify report navigation
  leaves all saved planner records unchanged. Planning must never start study.
- Cancel then confirm subject removal containing manual and locked slots. Keep
  the other subject's exact block IDs, baseline and partial credit. A stale plan
  save must not resurrect the removed subject or change any saved records.

These tests complement focused mutation/storage tests, not replace them. A new
cross-feature test can pass immediately because the existing behavior is correct;
that is not evidence that a production bug was fixed.

Sensitivity verification: temporarily adding one minute to the overview's
planned total makes all three workflows fail at exact-total assertions. The
mutation is reverted before final verification and is not part of this change.
This verifies a regression guard, not a newly discovered production defect.

## Remaining work, in dependency order

1. **Decide live-update semantics.** Preserve the current safe accepted-plan
   boundary unless explicitly changed. A useful follow-up is a reactive *draft*
   comparison with explicit acceptance, not silently replacing manual sessions.
2. **Clarify scheduling granularity and rebuild policy.** The app supports 15,
   25, 45, 60, 90 and 120-minute focus preferences and exact-minute final blocks;
   availability endpoints use half-hours. Do not force old plans into half-hour
   sessions, or change manual/lock behavior, under a testing-only update.
3. **Contextual recommendations.** Derive upcoming-deadline/high-priority advice
   from accepted versus draft values with actionable, truthful explanations.
4. **Optional example-plan preview.** If retained from the brief, use an explicit
   isolated preview or opt-in initialization. Never create dummy goals, study
   history or sample records on an existing installation without consent.
5. **Device acceptance.** Complete the checks below, then reconcile the remaining
   differences with the brief. Android tablet support is not a desktop release.

The above are follow-up scopes, not newly approved implementation. They must be
specified before changing behavior. This audit must not be used to declare all
requirements passed merely because the regression suite is green.

## Verification commands

PowerShell from the repository root:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:GRADLE_USER_HOME = 'C:\Users\amira\.gradle'
.\gradlew.bat testDebugUnitTest --tests '*PlannerWorkflowIntegrationTest' --no-daemon --max-workers=1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1
```

This is a test/documentation update. It does not introduce new on-device UI, need
a data reset, upload an APK, publish a release or modify CI. The previously
delivered overview APK remains the correct package for visible behavior.

### Local results, 2026-11-02

- Full command succeeded in 2 minutes 51 seconds: 552 tests in 111 suites, with
  zero failures, errors or skipped tests. All three new workflows passed.
- lintDebug: zero errors and 38 pre-existing warnings; none in the new test.
  Debug and unsigned Release builds succeeded. Up-to-date build tasks reuse
  unchanged runtime inputs; no fresh runtime functionality is claimed.
- The first full attempt was stopped after two `AppNotIdleException` failures
  in the unchanged `AccessibilitySemanticsTest`. All 24 cases passed in isolation
  and in the fresh complete run. No test, timeout or assertion was weakened.
  Intermittent idling remains an observed test-environment issue, not a fixed
  product defect or a reason to claim perfect suite reliability.
- The temporary sensitivity mutation was reverted. Production source, build
  configuration, dependencies, schemas and CI have no diff from the base.
- Scoped review found no required change in these tests/docs. All public text is
  English; no credentials or generated build outputs are included. Physical
  checks below remain open. The explicit publication request covers two focused
  commits: workflow regression tests, then the acceptance audit/documentation.
  Remote CI and branch synchronization are verified separately after pushing.

## Physical acceptance still pending

- [ ] Install over the previous app without uninstalling or clearing data; check
  goal progress, history, rewards, plan, subjects, availability and locked slots.
- [ ] Create/edit/cancel drafts and rotate; background/restore and restart the app.
- [ ] Navigate all planner editors, calendar and report with keyboard and TalkBack;
  verify focus order, announced values, error feedback and reachable actions.
- [ ] Check system reduced-motion settings, large text, landscape and tablet layout.
- [ ] Repeat the main workflow offline; confirm a real study session credits the
  correct goal and does not fabricate activity dates in the planner report.

Record device/Android version and observed results before checking these off.
