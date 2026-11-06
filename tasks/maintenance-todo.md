# Build and lint maintenance checklist

- [x] Reproduce Context retention; fix used ViewModels and verify focused tests.
  - Red retention case failed on the original code; all 38 retention/gameplay/
    focus-reliability cases passed after normalization. No assertion was weakened.
- [x] Remove approved unused Context arguments/colors and redundant label.
  - Removed only the seven template colors and two unused constructor arguments
    approved by the user. Lint and compilation passed on the original stack.
- [ ] Migrate KTX suggestions without changing commit semantics.
- [ ] Upgrade and verify KSP against the documented upstream exception.
- [ ] Upgrade the compatible AGP/Gradle/Kotlin/SDK toolchain.
- [ ] Upgrade and verify AndroidX/Compose/coroutines/test dependencies.
- [ ] Verify Room upgrade, exact join results and schema/data compatibility.
- [ ] Update CI action runtimes and document migration decisions.
- [ ] Full JVM/lint/build checks, emulator UI tests and upgrade retention.
- [ ] Scoped review and honest handoff with remaining physical-device limits.
