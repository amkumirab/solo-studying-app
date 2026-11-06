# Android emulator acceptance environment

The dedicated AVD `SoloStudying_QA_API36` runs AOSP Android 16/API 36 x86_64.
It uses the existing Android SDK, WHPX, four virtual CPU cores, 4 GB guest RAM
and software graphics. No Google account or Play service is required. AVD data
stays in the standard local Android AVD directory, outside this repository.
No real phone is targeted and no Windows security settings are changed.

## Start and repeat

From the repository directory in PowerShell:

```powershell
.\scripts\Start-AndroidQa.ps1
# Or, when the emulator is stopped, show a normal desktop emulator window:
.\scripts\Start-AndroidQa.ps1 -ShowWindow

$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug assembleDebugAndroidTest --no-daemon --max-workers=1
.\scripts\Invoke-AndroidQa.ps1
```

The test command refuses other AVDs, installs both APKs on `emulator-5580`, and
records the instrumentation result under ignored `app/build/reports/emulator-qa`.
It never clears the production app's data. Planner fixtures use a dedicated
`device_acceptance_` preferences namespace, including all ViewModel writes.
The fixture namespace alone is reset between scenarios. Keep these fixtures out
of production initialization and do not interpret synthetic study as real study.

The instrumented suite exercises real Android Compose screens and local storage:
subject creation, priorities/colors, availability/rest days, guided timed plan
creation, weekly goals/breaks, exact reports, cold ViewModel reload, partial credit,
lock conflicts, cancelled/confirmed rebuilding, subject removal and stale saves.
This component-level Android test is not the same as navigating every screen in
the packaged app; record full-app exploratory results separately.

Stop only this AVD using:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5580 emu kill
```

Do not use `-wipe-data`, uninstall the user's real app or clear its storage.
Snapshot loading is disabled; the AVD's persistent user-data partition is retained.
System-image install, when absent:

```powershell
sdkmanager --install 'system-images;android-36;default;x86_64'
'no' | avdmanager create avd --name SoloStudying_QA_API36 --package 'system-images;android-36;default;x86_64' --device pixel_5
```

## Verified run: 2026-11-06

Production baseline: `f69c858dd116f89172e0e332769e8337d32c06d3`.
The installed debug APK SHA-256 was
`35A96170934137CEB916B89505159901C573BA4AF261D1C4CD7DFD52C4CC4BE8`.

| Check | Observed result |
| --- | --- |
| Emulator boot and packaged app installation | Passed on Android 16/API 36 |
| Instrumented Android suite | All 8 tests passed in 45.612 seconds |
| Planner fixture isolation | Normal planner preferences unchanged across the final suite; isolation regression test passed |
| `assembleDebugAndroidTest` | Passed |
| `lintDebug` | Passed; 0 errors, 38 existing warnings |
| Notification permission denied | Onboarding and focus flow remained usable |
| Real app focus profile | Created and started a one-minute single-session profile through the UI |
| Natural timer completion | Summary displayed 1m 0s, +1 XP, +0 Gold and a one-day streak |
| App process restart after completion | Today still showed 1 of 45 minutes studied, with 44 minutes remaining |
| Crash buffer during exploratory run | No crash entries observed |

The seven new device tests cover planner workflow (3), guidance navigation (3),
and fixture-storage isolation (1). The eighth is the existing application-context
test. Existing JVM tests were not rerun as part of this emulator setup.

Early fixture trials on this disposable AVD exposed a test-wrapper context
escape into the normal planner namespace. The wrapper now returns itself from
`applicationContext`, and the regression test checks both that identity and
unchanged normal preferences. Only synthetic data in this AVD was affected;
no physical phone was connected or targeted. This AVD is therefore a QA sandbox,
not a clean-install or real-user-data baseline.

### Status-bar contrast: original finding

On this AVD, the system clock and status icons render dark against the app's
dark status-bar background. Reproduce by launching the app, completing or
skipping onboarding, and opening Today. Expected: clearly readable system
icons. Actual: poor contrast. Evidence:
`app/build/reports/emulator-qa/after-relaunch.png`.
This is an observed Android 16 emulator issue, not yet a cross-device finding.
The original environment-setup run did not modify production UI.

### System-bar fix

The Compose theme is always dark, but both activities previously called
`enableEdgeToEdge()` with automatic system-theme detection. In Android's light
mode this selected dark system icons against the app's dark background.
Both activities now use `enableSoloStudyingEdgeToEdge`: light icons over a
transparent status bar, with a dark app-surface navigation scrim for older
Android versions. This does not change the device's theme or user preferences.
See [Android edge-to-edge guidance](https://developer.android.com/develop/ui/views/layout/edge-to-edge).

The alarm-screen regression test reproduced the problem before the fix on
API 26 and API 36. `SystemBarsDeviceTest` checks the real main activity's icon
appearance on launch and after activity recreation. It can be run under either
emulator system theme:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s emulator-5580 shell cmd uimode night no
# After building and installing both APKs using Invoke-AndroidQa.ps1:
& $adb -s emulator-5580 shell am instrument -w -r -e class com.amkumirab.solostudying.SystemBarsDeviceTest com.amkumirab.solostudying.test/androidx.test.runner.AndroidJUnitRunner
& $adb -s emulator-5580 shell cmd uimode night yes
& $adb -s emulator-5580 shell am instrument -w -r -e class com.amkumirab.solostudying.SystemBarsDeviceTest com.amkumirab.solostudying.test/androidx.test.runner.AndroidJUnitRunner
# Restore the emulator's original setting (the QA baseline used "no").
& $adb -s emulator-5580 shell cmd uimode night no
```

Device verification on 2026-11-06:

- The new main-activity test failed on the original installed APK with
  `Dark app needs light status-bar icons` before the replacement APK was installed.
- All 9 device tests passed on the replacement APK in 55.368 seconds under the
  emulator's light system theme. The icon test also passed under the dark system
  theme in 7.989 seconds, including activity recreation. The emulator was then
  restored to its original light system setting.
- Visual inspection of the full Today screen confirmed readable white status
  icons and white navigation controls over a dark navigation background.
  Evidence: `app/build/reports/emulator-qa/bars-light-after.png`; original:
  `app/build/reports/emulator-qa/bars-before.png`.
- The replacement APK is `solo-studying-system-bars-20261106-debug.apk` in the
  debug output directory. SHA-256:
  `F559B233CD4D579143CF21B19587D4D9C3163C6E28AB8659AABD0F1CB180B8C6`.

The initial full JVM run was stopped after repeated Compose idling timeouts.
The new alarm-window test now checks styling at `onCreate`, without unnecessarily
rendering a Compose hierarchy outside a Compose test rule. Its assertions were
retained. The 6 alarm-activity cases and 24 accessibility cases then passed
together. No test was disabled or had its timeout increased.

Final verification passed with `assembleDebug assembleDebugAndroidTest lintDebug
testDebugUnitTest`: 595 JVM tests across 119 suites, with zero failures, errors
or skipped tests. Lint reported zero errors and the same 38 existing warnings.
The installed APK hash matched the named artifact above. The artifact is a
debug build for local testing, not a signed store release.

### Still pending

Full packaged-app acceptance across all screens, older Android API levels,
large fonts/TalkBack, widgets, strict-focus permissions, timer recovery after
reboot, background/locked-screen completion alerts, release-build behavior,
and real phone upgrade/data-retention checks have not been established by this
run. Do not treat the one-minute foreground timer check as alarm-delivery proof.

## Limits

One API level is not compatibility testing across supported Android versions.
Physical vibration strength, real audio output, OEM power management, real-device
lock-screen delivery and actual phone upgrades remain separate acceptance checks.
Do not mark the entire application tested merely because this suite passes.
Official reference: [Emulator command line](https://developer.android.com/studio/run/emulator-commandline).
