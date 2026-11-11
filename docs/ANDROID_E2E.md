# Packaged-app acceptance

This Windows host runner drives the installed app with ADB and Android's native
accessibility hierarchy. It does not substitute composables, inject ViewModels,
seed storage or advance a fake clock. Python 3.12 and its standard library are
used; no Python packages, online accounts or paid services are required.

## Isolation and safety

The `e2e` build type copies Debug configuration but has application ID
`com.amkumirab.solostudying.e2e` and launcher label **Solo Studying E2E**.
Its data, alarms and preferences belong to a different Android UID from the
normal app. Debug/Release application IDs, signing configuration, source logic,
database schema and permissions are unchanged.

The runner refuses an APK without that exact E2E identity. It only targets
`SoloStudying_QA_API36` on `emulator-5580` or `SoloStudying_QA_API37` on
`emulator-5582`, with matching reported API and completed boot. Physical phones
and other AVDs are not supported. `--reset-e2e-data` clears only this disposable
copy; never substitute the normal application ID in these commands.

The identity checks run before installation/reset. UI selection rejects missing,
ambiguous, disabled or off-screen controls instead of guessing coordinates.
Notification permission is denied through the real system dialog; the runner
does not change DND, battery policy, screen pinning or security settings.

See [official build-variant guidance](https://developer.android.com/build/build-variants)
for `initWith`, `applicationIdSuffix` and `matchingFallbacks`.

## Repeat on a dedicated emulator

From the repository root in PowerShell, with Android SDK platform/build-tools
37 installed and an existing QA AVD (see [emulator setup](ANDROID_EMULATOR_QA.md)):

```powershell
py -3.12 -m unittest discover -s scripts -p test_android_e2e.py -v
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleE2e --no-daemon --max-workers=1
.\scripts\Start-AndroidQa.ps1 -ApiLevel 36
# Wait for Android to finish booting, then:
py -3.12 scripts/android_e2e.py --api 36 --reset-e2e-data
```

Use `-ApiLevel 37` / `--api 37` for the other dedicated AVD. `--sdk` overrides
the default `%LOCALAPPDATA%\Android\Sdk` path when needed. A failed step exits
nonzero; `result.json` is not marked passed until the complete journey finishes.
An existing E2E installation is preserved unless the explicit reset flag is used.
Reset it before repeating this clean-onboarding fixture.

## What the journey checks

1. Fresh app launch, notification denial and the normal tutorial bypass.
2. Creation of **E2E Physics**, a Medium goal requiring two focus minutes.
3. Course selection and the guided weekly planner, fit preview and saved plan.
4. Starting study from the plan; pause, full process stop, relaunch with identical
   paused countdown, resume, background/foreground return and natural completion.
5. Goal completion, level-up notices, the two-minute summary and a saved note.
6. Persisted Today total and weekly report after a cold process restart.

Read-only copies of the E2E database (including WAL when present) establish exact
record counts and values: one completed 120-second session linked to the goal,
its note, 150 XP, 80 completion Gold plus 50 level-up Gold, level 2 and 230 total
Gold including the original 100. Another cold launch must not change these rows.
No SQL write is made to the device. SQLite integrity is checked on the host copy.

## Evidence and limits

Every run writes to ignored `app/build/reports/e2e/api*-<timestamp>/`: step XML,
screenshots, final JSON, and E2E-only database snapshots. Failures retain their
current step, exception and best-effort screenshot without overwriting a prior
run's pass. Do not upload databases or local reports as normal repository files.

The host guard/oracle tests run in CI without an emulator. The full real-time
journey is a local emulator test; a green host test alone is not an E2E pass.
This scenario does not establish physical vibration/audio, locked-screen alarm
delivery, reboot recovery, OEM battery behavior, all planner-edit combinations,
older API compatibility or real-phone upgrade safety. Background/foreground
recovery here is not evidence that an alarm was delivered while locked.

## Verified on 2026-11-11

The complete six-step journey passed on AOSP API 36 and API 37. On each, one
120-second session and its note survived full process stops with unchanged
goal, session and profile rows. The weekly report showed 2 planned minutes and
2 credited minutes. Rewards matched the exact oracle above; relaunching did not
duplicate progress or payouts. No production logic fix was needed for this
scenario. Both first and repeated notification-denial dialogs were exercised.

Run evidence:

- API 36: `app/build/reports/e2e/api36-20261111-075301/result.json`
- API 37: `app/build/reports/e2e/api37-20261111-080042/result.json`
- E2E APK SHA-256:
  `275D7B5872FC2AA11B120BDE6A67005FA1E384EB768A6734CF1198224F9B9D60`

All 10 host guard/oracle tests passed, including negative cases for unsupported
devices, duplicate controls/records, disabled ancestors, wrong credit/rewards
and lost notes. The new host test command is included in CI; the changed online
workflow has not been run or published as part of this local implementation.

Gradle verification passed for `testDebugUnitTest lintDebug assembleDebug
assembleRelease assembleDebugAndroidTest`. The unchanged JVM task reused its
up-to-date results: 601 cases in 123 suites, zero failures/errors/skips. Lint
reported zero errors and the same one documented `UseKtx` advisory. The normal
Debug APK SHA-256 remained
`228FADAF417E1D3505ADBB9F8EC87E792F1BDCA49C5528A0F1001CA03C9D7DA6`.
The unsigned Release artifact was built, not installed or store-published.

The existing nine instrumentation cases also passed on each emulator: API 36
in 45.022 seconds and API 37 in 38.340 seconds. Both crash buffers were empty
after verification. The emulators were stopped normally afterward; their data
was retained. These device suites and the full packaged journey are different
checks, not interchangeable measures of coverage.
