# App organization

- [x] Four destinations and contextual session return.
  - Verify labels, selection, callbacks and paused state in Compose tests.
- [x] Today separated from Goals, Focus and Plan.
  - Verify planner and start actions remain reachable.
- [x] Profile sections and separate Settings.
  - Verify visibility, edit actions and saved navigation state.
- [x] Review, full tests, lint, APK builds and automated layout checks.
  - Final verification on 2026-10-12: 233 tests passed, with no failures, errors or skips.
  - Debug and unsigned Release builds succeeded; lint reports 0 errors and 34 warnings.
  - Device visual review is still required; no phone or configured emulator was available.

## Planner time budget

- [x] Pure workload calculation and daily adjustment.
  - Verify deadline-prefix requirements, zero study days, rounding and 15-minute boundaries.
  - Run `testDebugUnitTest --tests '*PlannerTimeBudgetTest'`.
- [x] Interactive preview and safe save.
  - Verify adjustment, reset, rotation, busy states and saving the displayed scenario.
  - Run `testDebugUnitTest --tests '*StudyPlannerUiTest'`.
- [x] Complete verification and user testing handoff.
  - Run full unit tests, lintDebug, assembleDebug and assembleRelease.
  - Review English user-facing text and document actual device-test availability.
  - Verified on 2026-10-14: 251 tests, 0 failures, errors or skips.
  - Debug and unsigned Release APKs built; lint has 0 errors and 34 existing warnings.
  - Native text-layout checks pass at 288 dp card width on a 320 dp screen with 1.5 font scale.
  - No attached Android device; physical-device review remains pending.

## Planner data foundation

- [x] Write compatibility tests using a literal version-1 fixture.
  - Red phase: 13 tests ran; 9 failed against the original storage implementation.
- [x] Implement version-2 storage with lossless reads and overwrite protection.
- [x] Review, run full tests and lint, and build Debug and unsigned Release APKs.
  - Independent review: no required changes; the suggested stale-editor protection test was added.
  - Final verification on 2026-10-16: 267 tests, 0 failures, errors or skips.
  - Debug and unsigned Release APKs built; lint has 0 errors and 34 existing warnings.
  - A new commit-result lint warning was resolved with an explicit return, not suppressed.
  - Publication is split into a behavior-preserving codec extraction and the versioned-storage update.
  - The extraction passed 58 planner regression tests, lintDebug and assembleDebug independently.
- [x] Document verification and remaining physical-device checks.
  - The version-1 fixture and version-2 record retain equal data and study credit.
  - Physical upgrade testing remains pending; no phone installation was performed.

## Planner subjects

- [x] Subject validation, v3 plan compatibility and atomic catalog persistence.
  - Verify legacy1/2 bounds, strict schemas, deletion credit and stale-save guards.
  - Red: new subject contracts were missing. Green: focused storage tests and
    full regression suite passed; lintDebug and assembleDebug succeeded.
- [x] Accessible subject-management UI and persistent state.
  - Verify add/edit/remove, duplicates, missing goals, drafts, error recovery.
  - 25 focused UI tests passed; lintDebug and assembleDebug succeeded.
  - Repeated stale-save retries cannot restore a removed course. Screen-level
    saved-state restoration is covered; real Activity recreation remains manual.
- [x] Planner integration and final checks.
  - Verify catalog defaults, cleared optional deadline, weekly effort distinction,
    color labels and confirmed rebuild. Run full tests, lint and both APK builds.
  - Final regression run: 288 tests, 0 failures, errors or skips on 2026-10-17.
  - Narrow-screen native text-layout checks pass at 288 dp with 1.5 font scale.
  - Existing accessibility tests hit an Espresso idle timeout during the first
    combined run. The isolated 24-test suite and subsequent complete run passed
    without modifying, skipping or disabling any existing test.
  - Lint: 0 errors and 34 existing warnings. Debug and unsigned Release APKs built.
  - No attached Android device/emulator; physical upgrade and Activity recreation
    remain manual checks. Weekly weighted allocation belongs to a later slice.
