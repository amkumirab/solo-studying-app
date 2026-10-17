# Planner subjects

## Scope

Roadmap slice 2 adds a local catalog of up to 20 subjects linked one-to-one to
existing goals. Each subject has a planner-only display name, priority 1 (Low)
to 5 (Critical), a named color, optional preferred weekly minutes and optional
deadline. Names must be nonblank, at most 80 characters, and unique ignoring
case and repeated spaces. A goal can belong to only one catalog entry.

Weekly preferences are positive, up to 168 hours. Blank means unspecified, not
zero. They remain distinct from the goal's total effort and recorded progress.
Weighted weekly allocation and clock-time scheduling belong to later slices;
this slice must not claim those features already work.

## Integration and storage

The subject catalog uses its own versioned `subjects` record in the existing
private planner preferences. Existing plans need no catalog backfill and reads
never write. The accepted plan is still a bounded date-only schedule: an absent
subject deadline is not an invented exam date. The wizard separately asks for a
finish-by date when generating that schedule.

Plan version 3 permits priorities 1–5. Versions 1 and 2 retain their exact 1–3
validation and numeric values, block IDs, order and progress baselines. Retaining
legacy priorities avoids changing existing scheduling decisions on upgrade.

Subject settings seed explicit plan editing/rebuilding. Existing sessions keep
their dates until confirmation. A configured subject with no deadline must not
silently inherit a deadline removed by the user from its underlying goal.
Display names do not rename goals, and deleting a subject does not delete goals,
recorded study, rewards or history. Confirmed deletion removes only that
subject's planned blocks and course, preserving surviving IDs and baselines.
Removing the last planned course clears the accepted plan only.

Plan and catalog mutations use one preferences commit and compare the expected
snapshot under one process-wide lock. Stale saves fail rather than resurrecting
a deleted course. Invalid/future records cannot be silently overwritten. Recovery
requires separate confirmed actions for plan and catalog.

## Interface

Reuse the existing Material theme and controls. Add a Subjects destination
inside the planner, not a new app-wide navigation item. Separate the scrollable
subject list and editor, use visible labels and 48 dp controls, wrap priority
and color choices, and preserve drafts across rotation. Color is a swatch and
named label, never the sole indication of state. Missing/completed goals are
shown honestly; new subjects require an unfinished linked goal.

## Verification

Use JUnit/Robolectric and existing Compose UI tests. Cover creation, validation,
editing, deletion, empty/error states, restoration, safe storage, stale snapshots,
legacy priorities, selected-goal defaults, optional-deadline clearing and credit
preservation. Never skip or weaken existing tests.

Commands (PowerShell, JDK 21):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:GRADLE_USER_HOME = 'C:\Users\amira\.gradle'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon
```

Implementation lives in the existing `domain/planner`, `planner`,
`ui/viewmodel` and `ui/screens` packages, with tests in matching test packages.
Keep domain classes immutable and persistence outside composables. No new
dependencies, permissions, network calls, database changes or fabricated data.
