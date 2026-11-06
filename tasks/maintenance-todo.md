# Build and lint maintenance checklist

- [x] Reproduce Context retention; fix used ViewModels and verify focused tests.
  - Red retention case failed on the original code; all 38 retention/gameplay/
    focus-reliability cases passed after normalization. No assertion was weakened.
- [x] Remove approved unused Context arguments/colors and redundant label.
  - Removed only the seven template colors and two unused constructor arguments
    approved by the user. Lint and compilation passed on the original stack.
- [ ] Migrate KTX suggestions without changing commit semantics.
- [x] Upgrade and verify KSP against the documented upstream exception.
  - KSP 2.3.13 generated the existing Room sources successfully on the original
    toolchain. Its release lineage includes the 2.3.6 fix for upstream issue
    google/ksp#2763. Final full build/runtime verification remains below.
- [x] Upgrade the compatible AGP/Gradle/Kotlin/SDK toolchain.
  - AGP 9.5.1, Gradle 9.8.2, Kotlin 2.4.21 and compile/target SDK 37 compiled
    successfully before runtime-library upgrades. minSdk stays 24.
  - Regenerated the wrapper twice as documented; its JAR and distribution
    checksums matched the official Gradle SHA-256 values.
- [x] Upgrade and verify AndroidX/Compose/coroutines/test dependencies.
  - Compose BOM 2026.09.00, coroutines 1.11.0, AndroidX test core/runner 1.7.0,
    and Robolectric 4.17 compile; all 32 focused UI/retention/alarm/database cases
    passed after adding Robolectric's documented JDK 17+ test-only module flag.
  - Replaced deprecated dividers, progress overloads and directional icons.
- [ ] Verify Room upgrade, exact join results and schema/data compatibility.
- [ ] Update CI action runtimes and document migration decisions.
- [ ] Full JVM/lint/build checks, emulator UI tests and upgrade retention.
- [ ] Scoped review and honest handoff with remaining physical-device limits.
