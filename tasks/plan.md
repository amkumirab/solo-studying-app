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
