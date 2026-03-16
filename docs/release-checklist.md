# Release Checklist

Use this before cutting any non-trivial tag or publishing artifacts outside the repo.

## Build and test

- run `./gradlew clean test`
- run `./gradlew consumerSmokeTest`
- run `./gradlew verifyReleaseReadiness`
- run `./gradlew publishReleaseModulesToGitHubPackages` in dry-run rehearsal conditions or verify the publish workflow inputs are ready
- verify the woven sample modules still pass:
  - `sample-golden-path`
  - `sample-closeable`
  - `sample-dispose`
  - `sample-retention`
  - `sample-retention-jol`
  - `sample-retention-instrumentation`
  - `sample-fallback`
  - `sample-strict`
- confirm there are no new unexpected GC-sensitive test flakes

## Publishing rehearsal

- run `./gradlew publishReleaseModulesToMavenLocal`
- inspect `~/.m2/repository/cafe/woden/` and confirm all publishable modules are present:
  - `leakwatch-annotations`
  - `leakwatch-core`
  - `leakwatch-aspectj`
  - `leakwatch-jfr`
  - `leakwatch-testkit`
  - `leakwatch-instrumentation`
  - `leakwatch-jol`
  - `leakwatch-metrics`
  - `leakwatch-micrometer`
  - `leakwatch-bom`
- open one generated POM and verify that license, developer, issue tracker, and SCM metadata are present
- verify the signing key, passphrase, and optional signing key id are present in repository secrets before the first GitHub Actions publish
- import `cafe.woden:leakwatch-bom` into a scratch consumer build and verify a simple dependency declaration resolves cleanly
- confirm the nested `smoke-tests/consumer-bom` build still passes against `mavenLocal()`

## Observability sanity checks

- confirm `leakwatch-jfr` tests still pass
- confirm `STRICT_MODE_WARNING`, `GC_WITHOUT_CLEANUP`, `RETAINED_AFTER_CLEANUP`, `RETENTION_COUNT_EXCEEDED`, `RETENTION_APPROX_BYTES_EXCEEDED`, `FALLBACK_CLEANUP_EXECUTED`, and `FALLBACK_CLEANUP_FAILED` are still covered
- review any new logging or report-type additions for naming consistency

## Build output sanity checks

- inspect AspectJ warnings and make sure they are still the expected `adviceDidNotMatch` cases rather than a regression
- confirm sources jars and javadoc jars are produced for every published module
- confirm the instrumentation jar still carries the expected agent manifest entries
- make sure no sample accidentally depends on unpublished local-only behavior

## Docs and versioning

- update version numbers if the release is not a snapshot
- update `README.md` if the module list, BOM snippet, or launch guidance changed
- update `README.md` or the release notes if the publication set, signing flow, metadata, or consumer snippets changed
- give the remaining docs a quick skim so they still match the current behavior

## API and posture review

- verify fallback cleanup is still documented as a detached safety net rather than normal lifecycle control
- verify retention monitoring is still described as heuristic rather than proof of leakage
- verify any new public annotations or SPIs have basic sample coverage
- verify the BOM only aligns publishable modules and does not accidentally promise support for sample artifacts

## Final packaging review

- check generated artifacts for obvious omissions
- make sure sample apps still start with the documented main classes
- tag only after the docs and tests tell the same story
