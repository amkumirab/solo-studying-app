# Build and lint maintenance checklist

- [x] Reproduce Context retention; fix used ViewModels and verify focused tests.
  - Red retention case failed on the original code; all 38 retention/gameplay/
    focus-reliability cases passed after normalization. No assertion was weakened.
- [x] Remove approved unused Context arguments/colors and redundant label.
  - Removed only the seven template colors and two unused constructor arguments
    approved by the user. Lint and compilation passed on the original stack.
- [x] Migrate KTX suggestions without changing commit semantics.
  - Checked preference batches retain native commit failure, with no asynchronous
    fallback or partial batch after an editing exception. All 19 focused storage,
    timer-feedback, alarm-activity and observable-locale tests passed.
  - Lint retains one UseKtx advisory on the checked-edit helper because the KTX
    API returns Unit instead of commit success. No suppression was added.
- [x] Upgrade and verify KSP against the documented upstream exception.
  - KSP 2.3.13 generated the existing Room sources successfully on the original
    toolchain. Its release lineage includes the 2.3.6 fix for upstream issue
    google/ksp#2763. The final local build log contains no KSP exception.
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
- [x] Verify Room upgrade, exact join results and schema/data compatibility.
  - Room 2.8.5 passed 13 cold-reopen, migration and gameplay cases. The schema
    stays at version 12. Select only skills columns, eliminating the generated
    query mismatch warning. The explicit development fallback preserves the old
    drop-known-tables-only behavior. Packaged-app upgrade checks passed below.
- [x] Update CI action runtimes and document migration decisions.
  - Updated Node 24 action releases with read-only repository permissions and
    JDK 21 unchanged. Build rationale and the retained advisory are documented.
- [x] Full JVM/lint/build checks, emulator UI tests and upgrade retention.
  - Full JVM suite: 601 tests across 123 suites, zero failures, errors or skips.
    Lint: zero errors and one documented checked-persistence advisory.
  - Debug, unsigned Release and instrumentation APK builds passed together.
    All 9 device cases passed on each of API 36 (57.045s) and 37 (59.426s).
    API 37 dark-mode recreation also passed (6.457s).
  - API 37 in-place upgrade retained the saved one-minute session and profile;
    a new one-minute timer completed and raised the daily total to two minutes.
    App ID, minimum API and debug signing certificate were preserved.
- [x] Scoped review and honest handoff with remaining physical-device limits.
  - Reviewed production changes, test assertions, wrapper checksums, CI access
    and emulator identity guards; PowerShell syntax and git diff checks passed.
    No new suppression, disabled test, weaker assertion or extended timeout.
    The original task files remain unchanged. Physical alerts, OEM background
    behavior, older APIs and phone upgrades remain explicitly unverified.
