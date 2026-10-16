# App organization

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
