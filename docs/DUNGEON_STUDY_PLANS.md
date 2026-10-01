# Dungeon study plans

## Purpose

A dungeon can represent a course that spans many sessions or chapters. Its optional
study plan sets a total time target, a finish date, and selected study weekdays.
For example: Operating Systems, 100 hours, two months from today.

## Behavior

- Plans are managed from Dungeons > Goals. Existing dungeon names can be selected,
  or a new named dungeon can be planned before adding bosses.
- Targets accept hours (including two decimal places), from one minute to 10,000
  hours. Deadlines range from today to ten years ahead; month shortcuts use calendar months.
- Progress is the recorded time of all current bosses with the exact dungeon name,
  including completed bosses. Deleting a boss also removes its contribution.
  Free study and unrelated dungeon sessions do not contribute.
- Remaining effort is divided across selected weekdays, including today and the
  deadline when selected. Estimates round up to whole minutes.
- Completed, on-track, behind-schedule, due-today, overdue, and no-remaining-study-day
  states are distinct. A plan never divides by zero or silently assigns rest days.
- Editing a plan preserves boss progress and the original plan start date. Removing
  it only clears planning settings; it does not delete the dungeon or its bosses.
- Existing users receive an additive database migration with no data reset.

## Verification

Unit tests cover calculations, schedules, invalid input, dates, and completion.
Database tests cover migration, save/edit/clear behavior, and concurrent saves.
Compose tests cover form validation, saving, and accessible progress labels.

Run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`.
On a phone, create a 100-hour plan, choose the two-month shortcut, save it, finish
a session in that dungeon, then check the updated estimate. Edit and remove the
plan and confirm that boss progress remains unchanged.
