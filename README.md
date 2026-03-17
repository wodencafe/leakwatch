# LeakWatch

LeakWatch is a runtime lifecycle leak detector for Java applications.

It is built for the kinds of objects that have a real end-of-life contract: things that are supposed to be closed, disposed, shut down, disconnected, or unsubscribed. LeakWatch is not trying to replace a heap dump or profiler. It is trying to catch the boring, expensive bugs can that happen when lifecycle cleanup is forgotten or when cleaned-up objects stay pinned longer than they should.

## What LeakWatch is good at

LeakWatch currently focuses on four practical cases:

- **missed cleanup** - an object should have been cleaned up, but it reached GC first
- **retained after cleanup** - cleanup happened, but something still held a strong reference afterward
- **suspicious retention growth** - a type with no explicit cleanup boundary starts piling up past a configured count or approximate shallow-byte budget
- **best-effort fallback cleanup** - a leaked object can still trigger a detached cleanup action after GC-before-cleanup

## What it is not

LeakWatch is not:

- a universal heap leak detector
- proof that a still-reachable object is definitely a bug
- a replacement for JFR, heap dumps, profilers, or memory analysis tools
- a mechanism for calling instance cleanup methods on dead objects during GC (See `@FallbackCleanup`)

## The basic idea

You annotate the types you care about, objects that need to be closed, or otherwise that you suspect might not be getting cleaned up.
LeakWatch tracks object construction and cleanup boundaries, then reports problems when lifecycle expectations are violated.

Use the annotations for their various purposes:

- `@LeakTracked` - This type has a cleanup boundary that should be observed.  Catch entities that don't get explicitly cleaned up prior to garbage collection.
- `@CleanupMethod` - This is the method that counts as cleanup when the name is not conventional or you want the contract to stay explicit.
- `@ExpectUnreachableAfterCleanup` - After the cleanup method is called, catch entities that don't get garbage collected after a grace period.
- `@FallbackCleanup` - If a cleanup call was missed, LeakWatch may run a detached, best-effort fallback action.
- `@RetentionSuspect` - This type has no explicit cleanup contract, but suspicious accumulation is still worth flagging (count or memory usage boundary).

## Quick start

### 1) Add the dependency

The easiest path is to import the BOM and use the AspectJ module.

```kotlin
dependencies {
    implementation(platform("cafe.woden:leakwatch-bom:0.1.0"))

    implementation("cafe.woden:leakwatch-aspectj")
    aspect("cafe.woden:leakwatch-aspectj:0.1.0")
}
```

Why the explicit version on `aspect(...)`? The dedicated AspectJ plugin configuration may not inherit the BOM constraint the same way normal `implementation` does, so the safe consumer setup is to version that line directly.

### 2) Annotate the lifecycle type

```java
import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.ExpectUnreachableAfterCleanup;
import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(captureStackTrace = true, tags = {"network", "session"})
@ExpectUnreachableAfterCleanup(gracePeriodMillis = 500)
final class SessionHandle implements AutoCloseable {

    @Override
    @CleanupMethod
    public void close() {
        // release resources, unregister callbacks, etc.
    }
}
```

### 3) Configure LeakWatch at startup

```java
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.reporter.Slf4jLeakReporter;

LeakWatchRuntime.configureFromSystemPropertiesAndEnvironment(new Slf4jLeakReporter());
```

That gives you a simple startup path with system-property and environment-variable support.

### 4) Run the sample

If you want one in-repo example that shows the most complete “real app” setup without requiring a dedicated Java agent, start here:

```bash
./gradlew :samples:sample-golden-path:run
```

See `docs/golden-path.md` for the copy-paste walkthrough.

## Example reports you can get

Depending on annotations and configuration, LeakWatch can report:

- `GC_WITHOUT_CLEANUP`
- `RETAINED_AFTER_CLEANUP`
- `RETENTION_COUNT_EXCEEDED`
- `RETENTION_APPROX_BYTES_EXCEEDED`
- `FALLBACK_CLEANUP_EXECUTED`
- `FALLBACK_CLEANUP_FAILED`

## Annotation guide

### `@LeakTracked`

Use this for types that have a real cleanup step.

Typical examples:

- listeners or registrations that must be unregistered
- sessions, handles, subscriptions, sockets, UI resources, temp resources
- anything where not calling cleanup is a real bug

Useful attributes:

- `captureStackTrace` - capture an allocation trace for diagnostics
- `conventionCleanupDetection` - treat conventional zero-arg cleanup names as cleanup automatically
- `tags` - attach stable metadata for logs, JSON, metrics, JFR, and filtering

Conventional cleanup names currently include:

- `close`
- `dispose`
- `shutdown`
- `disconnect`
- `unsubscribe`

### `@CleanupMethod`

Use this when the cleanup method has a non-standard name, or when you want the contract to stay explicit even if the method name is conventional.

Good fits:

- `release()`
- `finish()`
- `tearDown()`
- a private helper that is the real terminal cleanup step

### `@ExpectUnreachableAfterCleanup`

Use this when you want to catch entities that don't get garbage collected after explicit cleanup is called. 

This is useful for catching zombie objects that stay pinned by:

- static collections
- listener registries
- caches
- queues
- accidental owner references

Key attributes:

- `gracePeriodMillis` - small grace window after cleanup before checking reachability
- `captureCleanupStackTrace` - capture where cleanup was observed
- `tags` - extra metadata for retained-after-cleanup reports

### `@FallbackCleanup`

Use this only when a leaked `@LeakTracked` object can safely perform a detached cleanup step after GC-before-cleanup.

This is for reducing damage, not replacing the normal cleanup path.

Good fits:

- deleting a temp file from a captured `Path`
- unregistering something by detached identifier
- cleanup work that can run without dereferencing the original tracked object

Bad fits:

- anything that needs to call instance methods on `this`
- fallback state that would strongly retain the tracked object
- normal business logic that must happen deterministically during cleanup

### `@RetentionSuspect`

Use this for types with no explicit cleanup contract when you still want a practical “this seems to be piling up” signal.

Key attributes:

- `maxLiveInstances` - live-count threshold
- `maxApproxShallowBytes` - approximate shallow-byte threshold
- `captureStackTrace` - capture allocation trace for retained instances
- `severity` - classify the report as `INFO`, `WARN`, or `ERROR`
- `tags` - attach metadata for downstream reporting

This is a suspicion signal, not proof of a leak.

## Runtime configuration

Programmatic configuration is still the main path, but LeakWatch also supports startup flags and environment-based wiring.

Common properties:

- `leakwatch.enabled`
- `leakwatch.captureStackTraces`
- `leakwatch.strictMode`
- `leakwatch.conventionCleanupDetection`
- `leakwatch.additionalExcludedPackages`

Core reporter properties:

- `leakwatch.reporters` (`slf4j`, `json`, `rolling-json`, `jfr`)
- `leakwatch.json.path`
- `leakwatch.rollingJson.path`
- `leakwatch.rollingJson.maxBytes`
- `leakwatch.rollingJson.maxArchives`

Matching environment variables are supported too, for example:

- `LEAKWATCH_ENABLED`
- `LEAKWATCH_REPORTERS`
- `LEAKWATCH_JSON_PATH`
- `LEAKWATCH_STRICT_MODE`

To disable LeakWatch completely for a given launch:

```bash
java -Dleakwatch.enabled=false -jar app.jar
```

or:

```bash
LEAKWATCH_ENABLED=false java -jar app.jar
```

## Optional shallow-size backends

LeakWatch can monitor suspicious retention by count alone, or by count plus approximate shallow-byte budgets.

You have three practical runtime stories:

| Launch mode | What you add | JVM launch change | Byte-budget result | Sample |
|---|---|---|---|---|
| Count-only | Nothing beyond the default core/aspect setup | None | `maxApproxShallowBytes` stays inactive | `samples/sample-retention` |
| JOL fallback | `leakwatch-jol` on the runtime classpath | None | Best-effort shallow-size estimates through JOL | `samples/sample-retention-jol` |
| Dedicated instrumentation | `leakwatch-instrumentation` on the runtime classpath | `-javaagent:/path/to/leakwatch-instrumentation-<version>.jar` | Preferred `Instrumentation#getObjectSize(...)` backend | `samples/sample-retention-instrumentation` |

If both optional modules are present, LeakWatch prefers the dedicated instrumentation backend when the agent is active and falls back to JOL when it is not.

## Publishing

For local builds:

```bash
./gradlew publishReleaseModulesToMavenLocal
```

## Modules

- `leakwatch-annotations` - public annotations and fallback-cleanup contracts
- `leakwatch-core` - runtime registry, cleaner integration, reporting, retention tracking
- `leakwatch-aspectj` - AspectJ advice for constructor tracking and cleanup interception
- `leakwatch-jfr` - optional JFR reporter and event types
- `leakwatch-testkit` - test helpers for in-memory configuration and GC-sensitive waits
- `leakwatch-instrumentation` - optional Java agent and `Instrumentation#getObjectSize(...)` backend
- `leakwatch-jol` - optional JOL-backed shallow-size backend
- `leakwatch-metrics` - optional in-process counters for report totals, types, and classes
- `leakwatch-micrometer` - optional Micrometer counter reporter
- `leakwatch-bom` - version alignment for LeakWatch consumer builds
- `samples` - runnable adoption examples and integration coverage

## Samples

The repo includes focused samples for the main feature paths:

- `sample-golden-path`
- `sample-closeable`
- `sample-dispose`
- `sample-retention`
- `sample-retention-jol`
- `sample-retention-instrumentation`
- `sample-fallback`
- `sample-strict`
- `sample-observability`
- `sample-micrometer`
- `sample-jfr`
- `sample-property-configured-jfr`
- `sample-rolling-json`

Useful sample commands:

```bash
./gradlew :samples:sample-golden-path:run
./gradlew :samples:sample-retention:run
./gradlew :samples:sample-retention-jol:run
./gradlew :samples:sample-retention-instrumentation:runWithLeakWatchAgent
```

## More docs

Read here for implementation details and additional info:

- `docs/golden-path.md` - the quickest end-to-end adoption path
- `docs/configuration.md` - runtime flags, reporter wiring, and startup options
- `docs/troubleshooting.md` - common setup mistakes and debugging tips
- `docs/release-checklist.md` - final pre-release sanity check

## Repo sanity-check commands

Before a release or GitHub push, these are the useful ones:

```bash
./gradlew clean test
./gradlew consumerSmokeTest
./gradlew verifyReleaseReadiness
```

What they do:

- `consumerSmokeTest` publishes the main LeakWatch artifacts to `mavenLocal()` and runs a nested Gradle consumer build under `smoke-tests/consumer-bom`
- `verifyReleaseReadiness` checks repo hygiene, generated POM metadata, sources/javadoc jars, and the nested consumer smoke test

## Limitations

Clarification footnotes:

- Retention monitoring is a suspicion engine, not proof of a memory leak.
- AspectJ weaving still has to be configured correctly by the consuming build
