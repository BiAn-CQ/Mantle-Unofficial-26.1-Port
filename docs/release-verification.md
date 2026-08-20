# Release verification: 26.1.2-1.12.0

Verified locally on 2026-08-21 with Java 25, Minecraft 26.1.2, and
NeoForge 26.1.2.95.

## Automated verification

- `gradlew clean test build`: passed.
- JUnit: 5 suites, 71 tests, 0 failures, 0 errors, 0 skipped.
- Resource validation: every JSON file under `src/main/resources` and
  `src/generated/resources` was parsed.
- `runGameTestServer`: 1 required test passed and the server shut down cleanly.
- `runReleaseServer`: a new dedicated-server world was created, saved, stopped,
  restarted, and stopped again.
- Second dedicated-server start: 0 ERROR log entries. The remaining WARN entries
  are Minecraft command ambiguity diagnostics and the deliberate offline-mode
  warning from the isolated smoke configuration.

`jarJar NO-SOURCE` is expected: Mantle does not embed third-party libraries.
`processTestResources NO-SOURCE` is also expected: the tests consume the main
resource tree directly. The actual `test` task executes 71 tests and is not
`NO-SOURCE`.

## Dependent-mod verification

Tinkers' Construct `26.1.2-3.11.3` was rebuilt against this exact Mantle JAR:

- 20 suites, 122 tests, 0 failures, 0 errors, 0 skipped.
- 88 cast model-to-texture chains validated.
- 1,721 TConstruct/Mantle model files and 2,218 local texture references validated.
- Its packaged Mantle dependency range is `[1.12.0,)`.

## Warning audit

NeoForge 26.1.2 still ships the legacy item/fluid handler adapters, while marking
them for later removal. Mantle retains them only in source locations explicitly
annotated as the TConstruct 26.1 compatibility boundary. Removal warnings outside
that boundary remain enabled.

Java 25 may print a terminal warning from Lombok's compile-time annotation
processor. Lombok is `compileOnly`; the final Mantle JAR contains zero
`lombok/` classes, so this warning is not a runtime dependency or gameplay path.

## Artifacts

- `Mantle-26.1.2-1.12.0.jar`
  - SHA-256: `5AC67D79FA365E3B544650FCC64E08CD9FF959CB30EA70A663C5C70864D8D345`
- `TinkersConstruct-26.1.2-3.11.3.jar`
  - SHA-256: `F315E798E802B19CB870AAECED19F5B6E22D0213D51E1A91132C1476EF052EF8`

