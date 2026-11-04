# App organization

## Contextual planner guidance

Approved follow-up: docs/PLANNER_GUIDANCE.md. Extend the same university-planner
roadmap; preserve previous verification records and pending physical checks.

1. Pure bounded advice from real plan/progress/week values; red/green domain
   tests and focused regression. Commit the verified foundation.
2. Reusable themed content, selected-week navigation actions and labelled draft
   advice. Test entry no-write routes, guards, reactivity and restoration.
3. Native narrow/large-font/tablet checks, scoped review, full tests/lint and both
   APK builds. Update docs and deliver three local commits and a named test APK.

Do not push, change existing schedules, add samples or modify CI in this slice.

## Planner change comparison

Contract: docs/PLANNER_CHANGE_COMPARISON.md. Approved follow-up to the acceptance
audit: safe reactive comparison, not silent live regeneration of accepted slots.

1. Test and implement exact slot comparison and a shared read-only rebuild
   proposal, retaining scheduling, lock and progress rules. Verify domain tests.
2. Build shared comparison content and a guarded background rebuild review;
   integrate with entry and answer preview. Test actual saves and cancellation.
3. Verify stale/restored/reactive states, conflicts, incomplete work, keyboard and
   native narrow/tablet layouts. Run the full suite, lint and both APK builds;
   review the scoped diff and deliver local commits and a named test APK.

The explicit publication request organizes delivery into five focused commits:
domain comparison, comparison interface, guarded planner integration, native
layout tests, and documentation. Publish after verifying the unchanged app/test
tree; check remote CI and main-branch synchronization separately after pushing.

Reuse existing controls and persistence. Keep all public text English. No new
sample data, automatic writes, half-hour migration or dependencies.

## Planner workflow verification

Verify the university-planner prompt against the actual Android implementation.
This slice adds cross-feature regression coverage and an evidence-based audit,
not automatic migration, sample-data insertion or a scheduling-policy change.

1. Exercise the real planner entry from an empty store: subject management,
   availability, guided creation, weekly overview, report and cold model reload.
2. Exercise locked sessions, recorded credit, settings changes and explicit
   rebuilds together; reject conflicts without altering accepted-plan bytes.
3. Check the original sixteen requirement groups against source and tests.
   Record adaptations, missing requirements and device-only acceptance separately.
4. Review the diff, run focused/full tests, lint and both APK builds. Publish
   two focused commits, separating workflow tests from the acceptance audit and
   documentation, under the explicit publication request.

Acceptance: exact totals across all report axes and overview; no unintended
study starts; accepted settings remain frozen until confirmed rebuild; locks,
credit and unrelated settings survive reload. Never weaken existing assertions.
Physical-device acceptance and any new product behavior remain separate work.

## Weekly planner overview

Approved next roadmap slice: docs/PLANNER_OVERVIEW.md. Consolidate accepted
weekly metrics without new navigation or writes. Daily ceilings are study
budgets, not guaranteed free slots; breaks and actual study stay distinct.

1. Pure totals/status calculations and boundary/accounting tests; focused
   regression and Debug verification; foundation commit.
2. Shared overview content in Study > Plan and the selected calendar week;
   replace the goal-only summary, retain editor/report flows; interaction tests.
3. Native large-font, tablet and short-viewport checks; separate test commit.
4. Documentation and final verification records. Run the full suite, lint and
   both APK builds before publishing all four commits under the push request.

Completed locally on 2026-11-01. Overview accounting and selected-week day labels
agree; the open action remains visible above the summary. See the contract for
verification and pending physical acceptance. Full original-spec audit is still
later work. The approved publication contains four focused commits; remote CI
results and branch synchronization are verified separately after pushing.

## Weekly planner report

Approved university-planner analytics slice: `docs/PLANNER_WEEKLY_REPORT.md`.
Keep aggregation pure and planning data distinct from credited work. Do not add
activity-date claims, new storage, dependencies or a replacement dashboard.

1. Test and implement exact day/course/priority totals and existing credit-ledger
   attribution; verify planner regressions and Debug build; commit foundation.
2. Build report views from current theme controls with read-only selection,
   empty states and accessible chart labels; test interactions; commit charts.
3. Add a dedicated weekly report route/dialog with previous/next week and return.
   Test restoration, reactive changes and byte-identical saved records. Review,
   run full tests, lint and both APK builds; commit integration and docs locally.

Completed locally on 2026-10-30. See the report contract for verification and
remaining physical checks. This closes the analytics slice, not the consolidated
planner dashboard or final roadmap acceptance. Await a separate push request.

## Planner breaks

Approved next university-planner slice: `docs/PLANNER_BREAKS.md`.
Preserve old plans, credit, locked slots and the existing break timer.

1. Write failing spacing/generation/mutation and strict v10 persistence tests.
2. Implement optional gaps using existing weekly allocation and reserved slots;
   verify domain/storage regression, lint and Debug build; commit foundation.
3. Add a session-step control and truthful preview/week labels. Test saved drafts,
   incomplete-plan confirmation and real saves. Run full tests, lint, both APKs
   and scoped review. Deliver three focused commits for scheduling/storage,
   interface/interactions, and native layout QA/docs. Publish after verification
   when requested; no store release or tracked APK is included.

## Custom study sessions

Approved next slice: `docs/PLANNER_CUSTOM_SESSIONS.md`.
1. Test immutable add/remove proposals, exact credit preservation and strict v9.
2. Add the editor and confirmed removal using the existing snapshot save path.
3. Test UI/restoration/conflicts and narrow layouts, run full checks and APKs.
Keep two new commits local, based on the two unpublished duration commits.

The approved layout contract is in `docs/APP_ORGANIZATION.md`.

1. Four destinations and contextual session return; verify navigation labels.
2. Separate Today and Study; verify section selection and study entry points.
3. Profile sections and Settings; verify visibility, callbacks and restoration.
4. Review, full tests, lint, APK builds and render checks.

Preserve timer, focus warnings, storage and reward logic. Reuse the current theme.

## Planner time budget

Contract: `docs/PLANNER_TIME_BUDGET.md`, based on the approved shared daily-budget
comparison. Keep this additive to the existing wizard and scheduling algorithm.

1. Test and implement deadline-prefix workload analysis and exact budget shifts.
2. Add the analysis card to the wizard's preview; test adjustment, restoration,
   saving and incomplete-plan safeguards.
3. Review the scoped diff, run the complete suite, lint and both APK builds.

Risks: a final surplus can mask an earlier deadline shortage; compare every
deadline. Background previews may be stale while rebuilding; only enable saving
the result matching the current draft. Preserve rest days and storage compatibility.

## Planner data foundation

Approved scope: the first slice of the university planner roadmap. Establish a
versioned storage boundary without changing scheduling, progress, or the UI.
Contract and rationale: `docs/decisions/0001-planner-storage-versions.md`.

1. Add a literal version-1 fixture and failing migration/write-protection tests.
2. Extract the codec, write a version-2 envelope, retain lossless version-1 reads,
   and refuse to overwrite unreadable or unsupported saved records.
3. Review the diff; run focused tests, the full suite, lint and both APK builds.

Preserve block IDs, list order, dates, capacity, priorities and progress baselines.
Upgrade only on explicit save. Future timed sessions, locks and new priority
levels must land with their mutation policies, not as silently discarded fields.

## Planner subjects

Approved scope: subject management from the university planner prompt.
Contract: `docs/PLANNER_SUBJECTS.md`. Preferred hours means weekly study hours.

1. Test and implement the subject model, strict catalog codec, v3 priorities and
   transactional snapshot storage. Verify domain/storage tests; commit foundation.
2. Add the subject list/editor and view-model mutations, with Compose tests for
   validation, restoration, deletion and recovery; commit working management UI.
3. Connect catalog defaults and colors to wizard/week/Today and explicit rebuild.
   Verify full regression suite, lint and both APK builds; commit integration.

Preserve the distinction between weekly preference and remaining lifetime effort.
No automatic relocation of accepted sessions or renumbering of surviving blocks.

## Weekly availability

Approved next capability: Monday-to-Sunday study windows in half-hour increments.
Contract: `docs/PLANNER_AVAILABILITY.md`. Build order: validated windows and
atomic storage → editor and state → capacity integration and final verification.

1. Add window/day/week validation and a strict version-1 codec. Extend snapshots
   and the store additively; prove legacy bytes and stale-save protection.
2. Build the Availability editor and view-model mutations. Verify multiple
   windows, errors, disabled days, cancellation, recovery and restored drafts.
3. Seed wizard capacity and confirmed rebuilds from saved availability. Prevent
   budget previews from exceeding windows; preserve legacy flows without settings.
4. Review, run the full suite, lint and both APK builds; document device limits.

Use three local commits for foundation, working editor and planner integration.
Do not push until requested. Do not assign clock times to date-only sessions or
implement weighted allocation/calendar locking in this slice.

## Weekly study target and allocation preview

Approved next slice: configure a weekly goal and preview recommended hours per
subject using priority, deadline proximity, preferred weekly limits and capacity.

1. Verify pure deterministic capped allocation, exact minute totals and shortages.
2. Persist an optional weekly goal in an additive v4 date-only plan. Continue
   writing v3 for plans without a goal; preserve all legacy reads and credit IDs.
3. Add goal input and reactive recommendations to the guided preview. Show actual
   scheduled hours separately in the weekly view and preserve draft restoration.
4. Run domain, storage and Compose tests, full regression, lint and APK builds.

Recommendations are planning advice; the existing deadline-first date-only
schedule remains visible separately. Timed scheduling and applying weighted
quotas to generated sessions are subsequent roadmap work. No new dependencies.

## Weighted weekly sessions

Approved scope: turn weekly allocations into real date-only blocks after preview
and explicit acceptance. Persist a v5 weekly-scheduling policy with frozen course
limits; v1–4 plans retain exact data and behavior until explicitly rebuilt/edited.

1. Test a pure weekly builder: target ceilings, priority shares, deadline reserves,
   subject limits, partial weeks, zero goals, exact minutes and deterministic IDs.
2. Persist the policy and enforce it in plan validation, Move and No time today.
   Preserve study-credit IDs/baselines and atomic storage conflict guards.
3. Connect wizard and confirmed rebuilds. Show actual weekly shares, truthful
   shortfalls and a clear distinction from old recommendation-only plans.
4. Review, run focused/full tests, lint and Debug/Release builds. Keep commits
   local until publication is requested. Clock times/calendar locking are later.

## Timed study sessions

Approved three-commit slice: `docs/PLANNER_TIMED_SESSIONS.md`.
Freeze saved windows only on explicit acceptance/rebuild; legacy plans stay exact.

1. Pack dated work into local windows with exact totals, validate non-overlap,
   persist strict v6, and preserve credit through moves/postponement.
2. Connect wizard/rebuild and show chronological, readable times in preview,
   week and Today. Without windows keep the existing date-only flow.
3. Add explicit-time move editing, guard invalid/stale slots, verify full tests,
   lint and both APK builds. Keep three commits local; no locks or custom sessions.

## Session locks

Approved next slice: `docs/PLANNER_SESSION_LOCKS.md`. Preserve timed session slots
through explicit rebuilding and answer edits; surface conflicts without saving.

1. Domain/storage tests first, then lock mutations and reserved-slot generation.
2. Weekly controls and guarded rebuild/preview paths with interaction tests.
3. Scoped review, full regression, lint and both APK builds; local commits only.

## Session duration editing

Approved slice: `docs/PLANNER_SESSION_DURATIONS.md`. Growth may take unstudied,
unlocked time from later days of the same course, after preview/confirmation.

1. Test atomic proposals and strict v8 duration markers; preserve legacy records.
2. Add the weekly action and accessible preview dialog; test restoration, credit,
   busy states and cancellation without saving.
3. Review, full regression, lint and both APK builds; keep two commits local.
