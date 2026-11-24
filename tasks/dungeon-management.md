# Dungeon management

A study goal is the card shown in Study > Goals. Realm categories and scheduled
study plans keep their existing behavior.

- [x] Deleted goals: keep the original row, progress, study steps, and session
  links. Hide archived goals from active selection and restore the same ID.
- [x] Editing: change the name, difficulty, deadline, and an unfinished goal's
  target without losing credited seconds. Completed targets stay in history.
- [x] Second Gate: give a completed goal one linked follow-up with its own
  additional minutes and progress. Display it inside the parent card. Existing
  completion credit and the original target are unchanged.
- [x] Protect active timers, verify upgrading a populated version-12 database,
  run regression checks and deliver a local test APK.

Use additive Room columns and preserve the existing signing identity. Archiving
a parent hides its Second Gate until the parent is restored; a separately
archived gate remains in Deleted goals. There is no permanent-delete action.
These changes cannot recover goals removed by older versions.

See [goal management verification](../docs/DUNGEON_MANAGEMENT.md#verification).
