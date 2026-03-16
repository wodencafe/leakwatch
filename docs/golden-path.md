# Golden-path sample

If you want one copy-paste sample that shows the most common LeakWatch story without requiring a dedicated Java agent, start here.

The `samples/sample-golden-path` module demonstrates all of these together:

- `@LeakTracked` with explicit cleanup
- `@FallbackCleanup` with detached state captured through `FallbackCleanupStateProvider`
- `@ExpectUnreachableAfterCleanup` for post-cleanup zombie retention detection
- `@RetentionSuspect` with both live-count and approximate shallow-byte budgets
- NDJSON reporting through `LeakReportJsonFileReporter`
- JOL-backed shallow-size auto-discovery through `leakwatch-jol`

## Why this sample exists

Most of the repo samples are intentionally narrow. They prove one idea each.

The golden-path sample is the opposite: it shows the practical baseline a consumer app can copy when they want LeakWatch to feel like a coherent product instead of a bag of unrelated demo modules.

## What it does

The sample app:

1. configures LeakWatch with a composite reporter made from `Slf4jLeakReporter` and `LeakReportJsonFileReporter`
2. creates and explicitly closes a `GoldenPathSession` so no report should be emitted for that class
3. creates a `GoldenPathTempFile`, drops the strong reference, and relies on detached fallback cleanup to delete the temp file after `GC_WITHOUT_CLEANUP`
4. creates and closes a `GoldenPathListenerRegistration`, but intentionally leaves it pinned in a static registry so `RETAINED_AFTER_CLEANUP` fires with allocation-site and cleanup-site traces
5. creates three `GoldenPathCacheEntry` instances so both `RETENTION_COUNT_EXCEEDED` and `RETENTION_APPROX_BYTES_EXCEEDED` can fire. The live-count report captures the threshold-crossing snapshot, not necessarily the final steady-state live count.

## Run it

```bash
./gradlew :samples:sample-golden-path:run
```

By default the app writes NDJSON reports to:

```text
samples/sample-golden-path/build/leakwatch/golden-path.ndjson
```

You can also pass a custom output file path as the first argument to the application.

## Why it uses JOL

The point of this sample is to stay operationally simple:

- no dedicated LeakWatch Java agent
- no `-javaagent` launch flags
- no JFR recording setup

That makes it a better “first real integration” example for most users.

If you want stronger agent-backed shallow-size estimates, see `samples/sample-retention-instrumentation` and the launch-mode summary in the root `README.md`.


## Post-cleanup reachability note

The listener-registration part of the sample is intentionally wrong on purpose. It demonstrates the kind of bug `@ExpectUnreachableAfterCleanup` is meant to catch: the cleanup method runs, but the object stays pinned by some other owner anyway.

That is the core idea behind `@ExpectUnreachableAfterCleanup`: cleanup happened, but some other owner still kept the object alive.
