# Planner change comparison

## Objective and integration

Let students inspect the effect of changed course settings, capacity and recorded
study before replacing an accepted plan. Reuse the existing scheduling engine,
Material theme, local snapshot store and guarded save path. No paid/cloud service,
dependency, schema, permission, timer or reward change is part of this slice.

Rebuild remaining opens a read-only review instead of immediately rebuilding on
confirmation. Generate the proposal in the background from current saved settings,
today and recorded progress. Editing answers also shows the comparison beside the
existing candidate preview. Creating a first plan keeps its existing flow.

## Contract

- Compare actual course/date/start/duration/lock/manual/edit flags, not generated
  IDs. Match identical slots first, including duplicate date-only slots. Pair
  unmatched slots within each subject in chronological order as **replanned**;
  leftovers are added or removed. Pairs are visual replacements, not credit IDs.
- Show all-plan change counts, explicit manual-slot warnings, selected-week
  accepted/proposed focus and budget, course deltas and exact unscheduled minutes
  before each proposed deadline. Distinguish accepted slots from remaining work.
  Rebuilding can remove past/studied slots; study history is never deleted.
- Preserve exact locked slots using the existing engine. Conflicts show the real
  reason and disable applying. Incomplete rebuilds remain non-applicable; direct
  users to Edit answers for the existing explicit incomplete-plan acknowledgement.
- Cancel/back/close and reading details never write. Only confirm the exact
  currently displayed candidate against its expected snapshot. Changed goals,
  date or settings invalidate old background results. Active study, busy saves,
  loading and unreadable settings block applying. Stale disk writes still fail.
- Restore navigation/disclosures, recompute candidates after restoration, and
  never restore an old pending save. Paginate changed-session details (20 rows)
  rather than composing up to 18,000 rows. Match with bounded maps/queues, not
  quadratic all-pairs comparison.

## Interface and code style

Keep the current dark Material theme, 8/12/16 dp spacing, readable stacked text,
48 dp controls and visible labels. Apply/cancel stay reachable outside the scroll
area. No new app-wide destination, palette, icon system or chart library.
Domain logic stays in `domain/planner`; UI in small `ui/screens` components;
tests use matching packages and existing JUnit/Robolectric/Compose conventions.

```kotlin
val result = previewStudyRebuild(snapshot, bosses, today)
val comparison = result.plan?.let { compareStudyPlans(accepted, it) }
```

## Verification and delivery

Test exact/renumbered slots, metadata changes, additions/removals, cross-week
replacement, generated invariants, legacy plans, locks and recorded progress.
Test real saves, cancellation, stale snapshots, changed inputs, restoration,
incomplete/conflicting proposals, keyboard access and large-font/tablet layouts.
Never weaken existing tests, suppress errors or silently change scheduling policy.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:GRADLE_USER_HOME = 'C:\Users\amira\.gradle'
.\gradlew.bat testDebugUnitTest --tests '*StudyPlanComparisonTest' --no-daemon --max-workers=1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1
```

Keep focused commits local until publication is requested. Deliver a uniquely
named debug APK for phone acceptance; do not clear user data or claim physical
TalkBack, rotation or upgrade testing without an attached device.

## Framework references

The project retains Compose BOM 2024.09.00, Kotlin 2.2.10, Activity Compose 1.10.1
and its existing coroutines. No framework upgrade is needed for this feature.
Input-keyed producers are cancelled/restarted with changed inputs, and cancelled
when their composition leaves. A separate input-identity check also rejects an
older result before applying: [Compose effect lifecycle](https://developer.android.com/develop/ui/compose/side-effects#producestate).

Only small navigation/disclosure/page values use saved UI state. Plans and
pending saves are not put in the saved-state Bundle; proposals are recomputed
from current persisted settings after restoration:
[Compose UI state saving](https://developer.android.com/develop/ui/compose/state-saving).

## Phone smoke test

Install the named Debug package over the existing app, without uninstalling or
clearing data. In Study → Plan, open the weekly plan. Save a changed subject
priority or availability window, return to Schedule and choose Rebuild remaining.
Inspect current/proposed totals, finish-date shortfalls and session changes.
Cancel and verify the accepted calendar stays exact; reopen and confirm a valid
proposal. Check a compatible locked slot stays fixed and an incompatible one
blocks applying. Review manual-slot warnings before replacing customization.

Edit answers and change the weekly goal; check the comparison updates before
saving. Rotate with an open review/draft, restart, inspect large text, keyboard
and TalkBack. Test offline and check prior goal progress/history/rewards remain.
Physical checks remain pending until their device and results are recorded.

## Local verification, 2026-11-03

- The first domain/UI API tests failed on missing functions, and three real-entry
  tests failed before integration. Six domain, six component, six real-entry and
  three native-layout tests now pass: 21 new tests, including 80 generated domain
  comparisons, real persistence, stale writes, changed progress and cold reload.
- The complete command passed in 5 minutes 24 seconds: **573 tests in 115 suites,
  zero failures, errors or skips**. Debug and unsigned Release builds succeeded.
  lintDebug reports zero errors and the same 38 pre-existing warnings; none in
  changed feature files. No dependency, schema, permission or CI change was made.
- Red cases caught hidden edit flags on manual slots and a recovery link that
  failed to leave review. Direct semantic OnClick cases also exposed callbacks
  bypassing disabled visuals. The callback now requires an applicable candidate
  for both ordinary and accessibility activation. All cases pass without relaxed
  timeouts, removed assertions, skipped tests or new lint suppressions.
- Existing rebuild tests wait for the new read-only preview and cancel review
  before changing sections. Data, credit and lock assertions are retained; invalid
  proposals explicitly assert that confirmation is disabled.
- Native PNGs under `app/build/reports/planner-comparison` were visually reviewed
  for 320 dp/1.5-font-scale, 800 dp tablet and 360 dp-high views. Full text/layout,
  48 dp controls, keyboard disclosure and paged restoration tests passed. The
  grey area below the short test viewport is the host window, not app content.
- Scoped review covered correctness, interface clarity, storage boundaries,
  bounded work and dependency risk. The existing allocator, lock builder, codecs,
  view-model store, timer and reward rules remain unchanged.
- The approved publication is organized into five focused commits: domain
  comparison/proposal, comparison interface, guarded planner integration, native
  layout verification, and documentation. Application and test files remain
  unchanged when splitting the unpublished history. Remote CI and main-branch
  synchronization are verified separately after pushing; no store release or
  APK upload is implied. The entire university-planner roadmap is **not** declared
  complete.

### Test package

Ignored local build output (not repository content):

`app/build/outputs/apk/debug/solo-studying-planner-comparison-20261103-debug.apk`

- Size: 20,577,874 bytes.
- SHA-256: `004814782766E3AD2CC6EC18C64E10BC5AC51543077BA2130BE83CA3971FE3C2`.
- Application ID: `com.amkumirab.solostudying`; version 1.0 (code 1), min SDK 24,
  target SDK 36. APK Signature Scheme v2 verified.
- Debug certificate SHA-256:
  `dc57434a4949a016ea3cddfc1e184412d530add37b926c48d73aaff25c62b824`, matching
  the previous overview test package. Install over it; do not uninstall first.
- No phone or emulator is attached. Physical upgrade/data retention, rotation,
  TalkBack and actual phone performance still need the smoke test above.
