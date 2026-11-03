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
