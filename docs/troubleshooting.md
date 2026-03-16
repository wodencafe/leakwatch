# Troubleshooting

## I only see `adviceDidNotMatch` warnings

That can be normal.

The shared AspectJ jar contains multiple cleanup advices (`close`, `dispose`, `shutdown`, `disconnect`, `unsubscribe`, and annotated cleanup). A given sample or consumer project may not exercise every one of them, so AspectJ may warn that some advice did not match during that compile.

If your sample build and tests pass, those warnings are usually informational rather than fatal.

## My `@LeakTracked` type is not being tracked

Check these first:

- the class is actually woven
- LeakWatch is enabled
- the class is not excluded by package
- the class exposes either `@CleanupMethod` or a recognized conventional cleanup method

If strict mode is enabled and LeakWatch skips the class, you should see a `STRICT_MODE_WARNING` explaining why. `samples/sample-strict` contains woven examples for that path.

## My fallback cleanup never runs

Check these rules carefully:

- the class uses `@FallbackCleanup(action = ...)`
- the class implements `FallbackCleanupStateProvider<T>`
- `leakWatchFallbackCleanupState()` returns detached state, not the tracked instance itself
- the fallback action has a no-arg constructor
- `samples/sample-strict` is a good reference if you want to see the warning paths for missing provider, self-retaining state, throwing state capture, or non-instantiable fallback actions
- the object actually becomes phantom reachable; if something still strongly retains it, Cleaner will not fire

Also remember the obvious-but-rude truth: fallback cleanup only runs when explicit cleanup did **not** happen. If `close()` ran successfully, there is nothing left for the fallback path to do.

## My fallback cleanup failed

If you now see `FALLBACK_CLEANUP_FAILED`, LeakWatch did attempt the detached safety-net action, but that action threw.

Check these first:

- the fallback action really can operate on the detached state alone
- the fallback action tolerates already-cleaned or partially-cleaned resources
- the action is not assuming framework state that no longer exists when GC happens

This report exists so fallback failures do not disappear into logs alone.

## My retention count seems noisy

That is expected to a point. `@RetentionSuspect` is a heuristic mode based on weak-reference live counts, not a heap-profiler-grade retained-size analysis.

## JFR events are missing

Make sure:

- you configured `JfrLeakReporter`
- the relevant JFR event names are enabled on the `Recording`
- your test waits long enough for the underlying report to be emitted before stopping the recording

## GC-sensitive tests may fail intermittently or time out

If a GC-sensitive test constructs the leaked object in the same method where it waits for cleanup reports, the JIT may keep the local variable strongly reachable longer than its lexical scope suggests.

Prefer creating the leaked object in a helper method that returns only a `WeakReference` (and any detached state you intentionally want to keep, such as a `Path`). That makes the test much more reliably GC-eligible.
