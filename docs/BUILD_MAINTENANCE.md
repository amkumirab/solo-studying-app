# Android build maintenance

## Stable toolchain alignment (2026-11-06)

The build uses AGP 9.5.1, Gradle 9.8.2, Kotlin/Compose compiler 2.4.21 and
KSP 2.3.13, with compile/target SDK 37. The minimum supported API remains 24.
Application ID, version code, signing setup and Room schema version 12 are
unchanged. This is maintenance, not a store release.

The Gradle wrapper was regenerated and checked against the official wrapper
and distribution SHA-256 values. The distribution checksum is committed in
`gradle/wrapper/gradle-wrapper.properties`.

Related runtime upgrades were verified in separate increments: AndroidX
Core 1.19.1, Activity 1.13.0, Lifecycle 2.11.0, Compose BOM 2026.09.00,
coroutines 1.11.0 and Room 2.8.5. Tests use AndroidX Test 1.7.0 and
Robolectric 4.17. Unchanged JUnit, Espresso and desugaring versions were already
the stable versions listed by their official repositories at verification time.

## Reasons and compatibility safeguards

- KSP 2.3.5 could log a nonfatal application-service/AWT exception on Linux CI.
  The upstream fix shipped in 2.3.6; the selected stable release includes it.
  Generation still uses KSP2, with no output filtering or ignored failures.
- Long-lived ViewModels retain only the application Context. Unused constructor
  Contexts and the seven approved unused template colors were removed.
- Room joins select only the fields mapped to `SkillEntity`. Reopen and migration
  tests cover persisted records. The explicit development-only destructive
  fallback argument retains the old behavior; no production migration was removed.
- Checked synchronous preference writes still return `commit()` success/failure.
  They do not switch to asynchronous writes or hide failed persistence.
  One `UseKtx` advisory remains on the shared checked-edit helper: Core KTX's
  `edit` returns `Unit`, so replacing it would discard the persistence result.
  The helper has three consumers and tests for success, failure and an exception
  before committing. This advisory is retained, not suppressed or baselined.
- Compose number formatting observes `LocalLocale`; changing the device language
  invalidates the displayed numbers. Primitive state and modifier signatures
  follow the updated APIs.
- Compose tests use the v2 JUnit factory. Robolectric 4.17 needs access to
  `jdk.internal.access` on JDK 17+: the documented module opening is scoped to
  local unit-test worker JVMs, not the application or the operating system.
- CI keeps read-only repository permissions and JDK 21. Actions use Node 24
  runtimes, and Ubuntu 24.04 is explicit rather than a moving runner label.

No lint baseline, new suppression, disabled test, relaxed assertion or extended
timeout was introduced to obtain a successful result. Older physical-device
acceptance tasks remain separate in the original task files.

## References

- [AGP 9.5 compatibility](https://developer.android.com/build/releases/agp-9-5-0-release-notes)
- [Gradle wrapper verification](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- [KSP upstream issue 2763](https://github.com/google/ksp/issues/2763)
- [KSP fix in 2.3.6](https://github.com/google/ksp/releases/tag/2.3.6)
- [KSP 2.3.13](https://github.com/google/ksp/releases/tag/2.3.13)
- [Robolectric setup](https://robolectric.org/getting-started/)
- [Robolectric 4.17 release](https://github.com/robolectric/robolectric/releases/tag/robolectric-4.17)

Final local and device results are recorded in the maintenance checklist and
the emulator QA document, including their explicit limits.
