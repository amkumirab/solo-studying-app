# App organization

## Objective

Make study actions easy to find without removing features or changing stored data.
Approved structure: Today, Study, Rewards and Profile. Settings is a separate
destination. An active study session remains reachable outside its timer screen.
Keep the existing dark theme, readable text and restrained cyan accents.

## Layout contract

- Today: daily progress, planned sessions and a compact entry to daily tasks.
- Study: Goals, Focus and Plan. Today's accepted sessions remain on Today.
  Recovery challenge details are expandable; an active penalty stays visible.
- Rewards: existing inventory and shop, with shorter headings.
- Profile: Overview, Skills and History, displayed separately rather than stacked.
- Settings: schedule, reminders, sound, breaks, focus shield and tutorial.
- Session: contextual, not a permanent bottom tab. A return bar shows its state.
- Navigation and settings controls: at least 48 dp, readable at narrow widths
  and larger font settings. No new palette or imagery.
- Tab, section and scroll state survive navigation and recreation. Leaving an
  active session retains the existing warning. Back exits Settings before home
  and never silently ends a session.
  As in previous builds, recreation while a session is active reopens the timer;
  other saved tabs and sections remain available when you navigate back to them.

## Structure and style

Use existing Material 3 controls and theme tokens. Keep navigation presentation
in focused files under `ui/screens`. Screens own state and pass data/callbacks
to sections. Example: `ActiveSessionBar(paused, onReturn, modifier)`.
No change to rewards, scheduling logic, stored data, permissions or dependencies.
Study and Profile keep separate saved-state scopes for each section so returning
to a section restores its scroll position and expanded details.

## Tests and commands

Robolectric Compose tests under `app/src/test/java/.../ui/screens` cover labels,
selection, callbacks, settings isolation, restoration and session return.
Check narrow and large-font layouts. Use Android Studio JBR as JAVA_HOME.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon
```

Always run focused tests per slice and the full suite before handoff. Never
disable failing tests or change user data to simplify UI. Ask before adding
dependencies, changing storage or publishing. Phone testing is separate.

## Phone checks before publication

Install the debug APK as an update; do not uninstall or clear app data to test navigation.

1. Open Today, expand daily tasks, then return after visiting another tab.
2. Open Study and switch between Goals, Focus and Plan. Check existing courses
   and deadlines, and open the weekly planner.
3. Start a focus session, cancel the leave-screen warning once, then confirm it.
   Return through the session bar. Pause and repeat; the paused timer must stay paused.
4. Open Profile, visit Skills and History, and check recorded session notes.
5. Open Settings with the gear icon; check schedule, reminders, sound, and focus controls.
   Back should return to Profile without ending an active session.
6. Repeat navigation with a larger system font. Check for clipped text,
   overlapping actions, readable contrast, and reachable controls.

Keep historical README screenshots labeled as older builds until new device
captures are available. Automated layout tests do not replace device visual review.

## Verification record: 2026-10-10

- Two final full-suite executions passed: 232 tests, zero failures, errors or skips.
- `lintDebug` passed with zero errors and 34 warnings; warnings remain for future cleanup.
- `assembleDebug` and `assembleRelease` succeeded. The Release APK is unsigned.
- Tests exercise navigation, section restoration, daily-task expansion, large-font
  reachability, skill creation, planner loading, and active/paused session return.
- Planner creation uses the current lifecycle owner's CreationExtras. A regression
  assertion checks the actual Application and saved plan, after a stale cached
  default factory was found during combined test execution.
- One intermediate Robolectric execution stalled during UI idling and was stopped.
  Focused tests and both final full runs passed; no tests were disabled and no
  wait limits were increased. Revisit test-scope isolation if the stall recurs.
- No physical device was connected and no configured emulator was available.
  Device screenshots and visual review remain pending; older README images stay labeled.
- No database schema, permissions, dependencies, or reward/scheduling logic changed.

## Final review: 2026-10-12

- Fixed per-section Study state restoration after reproducing the reset in a
  regression test. Goals retains its scroll position and expanded recovery
  details when switching sections and after recreation.
- Full suite passed: 233 tests, zero failures, errors or skips.
- Debug and unsigned Release builds passed; lint reports zero errors and 34 warnings.
- Review found no remaining blocking issue in the navigation changes.
- Public text is English. No credentials, build artifacts, or machine-local
  configuration are included. Existing license and third-party notices are preserved.
- Phone visual review remains separate; no Android device was connected.
