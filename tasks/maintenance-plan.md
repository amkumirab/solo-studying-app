# Build and lint maintenance

Approved on 2026-11-06: remove verified unused resources/context parameters and
perform a broad dependency/toolchain upgrade, not just a narrow KSP patch.
Keep the original planner acceptance tasks unchanged. Tasks are tracked in
`tasks/maintenance-todo.md` by explicit user choice.

## Contract

- Fix warning causes; do not add lint suppressions, baselines, skipped tests,
  relaxed assertions or weaker quality thresholds to obtain a clean result.
- Preserve application ID, signing identity, minSdk 24, saved data, Room schema,
  planner accounting, timer completion behavior and user device permissions.
- Use stable official releases and verify compatibility before each upgrade.
  Keep toolchain, platform libraries and UI changes in independently verifiable
  slices; record exceptions rather than silently changing product behavior.
- Do not publish a store release. Remote publication needs an explicit request.

## Ordered implementation

1. Reproduce retained Contexts; normalize the three used ViewModel contexts.
   Verify focused retention/gameplay tests on the existing stack.
2. Remove approved unused Context arguments and seven template colors; remove
   redundant activity label. Verify compilation/resource tests.
3. Adopt KTX helpers while preserving synchronous commit return values. Verify
   timer, feedback and tutorial tests, especially failed-save guards.
4. Upgrade KSP, verifying the documented 2.3.5 upstream exception fix and real
   generated Room implementations. No log filtering or fallback to KSP1.
5. Upgrade AGP/Gradle/Kotlin and compile/target SDK as a compatible stable set.
   Verify project configuration and compilation before library upgrades.
6. Upgrade AndroidX/Compose/coroutines/test libraries in compatible groups and
   migrate actual removed/deprecated APIs. Verify UI and lifecycle regressions.
7. Upgrade Room without a schema change; select only skill columns in the join,
   verify relationships and data retention. No destructive migration shortcut.
8. Update supported GitHub Actions/runtime versions and document build choices.
9. Full JVM tests, lint, Debug/Release/test APK builds, emulator acceptance and
   packaged-app upgrade smoke test. Record counts, warnings and limits honestly.

## Risks

Major UI/toolchain versions may change APIs, test scheduling and SDK behavior.
Keep changes small, reproduce failures, and do not assume compilation proves
screen behavior or database retention. API 36 emulator checks do not establish
API 37 device compatibility; report that distinction explicitly.
