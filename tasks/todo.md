# Weekly planner in Today

- [x] Domain: today's progress and safe postponement, with unit tests.
- [x] UI: compact rows, progress and preview confirmation, with Compose tests.
- [x] Integrate the existing dashboard and exclude duplicate course recommendations.
- [x] Full regression tests, lint, debug/release builds and review.

Physical-device verification is separate from automated test results.

Verified on 2026-10-08: 223 tests passed (12 added), lint reported 0 errors and
34 existing warnings, debug and unsigned release APK builds succeeded.
The final full run used `--no-daemon`; see the verification notes in the contract.
