# Managing study goals

In **Study > Goals**, use the pencil on a goal to edit its name, difficulty,
deadline, or unfinished study target. If you change the target, it must be above
the time already studied. A completed goal keeps its original target and progress.

To add more study after completing a goal, choose **Open Second Gate** and enter
only the extra minutes. For example, after completing 180 minutes, enter 60 to
add a one-hour goal inside the original card. The original goal remains completed
and the gate starts at zero. Each original goal can have one Second Gate.

Use the trash button to move a goal to **Deleted goals**. Restore brings back the
same goal, including its progress, study steps and links to existing sessions.
Deleting an original goal hides its gate until the original is restored. A gate
deleted on its own must also be restored on its own. Finish the active timer
before editing or removing goals. This cannot recover goals permanently removed
by older app versions.

## Storage and upgrade

Room migration 12 → 13 adds nullable archive and parent-link columns to
`bosses` and a unique index for the gate link. It does not replace existing
records or modify earned study credit. A gate tracks its own new sessions and
does not replay the original goal's rewards. Timer updates write progress
fields only, so they do not overwrite goal edits or archive metadata.
See [Room's database migration guide](https://developer.android.com/training/data-storage/room/migrating-db-versions).

Install the new APK over the existing app with the same signing key. Do not
uninstall or clear data to upgrade. An older build cannot safely open the newer
database; do not install an older APK over it.

Local test build: `solo-studying-goal-management-20261124-debug.apk`.
SHA-256: `64f2f1b7f382a8a388a869b7cff145a2da67b7298854dd367e62c678e9a7b785`.

## Verification

The full local JVM suite passed: 611 tests in 124 suites, with no failures,
errors, or skips. The ten packaged-app runner guard tests also passed.
The debug APK built successfully. Lint reported no errors and seven advisories:
six dependency-update notices and the existing SharedPreferences KTX advisory.

Repository tests cover exact time and session retention, validation of edits,
concurrent gate creation, archive/restore of linked goals, and opening populated
version-11 and version-12 fixtures through Room's schema validation.

The API 36 test installation upgraded from schema 12 to 13 in place. In the
packaged app, a one-minute goal finished normally; its Second Gate appeared
under the completed card. The gate's target changed from two to three minutes.
After archiving and restoring the original, a cold database read confirmed the
same goal ID, 60 credited seconds, the original session, and the edited gate.

The same user journey can be rerun on the dedicated API 36 emulator:

```powershell
.\gradlew.bat assembleE2e
.\scripts\Start-AndroidQa.ps1 -ApiLevel 36
# Wait for the emulator to finish booting.
py -3.12 scripts/android_dungeon_e2e.py --api 36 --reset-e2e-data
```

The runner verifies APK and emulator identity before resetting only the
disposable E2E installation. It does not touch the normal app or any phone.
