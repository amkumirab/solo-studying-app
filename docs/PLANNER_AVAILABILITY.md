# Weekly availability

## Objective and scope

Roadmap slice 3 adds editable local study windows for Monday through Sunday.
Each day can be enabled or disabled and contain multiple non-overlapping windows.
Times use 30-minute increments. Show daily and weekly capacity immediately.
Keep the current planner's 8-hour daily limit, with at most 16 windows per day.
All seven days may be disabled; explain that no schedule can then be generated.

Start is inclusive and end is exclusive. 00:00 is a valid start and 24:00 is a
valid end of that same day. Reject overnight windows; split those across days.
Adjacent windows are valid. Disabled days retain their windows but contribute
zero capacity. Times are local wall-clock preferences, not UTC timestamps or
promises about elapsed time across daylight-saving transitions.

This slice stores clock windows and applies their capacity to explicit schedule
editing/rebuilding. It does not assign start/end times to individual sessions.
Weighted allocation, timed calendars and locked sessions belong to later slices.
Accepted date-only plans keep their exact sessions, credit IDs and baselines until
the user explicitly edits or confirms a rebuild. No changes to goals or history.

## Storage and contracts

Use a separate version-1 `availability` record in private planner preferences.
Extend `PlannerSnapshot` additively so plan, subjects and availability share the
existing expected-snapshot mutation guard. Corrupt/future records block writes;
confirmed availability recovery clears only that record. Never migrate on read
or rewrite legacy plan bytes to invent clock times. Existing plan codecs stay v3.

Restored editors wait for storage to load. The availability draft retains a
SHA-256 identity of its original expected snapshot across recreation. If saved
settings changed while it was closed, saving is rejected with an explicit
cancel/reopen instruction rather than silently rebasing the old draft.

For a first unsaved editor draft, suggest clock windows using the existing daily
minutes rounded down to half hours. Explain that suggested times require review.
Do not change saved daily budgets merely by opening the screen.

Saved windows seed wizard capacity and explicit rebuilds. Budget comparisons may
reduce the daily study budget but must never increase it past a saved day's
window capacity. With no availability record, retain all legacy behavior.

## Interface and code style

Add Availability inside the current planner, not a new app-wide destination.
Use the existing Material theme, scrollable day cards, labelled 48 dp controls,
half-hour duration sliders and an accessible start/end editor. Show overlap and
range errors, preserve drafts across restoration, and require explicit saving.
Example domain style: `data class StudyTimeWindow(val startMinute: Int, val endMinute: Int)`.
Keep domain calculations in `domain/planner`, codecs in `planner`, persistence
in the existing store/view model, and small UI components in `ui/screens`.

## Boundaries and verification

Always validate bounded integer inputs and strict versioned JSON. Treat form
values and stored JSON as untrusted. Data remains local; do not log schedules.
Ask before adding dependencies, permissions, database schema changes or CI edits.
Never skip tests, discard unreadable data silently, alter rewards/history, or
claim date-only blocks already have scheduled clock times.

Use JUnit/Robolectric and Compose tests beside existing tests. Cover adjacency,
overlap, midnight, half-hour rounding, disabled/all-rest days, totals, strict JSON,
legacy bytes, atomic conflicts/retries, recovery, save/cancel/restoration, wizard
and rebuild capacity, budget ceilings and larger-font layouts. Device rotation,
restart and airplane-mode checks remain manual when no device is attached.

## Component references

- [Compose sliders](https://developer.android.com/develop/ui/compose/components/slider):
  controlled value and selectable endpoints; interior steps select half hours.
- [Compose state saving](https://developer.android.com/develop/ui/compose/state-saving):
  saveable form strings retain incomplete drafts across recreation. Store only a
  compact baseline identity, not the entire accepted plan, in the saved UI state.

## Verification record

On 2026-10-18, the full local regression suite passed: 314 tests, zero failures,
errors or skipped tests. Coverage includes strict storage validation, unchanged
legacy records, stale saves, draft restoration during asynchronous loading,
window editing, large-font layouts, budget ceilings and confirmed rebuilds.
`lintDebug` completed with zero errors and 34 existing warnings. Both Debug and
unsigned Release APKs were built successfully.
There was no connected Android device or emulator; installation, rotation and
airplane-mode checks on a physical device remain manual verification steps.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:GRADLE_USER_HOME = 'C:\Users\amira\.gradle'
.\gradlew.bat testDebugUnitTest --tests '*Availability*' --no-daemon
.\gradlew.bat testDebugUnitTest --no-daemon
.\gradlew.bat lintDebug assembleDebug assembleRelease --no-daemon
```
