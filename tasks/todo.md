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

## Weekly availability

- [x] Validated windows and versioned persistence.
  - Verify half-hour bounds, adjacency/overlap, all-rest days, strict JSON,
    unchanged legacy records, atomic conflicts and separate recovery.
  - Foundation regression: 297 tests passed with no failures, errors or skips.
  - Literal catalog bytes remain unchanged on availability-only saves.
  - Existing subject tests now explicitly drain Robolectric's paused main looper
    while awaiting asynchronous loading; assertions and timeout are unchanged.
- [x] Availability editor and persistent state.
  - Verify adding/editing/removing windows, sliders, toggles, errors, saving,
    cancellation, restoration and stale retries with Compose tests.
  - Restored drafts retain their original baseline through asynchronous loading;
    changed saved settings are rejected rather than silently rebasing the draft.
  - Narrow-screen checks at 1.5 font scale verify complete labels and 48 dp
    controls, including an explicitly sized Material slider thumb.
- [x] Planner capacity integration and final checks.
  - Verify explicit wizard/rebuild application and budget ceilings, then run
    full tests, lintDebug, assembleDebug and assembleRelease.
  - Check English text, accessible layouts and document physical-device status.
  - Final regression on 2026-10-18: 314 tests; no failures, errors or skips.
  - Lint: 0 errors, 34 existing warnings. Debug and unsigned Release APKs built.
  - Confirmed rebuild applies saved windows; cancellation and all-rest rejection
    leave accepted sessions unchanged. Budget shifts respect each day's ceiling.
  - No device or emulator connected. Physical upgrade, rotation and offline
    checks remain manual. Individual sessions still do not have clock times.

## Weekly study target and allocation preview

- [x] Deterministic capped allocation with exact totals and shortage reporting.
  - Nine domain tests include 100 generated deadline-prefix scenarios.
- [x] Optional weekly goal persistence with strict v4 and unchanged legacy reads.
  - Three storage tests verify round trips, legacy bytes and malformed schemas.
- [x] Goal editor, live preview, actual weekly totals and restored drafts.
  - Four interaction tests and one native large-font layout test passed.
  - Exact saved minutes survive editing despite rounded hour labels.
- [x] Review, focused tests, full suite, lint and Debug/Release builds.
  - Final verification on 2026-10-19: 331 tests, no failures, errors or skips.
  - Debug and unsigned Release builds succeeded; lint has 0 errors and 34 existing warnings.
  - An initial combined run encountered existing AccessibilitySemanticsTest idle
    timeouts and was stopped. Its unchanged 24-test suite and the final full
    regression passed in fresh workers. No assertions or timeouts were weakened.
  - Native layout verification uses 288 dp content width with 1.5 font scale.
  - No attached device/emulator; installation and physical rotation remain manual.

## Weighted weekly sessions

- [x] Weekly scheduling policy, deterministic generation and deadline reservations.
  - Ten domain tests include 150 generated constraint/accounting scenarios.
- [x] Strict v5 persistence, legacy preservation and safe move/postpone/deletion.
  - Four storage tests cover malformed policies and exact legacy data preservation.
- [x] Wizard preview, confirmed rebuilds and actual weekly shares.
  - Interaction coverage includes legacy upgrade confirmation, zero-goal warnings,
    restored exact-minute goals and actual deadline-reserved shares.
  - Native layout coverage verifies actual shares and frozen limits at 288 dp
    content width with 1.5 font scale.
- [x] Full regression, lint, APK builds and final review.
  - Verified on 2026-10-21: 349 tests, zero failures, errors or skips.
  - Debug and unsigned Release builds succeeded; lint: 0 errors, 34 existing warnings.
  - Reviewed bounds, schema rejection, credit preservation and off-main generation.
  - No new dependencies, permissions, network services or Room schema changes.
  - No device/emulator connected; installation and physical rotation remain manual.

## Timed study sessions

- [x] Exact window packing, validation and strict v6 compatibility.
  - Nine domain tests include 100 generated cases and a 20-course/year-long bound.
  - Three storage tests verify strict records and unchanged date-only reads.
- [x] Safe timed date moves/postponement preserving credit and caps.
- [x] Explicit wizard/rebuild integration and chronological time displays.
  - Three interaction tests and a native 288 dp/1.5-font-scale label check passed.
  - First checkpoint: 365 tests passed; lint: 0 errors, 34 existing warnings;
    assembleDebug succeeded. A new lazy-list test now scrolls to each target
    before comparing ordering. An initial run hit the existing accessibility
    idle timeout; its unchanged suite and the fresh full run passed.
- [x] Explicit-time move editor with validation, cancellation and restoration.
  - Four domain and six interaction tests verify exact slots, date selection,
    invalid clocks, overlap/cap rejection, busy/credit guards and restored drafts.
  - Confirmed-rebuild test verifies new windows only apply on confirmation and
    preserve recorded progress. Native dialog labels and 48 dp actions pass at
    1.5 font scale on a narrow screen.
- [x] Full regression, lint, APKs, review and three local commits.
  - Final check on 2026-10-21: 377 tests, no failures, errors or skipped tests.
  - lintDebug: 0 errors, 34 existing warnings. Debug and unsigned Release built.
  - Reviewed window bounds, strict schemas, overlap/cap guards, restored drafts,
    preserved credit ordering, and explicit acceptance of new clock windows.
  - No dependency, permission, Room, timer/reward or CI changes.
  - No attached phone/emulator; device installation, rotation and TalkBack remain
    manual. Local commits only; no publication or main-branch merge in this slice.

## Session locks

- [x] Domain lock guards, deterministic reserved-slot rebuild and strict v7 storage.
  - Verify credit/order preservation, invalid locks, caps, deadlines and legacy bytes.
- [x] Weekly controls, rebuild and answer-edit integration with clear conflicts.
  - Verify busy states, unlock, blocked moves, cancellation and snapshot protection.
- [x] Scoped review, full suite, lint, Debug/Release builds and device handoff.
  - Verified on 2026-10-24: 396 tests, no failures, errors or skipped tests.
  - New coverage includes 80 generated multi-lock cases, exact slot/ID retention,
    progress rebasing, full reserved days, strict booleans and stale-save guards.
  - Four initial interaction tests failed before UI integration; all now pass,
    with compatible/conflicting previews and narrow-screen native text checks.
  - lintDebug: 0 errors and 34 existing warnings. Debug and unsigned Release built.
  - Reviewed domain invariants, storage compatibility, busy/credit guards and
    atomic rejection of lock conflicts. No dependencies, permissions or CI changes.
  - No attached Android device/emulator; physical upgrade, rotation and TalkBack
    remain manual. Keep the two commits local until publication is requested.

## Session duration editing

- [x] Atomic resize/transfer proposals and strict v8 persistence.
  - Verify exact accounting, untouched credit/locks, bounds and legacy records.
- [x] Weekly duration controls and confirmed preview with restored drafts.
  - Verify growth/shrink, changed/removed donors, busy/conflict and cancel paths.
- [x] Full suite, lint, APKs, scoped review and manual-device handoff.
  - Verified on 2026-10-25: 416 tests; zero failures, errors or skipped tests.
  - 150 generated cases verify exact accounting and lock/credit preservation.
  - Coverage includes same-week cap reservations, 24:00, saved drafts, invalid
    inputs, complete donor removal, real saves and a concurrent-lock conflict.
  - Native narrow-screen text and 48 dp confirmation checks pass at 1.5 font scale.
  - lintDebug: 0 errors, 35 warnings, all located in unchanged files; no warnings
    in the duration feature. SDK/dependency freshness warnings remain separate.
  - Debug and unsigned Release APKs built. No dependencies, permissions, Room or
    CI changes, and no tests or assertions were weakened or disabled.
  - No phone/emulator connected. Physical upgrade, rotation and TalkBack remain
    manual. Keep both commits local until publication is requested.

## Custom study sessions

- [x] Add/remove domain tests and strict v9 compatibility/persistence tests.
  - Red: missing add/remove APIs and manual marker failed compilation.
  - Green: 50 focused custom-session, duration and lock tests passed.
- [x] Domain/storage implementation, focused regression, first local commit.
  - lintDebug and assembleDebug succeeded for the foundation.
- [x] Accessible editor, confirmed removal, draft and conflict coverage.
  - Initial weekly entry tests failed before the new actions were implemented.
  - Add/remove confirmation, restored inputs, date/course selection, active and
    credit guards, real saves and concurrent-snapshot conflicts are covered.
  - Native 320 dp / 1.5-font-scale checks passed for both dialogs.
  - A regression test caught a removal action leaking into date-only plans;
    the action is now confined to timed plans before final full verification.
- [x] Full tests, lint, Debug/Release builds, review and second local commit.
  - Final verification on 2026-10-26: 442 tests, zero failures/errors/skips.
  - lintDebug: 0 errors, 35 existing warnings; none in changed files.
  - Debug and unsigned Release builds succeeded. No new dependencies, permissions,
    Room migrations or changes to timer/reward behavior.
  - An initial full run was stopped on existing accessibility idle failures.
    Its unchanged 24-test suite and the fresh full run passed; no tests,
    assertions or timeouts were weakened or disabled.
  - Reviewed exact credit/ID retention, strict schemas, caps, stale previews and
    guarded saves. No phone/emulator attached; physical upgrade, rotation,
    keyboard and TalkBack remain manual. Keep the two new commits local.

## Timer completion feedback

- [x] Reproduce missing foreground/background completion cues with tests.
  - New feedback APIs first failed compilation; six integration cases then
    failed until the real timer and receiver paths were connected.
- [x] Add bounded themed audio and silent-mode vibration with DND/duplicate guards.
  - 53 focused feedback, notification, break and study-reliability tests passed.
  - Audio fixtures provide duration metadata to Robolectric's real player shadow.
  - Existing break receiver tests now arrange an expired stored break, retaining
    their assertions while excluding premature/cancelled broadcasts.
- [x] Full regression, lint, APK builds, scoped review and one local commit.
  - Final verification on 2026-10-26: 458 tests, zero failures/errors/skips.
  - Debug and unsigned Release builds succeeded. lintDebug: zero errors and
    36 warnings (35 existing; one visible UseKtx advisory for the checked native
    commit result). No new lint suppression or weakened test assertions.
  - Reviewed silent-mode gating, DND restoration, bounded player lifetime,
    duplicate claims and normal VIBRATE permission; reward/storage schemas stay
    unchanged. Phone vibration, lock-screen timing and upgrade remain manual.
    Keep the single commit local until publication is requested.
